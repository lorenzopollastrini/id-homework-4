package com.github.lorenzopollastrini;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    static final XPathFactory xPathFactory = XPathFactory.newInstance();
    static String sourceDirectoryString;
    static String destinationDirectoryString;
    static int currentDoc = 0;

    public static void main(String[] args) throws Exception {
        String usage = "Utilizzo: java com.github.lorenzopollastrini.Main" +
                " [-s SOURCE_DIRECTORY] [-d DESTINATION_DIRECTORY]\n\n";

        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-s":
                    sourceDirectoryString = args[++i];
                    sourceDirectoryString= sourceDirectoryString.replaceAll("/", "\\");
                    break;
                case "-d":
                    destinationDirectoryString = args[++i];
                    destinationDirectoryString = destinationDirectoryString.replaceAll("/", "\\");
                    if (destinationDirectoryString.endsWith("\\")) {
                        destinationDirectoryString = destinationDirectoryString
                                .substring(0, destinationDirectoryString.length() - 1);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Parametro " + args[i] + " sconosciuto");
            }
        }

        if (sourceDirectoryString == null || destinationDirectoryString == null) {
            System.err.println(usage);
            System.exit(1);
        }

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementation domImplementation = registry.getDOMImplementation("LS");
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        lsSerializer.setNewLine("");
        LSOutput lsOutput = domImplementationLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.disableHtmlEscaping();
        Gson gson = gsonBuilder.create();

        Path sourceDirectoryPath = Paths.get(sourceDirectoryString);
        try (Stream<Path> pathStream = Files.list(sourceDirectoryPath)) {
            pathStream.forEach(path -> {
                try {
                    currentDoc++;
                    String absolutePath = path.toFile().getAbsolutePath();
                    Document document = builder.parse(new File(absolutePath));
                    removeWhitespaces(document.getDocumentElement());
                    System.out.println("Conversione del documento @ " + absolutePath + " in corso...");
                    convertDocument(document, lsSerializer, lsOutput, gson);
                    System.out.println("Documento #" + currentDoc + " @ " + absolutePath + " convertito.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void convertDocument(Document document, LSSerializer lsSerializer, LSOutput lsOutput, Gson gson)
            throws Exception
    {
        XPath xPath = xPathFactory.newXPath();

        Article article = new Article();

        // Estrazione del pmcid
        Node pmcidNode = findNode(xPath, "//*[@pub-id-type='pmc']", document);
        String pmcid;
        if (pmcidNode != null) {
            pmcid = pmcidNode.getTextContent();
        } else {
            pmcid = getFileName(document.getDocumentURI().substring(6));
        }
        pmcid = pmcid.substring(3);  // Rimuove "PMC" dal pmcid
        article.setPmcid(pmcid);

        Content content = new Content();

        // Estrazione del titolo
        Node titleNode = findNode(xPath, "//article-title", document);
        String title = titleNode.getTextContent();
        content.setTitle(title);

        // Estrazione dell'abstract
        Node abstractStringNode = findNode(xPath, "//abstract", document);
        String abstractString = "";
        if (abstractStringNode != null) {
            abstractString = getInnerXML(abstractStringNode, lsSerializer, lsOutput);
        }
        content.setAbstractString(abstractString);

        // Estrazione delle parole chiave
        NodeList keywordNodeList = findNodes(xPath, "//kwd", document);
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < keywordNodeList.getLength(); i++) {
            Node childNode = keywordNodeList.item(i);
            keywords.add(childNode.getTextContent());
        }
        content.setKeywords(keywords);

        // Estrazione delle tabelle
        NodeList tableWrapNodes = findNodes(xPath, "//table-wrap", document);
        List<Table> tables = new ArrayList<>();
        for (int i = 0; i < tableWrapNodes.getLength(); i++) {
            Node tableWrapNode = tableWrapNodes.item(i);
            Table table = new Table();

            // Estrazione dell'ID
            String id = tableWrapNode.getAttributes().getNamedItem("id").getNodeValue();
            table.setTableId(id);

            // Estrazione del body
            Node tableNode = findNode(xPath, "table", tableWrapNode);
            String body = "";
            if (tableNode != null) {
                body = getInnerXML(tableNode, lsSerializer, lsOutput);
            }
            table.setBody(body);

            // Estrazione della didascalia
            Node captionNode = findNode(xPath, "caption", tableWrapNode);
            String caption = "";
            if (captionNode != null) {
                caption = getInnerXML(captionNode, lsSerializer, lsOutput);
            }
            table.setCaption(caption);

            // Estrazione dei riferimenti bibliografici menzionati nella didascalia
            List<String> captionCitations = new ArrayList<>();
            if (captionNode != null) {
                // Estrazione delle menzioni di riferimenti bibliografici contenute nella didascalia
                NodeList captionCrossRefsNodes = findNodes(xPath, "descendant::xref[@ref-type='bibr']", captionNode);
                for (int j = 0; j < captionCrossRefsNodes.getLength(); j++) {
                    Node captionCrossRefNode = captionCrossRefsNodes.item(j);
                    String captionCrossRefId = captionCrossRefNode.getAttributes().getNamedItem("rid").getNodeValue();
                    Node captionCitationNode = findNode(xPath, "//ref[@id='" + captionCrossRefId + "']", document);
                    if (captionCitationNode != null) { // Alcuni documenti XML hanno un xref che punta a un ref inesistente
                        captionCitations.add(getOuterXML(captionCitationNode, lsSerializer, lsOutput));
                    }
                }
            }
            table.setCaptionCitations(captionCitations);

            // Estrazione dei footer
            List<String> foots = new ArrayList<>();
            NodeList footsNodes = findNodes(xPath, "table-wrap-foot", tableWrapNode);
            for (int j = 0; j < footsNodes.getLength(); j++) {
                foots.add(getInnerXML(footsNodes.item(j), lsSerializer, lsOutput));
            }
            table.setFoots(foots);

            // Estrazione dei paragrafi che menzionano la tabella
            List<Paragraph> paragraphs = new ArrayList<>();
            NodeList paragraphsNodes = findNodes(xPath, "//p[descendant::xref[@ref-type='table' and @rid='" + id + "']]", document);
            for (int j = 0; j < paragraphsNodes.getLength(); j++) {
                Paragraph paragraph = new Paragraph();
                Node paragraphNode = paragraphsNodes.item(j);
                paragraph.setText(getOuterXML(paragraphNode, lsSerializer, lsOutput));
                List<String> citations = new ArrayList<>();
                // Estrazione delle menzioni di riferimenti bibliografici contenute nel paragrafo
                NodeList paragraphCrossRefsNodes = findNodes(xPath, "descendant::xref[@ref-type='bibr']", paragraphNode);
                for (int k = 0; k < paragraphCrossRefsNodes.getLength(); k++) {
                    Node paragraphCrossRefNode = paragraphCrossRefsNodes.item(k);
                    String paragraphCrossRefId = paragraphCrossRefNode.getAttributes().getNamedItem("rid").getNodeValue();
                    Node paragraphCitationNode = findNode(xPath, "//ref[@id='" + paragraphCrossRefId + "']", document);
                    citations.add(getOuterXML(paragraphCitationNode, lsSerializer, lsOutput));
                }
                paragraph.setCitations(citations);
                paragraphs.add(paragraph);
            }
            table.setParagraphs(paragraphs);

            // Estrazione delle celle
            List<Cell> cells = new ArrayList<>();
            if (tableNode != null) {
                NodeList cellsNodes = findNodes(xPath, "descendant::td", tableNode);
                // Si utilizza una mappa per non ripetere le celle con lo stesso contenuto
                Map<String, Node> textContentToNode = new HashMap<>();
                for (int j = 0; j < cellsNodes.getLength(); j++) {
                    Node cellNode = cellsNodes.item(j);
                    String textContent = cellNode.getTextContent();
                    if (!textContentToNode.containsKey(textContent) && !textContent.isEmpty()) {
                        textContentToNode.put(textContent, cellNode);
                        Cell cell = new Cell();
                        List<String> citedIn = new ArrayList<>();
                        NodeList citedInNodes =
                                findNodes(xPath, "//p[contains(text(), " + buildXpathConcat(textContent) + ")]", document);
                        for (int k = 0; k < citedInNodes.getLength(); k++) {
                            citedIn.add(getOuterXML(citedInNodes.item(k), lsSerializer, lsOutput));
                        }
                        cell.setContent(textContent);
                        cell.setCitedIn(citedIn);
                        cells.add(cell);
                    }
                }
            }
            table.setCells(cells);

            tables.add(table);
        }
        content.setTables(tables);

        // Estrazione delle figure
        NodeList figureNodes = findNodes(xPath, "//fig", document);
        List<Figure> figures = new ArrayList<>();
        for (int i = 0; i < figureNodes.getLength(); i++) {
            Node figureNode = figureNodes.item(i);
            Figure figure = new Figure();

            // Estrazione dell'ID
            String id = figureNode.getAttributes().getNamedItem("id").getNodeValue();
            figure.setFigId(id);

            // Estrazione dell'URL
            Node sourceNode = findNode(xPath, "graphic", figureNode);
            String source = "";
            if (sourceNode != null) {
                source = sourceNode.getAttributes().getNamedItem("xlink:href").getNodeValue();
                source = "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC" + pmcid + "/bin/" + source + ".jpg";
            }
            figure.setSource(source);

            // Estrazione della didascalia
            Node captionNode = findNode(xPath, "caption", figureNode);
            String caption = "";
            if (captionNode != null) {
                caption = getInnerXML(captionNode, lsSerializer, lsOutput);
            }
            figure.setCaption(caption);

            // Estrazione dei riferimenti bibliografici menzionati nella didascalia
            List<String> captionCitations = new ArrayList<>();
            if (captionNode != null) {
                // Estrazione delle menzioni di riferimenti bibliografici contenute nella didascalia
                NodeList captionCrossRefsNodes = findNodes(xPath, "descendant::xref[@ref-type='bibr']", captionNode);
                for (int j = 0; j < captionCrossRefsNodes.getLength(); j++) {
                    Node captionCrossRefNode = captionCrossRefsNodes.item(j);
                    String captionCrossRefId = captionCrossRefNode.getAttributes().getNamedItem("rid").getNodeValue();
                    Node captionCitationNode = findNode(xPath, "//ref[@id='" + captionCrossRefId + "']", document);
                    if (captionCitationNode != null) { // Alcuni documenti XML hanno un xref che punta a un ref inesistente
                        captionCitations.add(getOuterXML(captionCitationNode, lsSerializer, lsOutput));
                    }
                }
            }
            figure.setCaptionCitations(captionCitations);

            // Estrazione dei paragrafi che menzionano la figura
            List<Paragraph> paragraphs = new ArrayList<>();
            NodeList paragraphsNodes = findNodes(xPath, "//p[descendant::xref[@ref-type='fig' and @rid='" + id + "']]", document);
            for (int j = 0; j < paragraphsNodes.getLength(); j++) {
                Paragraph paragraph = new Paragraph();
                Node paragraphNode = paragraphsNodes.item(j);
                paragraph.setText(getOuterXML(paragraphNode, lsSerializer, lsOutput));
                List<String> citations = new ArrayList<>();
                // Estrazione delle menzioni di riferimenti bibliografici contenute nel paragrafo
                NodeList paragraphCrossRefsNodes = findNodes(xPath, "descendant::xref[@ref-type='bibr']", paragraphNode);
                for (int k = 0; k < paragraphCrossRefsNodes.getLength(); k++) {
                    Node paragraphCrossRefNode = paragraphCrossRefsNodes.item(k);
                    String paragraphCrossRefId = paragraphCrossRefNode.getAttributes().getNamedItem("rid").getNodeValue();
                    Node paragraphCitationNode = findNode(xPath, "//ref[@id='" + paragraphCrossRefId + "']", document);
                    citations.add(getOuterXML(paragraphCitationNode, lsSerializer, lsOutput));
                }
                paragraph.setCitations(citations);
                paragraphs.add(paragraph);
            }
            figure.setParagraphs(paragraphs);

            figures.add(figure);
        }
        content.setFigures(figures);

        article.setContent(content);

        FileWriter fileWriter = new FileWriter(destinationDirectoryString + "\\pmcid_" + pmcid + ".json");
        JsonWriter jsonWriter = new JsonWriter(fileWriter);
        jsonWriter.setIndent(" ");

        gson.toJson(article, Article.class, jsonWriter);
        jsonWriter.close();
    }

    private static Node findNode(XPath xPath, String xPathExpressionString, Object document) throws XPathExpressionException {
        XPathExpression expr = xPath.compile(xPathExpressionString);
        return (Node) expr.evaluate(document, XPathConstants.NODE);
    }

    private static NodeList findNodes(XPath xPath, String xPathExpressionString, Object document) throws XPathExpressionException {
        XPathExpression expr = xPath.compile(xPathExpressionString);
        return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
    }

    private static String getOuterXML(Node node, LSSerializer lsSerializer, LSOutput lsOutput) {
        StringWriter writer = new StringWriter();
        lsOutput.setCharacterStream(writer);
        lsSerializer.write(node, lsOutput);
        return writer.toString();
    }

    private static String getInnerXML(Node node, LSSerializer lsSerializer, LSOutput lsOutput) {
        StringBuilder innerXML = new StringBuilder();
        NodeList childrenNodes = node.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            Node childNode = childrenNodes.item(i);
            innerXML.append(getOuterXML(childNode, lsSerializer, lsOutput));
        }

        return innerXML.toString();
    }

    /**
     * Rimuove gli spazi bianchi dovuti all'indentazione
     * @param node il nodo radice
     */
    private static void removeWhitespaces(Node node) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);

            if (currentNode.getNodeType() == Node.TEXT_NODE) {
                // Verifica se il nodo contiene solo spazi bianchi
                if (currentNode.getTextContent().trim().isEmpty()) {
                    node.removeChild(currentNode);
                    i--; // Aggiusta l'indice dopo la rimozione
                } else {
                    currentNode.setTextContent(currentNode.getTextContent().replaceAll("\\s{2,}", " "));
                }
            }
            if (currentNode.hasChildNodes()) {
                // Ricorsivamente rimuove gli spazi nei nodi figli
                removeWhitespaces(currentNode);
            }
        }
    }

    /**
     * Costruisce una chiamata a concat di XPath per evitare conflitti tra il contenuto di textContent e i delimitatori
     * di stringhe usati in concat
     * @param textContent il contenuto testuale del nodo
     * @return la chiamata a concat senza conflitti
     */
    private static String buildXpathConcat(String textContent) {
        String delimiter = "'";

        // Separa le stringhe in base a delimiter e lo include nel risultato
        String[] tokens = textContent.split("(?=" + delimiter + ")|(?<=" + delimiter + ")");

        if (tokens.length <= 1) {
            return "'" + textContent.replaceAll("\"", "\\\\\"") + "'";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String token : tokens) {
            if (token.equals("'")) {
                stringBuilder.append("\"'\", ");
            } else {
                stringBuilder.append("'" + token + "', ");
            }
        }
        String arguments = stringBuilder.toString();
        arguments = arguments.substring(0, arguments.length() - 2);

        return "concat(" + arguments + ")";
    }

    private static String getFileName(String filePath) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
