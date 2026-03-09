package com.leo.servlet.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CurrencyRateService {

    private static final String API_URL = "https://api.frankfurter.dev/v1/latest?from=EUR&symbols=USD,GBP";
    private static final int REQUEST_INTERVAL = 10;

    private Map<String, Double> rates;
    private int requestCount;
    private String lastUpdate;

    public CurrencyRateService() {
        this.rates = new HashMap<>();
        this.requestCount = 0;
        this.lastUpdate = null;
        fetchRates();
    }

    public synchronized Map<String, Double> getRates() throws IOException {
        requestCount++;
        
        if (requestCount % REQUEST_INTERVAL == 0 || rates.isEmpty()) {
            fetchRates();
        }
        
        if (rates.isEmpty()) {
            throw new IOException("Impossibile recuperare i tassi di cambio. Riprova più tardi.");
        }
        
        return rates;
    }

    private void fetchRates() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                parseRates(response.toString());
                System.out.println("Tassi di cambio aggiornati: " + rates);
            } else {
                System.err.println("API error: " + responseCode);
            }
            
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Errore nel recupero tassi: " + e.getMessage());
        }
    }

    private void parseRates(String json) {
        rates.clear();
        
        String ratesSection = extractJsonSection(json, "rates");
        
        if (ratesSection != null) {
            ratesSection = ratesSection.trim();
            ratesSection = ratesSection.substring(1, ratesSection.length() - 1);
            
            String[] pairs = ratesSection.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String currency = keyValue[0].trim().replace("\"", "");
                    String rateStr = keyValue[1].trim();
                    try {
                        double rate = Double.parseDouble(rateStr);
                        rates.put(currency, rate);
                    } catch (NumberFormatException e) {
                        System.err.println("Errore parsing rate: " + rateStr);
                    }
                }
            }
        }
        
        lastUpdate = extractJsonValue(json, "date");
    }

    private String extractJsonSection(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int braceStart = json.indexOf("{", colonIndex);
        int braceEnd = json.indexOf("}", colonIndex);
        
        if (braceStart == -1 || braceEnd == -1) return null;
        
        return json.substring(braceStart, braceEnd + 1);
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int valueStart = json.indexOf("\"", colonIndex);
        int valueEnd = json.indexOf("\"", valueStart + 1);
        
        if (valueStart == -1 || valueEnd == -1) return null;
        
        return json.substring(valueStart + 1, valueEnd);
    }

    public String getLastUpdate() {
        return lastUpdate;
    }
}
