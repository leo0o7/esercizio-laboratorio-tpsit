package com.leo.servlet.util;

import jakarta.servlet.ServletContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversionHistory {

    private static final String MEMORY_FILE = "conversion_history.txt";
    private static final int MAX_HISTORY_SIZE = 10;

    private final ServletContext servletContext;

    public ConversionHistory(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void saveConversion(String userId, double importo, String valuta, double risultato) {
        try {
            Path filePath = getFilePath();
            Map<String, List<String>> history = readHistoryFile(filePath);

            List<String> userHistory = history.getOrDefault(userId, new ArrayList<>());
            String conversionRecord = String.format("%.2f EUR -> %.2f %s", importo, risultato, valuta);
            userHistory.add(0, conversionRecord);

            if (userHistory.size() > MAX_HISTORY_SIZE) {
                userHistory = userHistory.subList(0, MAX_HISTORY_SIZE);
            }

            history.put(userId, userHistory);
            writeHistoryFile(filePath, history);
        } catch (IOException e) {
            System.err.println("Error saving conversion: " + e.getMessage());
        }
    }

    public List<String> getHistory(String userId) {
        try {
            Path filePath = getFilePath();
            Map<String, List<String>> history = readHistoryFile(filePath);
            return history.getOrDefault(userId, new ArrayList<>());
        } catch (IOException e) {
            System.err.println("Error reading conversion history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Path getFilePath() throws IOException {
        String webAppPath = servletContext.getRealPath("/");
        if (webAppPath == null) {
            webAppPath = System.getProperty("java.io.tmpdir");
        }
        return Paths.get(webAppPath, MEMORY_FILE);
    }

    private Map<String, List<String>> readHistoryFile(Path filePath) throws IOException {
        Map<String, List<String>> history = new HashMap<>();

        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String userId = parts[0].trim();
                        String[] conversions = parts[1].trim().split("\\|");
                        List<String> userHistory = new ArrayList<>();
                        for (String conv : conversions) {
                            if (!conv.trim().isEmpty()) {
                                userHistory.add(conv.trim());
                            }
                        }
                        history.put(userId, userHistory);
                    }
                }
            }
        }

        return history;
    }

    private void writeHistoryFile(Path filePath, Map<String, List<String>> history) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (Map.Entry<String, List<String>> entry : history.entrySet()) {
                writer.write(entry.getKey() + ":" + String.join("|", entry.getValue()));
                writer.newLine();
            }
        }
    }
}
