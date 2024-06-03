package rs;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Sequential {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java rs.Sequential <input file>");
            return;
        }

        String inputFileName = args[0];
        String outputFileName = "result.txt";

        long startTime = System.currentTimeMillis();

        try {
            Map<String, Integer> wordCounts = countWordOccurrences(inputFileName);
            List<Map.Entry<String, Integer>> sortedWordCounts = sortWordCounts(wordCounts);
            writeResultToFile(sortedWordCounts, outputFileName);
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("Execution time: " + executionTime + " ms");
    }

    private static Map<String, Integer> countWordOccurrences(String fileName) throws IOException {
        Map<String, Integer> wordCounts = new HashMap<>();

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName))) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(bis));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] words = line.toLowerCase().replaceAll("[\\p{P}&&[^\u0027]]", "").split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty() && word.length() > 1) {
                        wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        return wordCounts;
    }

    private static List<Map.Entry<String, Integer>> sortWordCounts(Map<String, Integer> wordCounts) {
        return wordCounts.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .collect(Collectors.toList());
    }

    private static void writeResultToFile(List<Map.Entry<String, Integer>> sortedWordCounts, String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, Integer> entry : sortedWordCounts) {
                writer.write(entry.getKey() + " " + entry.getValue());
                writer.newLine();
            }
        }
    }
}
