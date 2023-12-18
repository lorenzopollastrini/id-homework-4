package com.github.lorenzopollastrini;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatsCalculator {

    public static void main(String[] args) throws Exception {
        String usage = "Utilizzo: java com.github.lorenzopollastrini.StatsCalculator" +
                " [-s SOURCE_DIRECTORY]\n\n";

        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        String sourceDirectoryString = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-s")) {
                sourceDirectoryString = args[++i];
            } else {
                throw new IllegalArgumentException("Parametro " + args[i] + " sconosciuto");
            }
        }

        if (sourceDirectoryString == null) {
            System.err.println(usage);
            System.exit(1);
        }

        int tableCount = 0;
        int figureCount = 0;
        int citationCount = 0;

        float tablesPerArticleMean;
        float figuresPerArticleMean;
        float citationsPerArticleMean;

        Map<Integer, Integer> paragraphCountToTableCount = new HashMap<>();
        Map<Integer, Integer> paragraphCountToFigureCount = new HashMap<>();

        Path sourceDirectoryPath = Paths.get(sourceDirectoryString);
        try (Stream<Path> pathStream = Files.list(sourceDirectoryPath)) {
            List<Path> paths = pathStream.collect(Collectors.toList());
            int i = 0;
            for (Path path : paths) {
                String absolutePath = path.toFile().getAbsolutePath();
                String json = new String(Files.readAllBytes(Paths.get(absolutePath)));
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);

                System.out.println("Analisi del documento " + i++ + " in corso...");

                int currentTableCount = countTables(document);
                tableCount += currentTableCount;
                int currentFigureCount = countFigures(document);
                figureCount += currentFigureCount;
                citationCount += countCitations(document);

                updateParagraphCountToTableCount(document, paragraphCountToTableCount, currentTableCount);
                updateParagraphCountToFigureCount(document, paragraphCountToFigureCount, currentFigureCount);
            }

            tablesPerArticleMean = (float) tableCount / paths.size();
            figuresPerArticleMean = (float) figureCount / paths.size();
            citationsPerArticleMean = (float) citationCount / paths.size();

            System.out.println("Numero di tabelle totali: " + tableCount);
            System.out.println("Numero di figure totali: " + figureCount);
            System.out.println("Numero di citazioni totali: " + citationCount);

            System.out.println("Numero medio di tabelle per articolo: " + tablesPerArticleMean);
            System.out.println("Numero medio di figure per articolo: " + figuresPerArticleMean);
            System.out.println("Numero medio di citazioni per articolo: " + citationsPerArticleMean);

            System.out.println("Distribuzione dei paragrafi sulle tabelle: " + paragraphCountToTableCount);
            System.out.println("Distribuzione dei paragrafi sulle figure: " + paragraphCountToFigureCount);
        }

    }

    private static int countTables(Object document) {
        List<Object> tables = JsonPath.read(document, "$.content.tables[*]");
        return tables.size();
    }

    private static int countFigures(Object document) {
        List<Object> figures = JsonPath.read(document, "$.content.figures[*]");
        return figures.size();
    }

    private static int countCitations(Object document){
        int citationCount = 0;
        List<Object> tablesCaptionCitations = JsonPath.read(document,"$.content.tables[*].caption_citations[*]");
        citationCount += tablesCaptionCitations.size();
        List<Object> figuresCaptionCitations = JsonPath.read(document,"$.content.figures[*].caption_citations[*]");
        citationCount += figuresCaptionCitations.size();
        List<Object> tablesParaghraphsCitations = JsonPath.read(document,"$.content.tables[*].paragraphs[*].citations[*]");
        citationCount += tablesParaghraphsCitations.size();
        List<Object> figuresParaghraphsCitations = JsonPath.read(document,"$.content.figures[*].paragraphs[*].citations[*]");
        citationCount += figuresParaghraphsCitations.size();
        return citationCount;
    }

    private static  void updateParagraphCountToTableCount(Object document,
                                                          Map<Integer,Integer> paragraphCountToTableCount,
                                                          int currentTableCount) {
        for (int i = 0; i < currentTableCount; i++){
            List<Object> currentTableParagraphs = JsonPath.read(document, "$.content.tables[" + i + "].paragraphs[*]");
            int currentTableParagraphCount = currentTableParagraphs.size();
            if (!paragraphCountToTableCount.containsKey(currentTableParagraphCount))
                paragraphCountToTableCount.put(currentTableParagraphCount, 1);
            else
                paragraphCountToTableCount.put(
                        currentTableParagraphCount, paragraphCountToTableCount.get(currentTableParagraphCount)+1);
        }
    }

    private static void updateParagraphCountToFigureCount(Object document,
                                                          Map<Integer, Integer> paragraphCountToFigureCount,
                                                          int currentFigureCount) {
        for (int i = 0; i < currentFigureCount; i++){
            List<Object> currentFigureParagraphs = JsonPath.read(document, "$.content.figures[" + i + "].paragraphs[*]");
            int currentFigureParagraphCount = currentFigureParagraphs.size();
            if (!paragraphCountToFigureCount.containsKey(currentFigureParagraphCount))
                paragraphCountToFigureCount.put(currentFigureParagraphCount, 1);
            else
                paragraphCountToFigureCount.put(
                        currentFigureParagraphCount, paragraphCountToFigureCount.get(currentFigureParagraphCount)+1);
        }
    }

}

