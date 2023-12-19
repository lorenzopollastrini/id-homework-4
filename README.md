# Homework 4 del corso di Ingegneria dei Dati (2023/2024)

## Descrizione del progetto
Il progetto consiste in quanto segue:
* Estrazione di informazioni, tramite espressioni XPath, da un dataset di più di 100.000 articoli scientifici (in
  formato XML) archiviati in [PubMed Central](https://www.ncbi.nlm.nih.gov/pmc/);
* Calcolo delle statistiche del dataset.

## Struttura del progetto
Il progetto include due classi Java i cui rispettivi metodi `main` possono essere
eseguiti da riga di comando:
* **Main.java** contiene il codice che estrae (in formato JSON) le informazioni dagli articoli scientifici (in formato
  XML) contenuti in una directory locale (viene creato un file JSON per ciascun articolo);
* **StatsCalculator.java** contiene il codice che calcola e stampa alcune statistiche del dataset.

## Comandi
* `java com.github.lorenzopollastrini.Main [-s SOURCE_DIRECTORY] [-d DESTINATION_DIRECTORY]`: estrae al percorso
  `DESTINATION_DIRECTORY` (in formato JSON) le informazioni dagli articoli scientifici (in formato XML) contenuti al percorso
  `SOURCE_DIRECTORY`.
