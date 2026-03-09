package com.leo.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet("/CambioValuta")
public class CambioValutaServlet extends HttpServlet {

  private static final String MEMORY_FILE = "conversion_memory.txt";
  private static final double EUR_TO_USD = 1.0856;
  private static final double EUR_TO_GBP = 0.8567;
  private static final int MAX_HISTORY_SIZE = 10;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/html;charset=UTF-8");

    String importoStr = request.getParameter("importo");
    String valuta = request.getParameter("valuta");

    // Validate input
    if (importoStr == null || importoStr.trim().isEmpty()) {
      sendError(response, "Inserire un importo valido");
      return;
    }

    double importo;
    try {
      importo = Double.parseDouble(importoStr.replace(",", "."));
    } catch (NumberFormatException e) {
      sendError(response, "Formato numero non valido");
      return;
    }

    if (importo <= 0) {
      sendError(response, "L'importo deve essere maggiore di zero");
      return;
    }

    // Validate currency
    if (valuta == null || (!valuta.equals("USD") && !valuta.equals("GBP"))) {
      valuta = "GBP"; // Default
    }

    // Calculate conversion
    double tasso = valuta.equals("USD") ? EUR_TO_USD : EUR_TO_GBP;
    double risultato = importo * tasso;

    // Get or create cookie
    String userId = getUserCookie(request, response);

    // Save to memory file
    saveConversion(userId, importo, valuta, risultato);

    // Get conversion history for display
    List<String> conversionHistory = getConversionHistory(userId);

    // Build response
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html lang='it'>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<title>Risultato Conversione</title>");
    html.append("<style>");
    html.append(
        "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }");
    html.append(
        ".container { background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 500px; width: 100%; }");
    html.append("h1 { color: #333; text-align: center; margin-bottom: 30px; }");
    html.append(
        ".result { margin-top: 20px; padding: 15px; background-color: #e7f3ff; border-radius: 5px; border-left: 4px solid #007bff; }");
    html.append(".result h2 { margin: 0 0 10px 0; color: #007bff; font-size: 24px; }");
    html.append(".result p { margin: 5px 0; color: #555; }");
    html.append(".error { background-color: #ffe7e7; border-left-color: #dc3545; }");
    html.append(".error h2 { color: #dc3545; }");
    html.append(
        ".back-link { display: block; margin-top: 20px; text-align: center; color: #007bff; text-decoration: none; }");
    html.append(".back-link:hover { text-decoration: underline; }");
    if (!conversionHistory.isEmpty()) {
      html.append(historyTableStyles());
    }
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");
    html.append("<h1>Risultato Conversione</h1>");
    html.append("<div class='result'>");
    html.append("<h2>").append(formatCurrency(risultato, valuta)).append("</h2>");
    html.append("<p><strong>Importo originale:</strong> ").append(formatCurrency(importo, "EUR")).append("</p>");
    html.append("<p><strong>Valuta di destinazione:</strong> ")
        .append(valuta.equals("USD") ? "Dollaro Statunitense" : "Sterlina").append("</p>");
    html.append("<p><strong>Tasso di cambio:</strong> 1 EUR = ").append(String.format("%.4f", tasso)).append(" ")
        .append(valuta).append("</p>");
    html.append("</div>");
    if (!conversionHistory.isEmpty()) {
      html.append(buildHistoryTable(conversionHistory, true));
    }
    html.append("<a href='CambioValuta' class='back-link'>Nuova conversione</a>");
    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    response.getWriter().write(html.toString());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Show the form with last conversion if available
    response.setContentType("text/html;charset=UTF-8");

    String userId = getUserCookie(request, response);
    List<String> conversionHistory = getConversionHistory(userId);

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html lang='it'>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<title>Cambio Valuta - Convertitore EUR</title>");
    html.append("<style>");
    html.append(
        "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }");
    html.append(
        ".container { background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 500px; width: 100%; }");
    html.append("h1 { color: #333; text-align: center; margin-bottom: 30px; }");
    html.append(".form-group { margin-bottom: 20px; }");
    html.append("label { display: block; margin-bottom: 5px; color: #555; font-weight: bold; }");
    html.append(
        "input[type='number'], select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px; font-size: 16px; box-sizing: border-box; }");
    html.append("input[type='number']:focus, select:focus { outline: none; border-color: #007bff; }");
    html.append(
        "button { width: 100%; padding: 12px; background-color: #007bff; color: white; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; transition: background-color 0.3s; }");
    html.append("button:hover { background-color: #0056b3; }");
    if (!conversionHistory.isEmpty()) {
      html.append(historyTableStyles());
    }
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");
    html.append("<h1>Cambio Valuta</h1>");
    html.append("<form action='CambioValuta' method='POST'>");
    html.append("<div class='form-group'>");
    html.append("<label for='importo'>Importo in Euro (EUR):</label>");
    html.append(
        "<input type=\"number\" id=\"importo\" name=\"importo\" step=\"0.01\" min=\"0.01\" required placeholder=\"Inserisci l'importo in euro\">");
    html.append("</div>");
    html.append("<div class='form-group'>");
    html.append("<label for='valuta'>Valuta di destinazione:</label>");
    html.append("<select id='valuta' name='valuta' required>");
    html.append("<option value='USD'>USD - Dollaro Statunitense</option>");
    html.append("<option value='GBP'>GBP - Sterlina</option>");
    html.append("</select>");
    html.append("</div>");
    html.append("<button type='submit'>Converti</button>");
    html.append("</form>");
    if (!conversionHistory.isEmpty()) {
      html.append(buildHistoryTable(conversionHistory, false));
    }
    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    response.getWriter().write(html.toString());
  }

  private String getUserCookie(HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    String userId = null;

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("userId".equals(cookie.getName())) {
          userId = cookie.getValue();
          break;
        }
      }
    }

    if (userId == null) {
      userId = UUID.randomUUID().toString();
      Cookie newCookie = new Cookie("userId", userId);
      newCookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
      newCookie.setPath("/");
      response.addCookie(newCookie);
    }

    return userId;
  }

  private void saveConversion(String userId, double importo, String valuta, double risultato) {
    try {
      Path filePath = getMemoryFilePath();
      Map<String, List<String>> memory = readMemoryFile(filePath);

      List<String> history = memory.getOrDefault(userId, new ArrayList<>());
      String conversionRecord = String.format("%.2f EUR -> %.2f %s", importo, risultato, valuta);
      history.add(0, conversionRecord);

      if (history.size() > MAX_HISTORY_SIZE) {
        history = history.subList(0, MAX_HISTORY_SIZE);
      }

      memory.put(userId, history);

      writeMemoryFile(filePath, memory);
    } catch (IOException e) {
      System.err.println("Error saving conversion: " + e.getMessage());
    }
  }

  private List<String> getConversionHistory(String userId) {
    try {
      Path filePath = getMemoryFilePath();
      Map<String, List<String>> memory = readMemoryFile(filePath);
      return memory.getOrDefault(userId, new ArrayList<>());
    } catch (IOException e) {
      System.err.println("Error reading conversion history: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  private Path getMemoryFilePath() throws IOException {
    String webAppPath = getServletContext().getRealPath("/");
    if (webAppPath == null) {
      webAppPath = System.getProperty("java.io.tmpdir");
    }
    return Paths.get(webAppPath, MEMORY_FILE);
  }

  private Map<String, List<String>> readMemoryFile(Path filePath) throws IOException {
    Map<String, List<String>> memory = new HashMap<>();

    if (Files.exists(filePath)) {
      try (BufferedReader reader = Files.newBufferedReader(filePath)) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] parts = line.split(":", 2);
          if (parts.length == 2) {
            String userId = parts[0].trim();
            String[] conversions = parts[1].trim().split("\\|");
            List<String> history = new ArrayList<>();
            for (String conv : conversions) {
              if (!conv.trim().isEmpty()) {
                history.add(conv.trim());
              }
            }
            memory.put(userId, history);
          }
        }
      }
    }

    return memory;
  }

  private void writeMemoryFile(Path filePath, Map<String, List<String>> memory) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
      for (Map.Entry<String, List<String>> entry : memory.entrySet()) {
        writer.write(entry.getKey() + ":" + String.join("|", entry.getValue()));
        writer.newLine();
      }
    }
  }

  private void sendError(HttpServletResponse response, String message) throws IOException {
    response.setContentType("text/html;charset=UTF-8");

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html lang='it'>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<title>Errore</title>");
    html.append("<style>");
    html.append(
        "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }");
    html.append(
        ".container { background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 500px; width: 100%; }");
    html.append("h1 { color: #333; text-align: center; }");
    html.append(
        ".error { margin-top: 20px; padding: 15px; background-color: #ffe7e7; border-radius: 5px; border-left: 4px solid #dc3545; color: #dc3545; }");
    html.append(
        ".back-link { display: block; margin-top: 20px; text-align: center; color: #007bff; text-decoration: none; }");
    html.append(".back-link:hover { text-decoration: underline; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");
    html.append("<h1>Errore</h1>");
    html.append("<div class='error'>").append(message).append("</div>");
    html.append("<a href='CambioValuta' class='back-link'>Torna al form</a>");
    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    response.getWriter().write(html.toString());
  }

  private String formatCurrency(double amount, String currency) {
    String symbol = "";
    switch (currency) {
      case "USD":
        symbol = "$";
        break;
      case "GBP":
        symbol = "£";
        break;
      case "EUR":
        symbol = "€";
        break;
    }
    return String.format("%s%.2f", symbol, amount);
  }

  private String historyTableStyles() {
    return ".history-section { margin-top: 20px; }"
        + ".history-section h3 { color: #333; margin-bottom: 10px; font-size: 16px; }"
        + ".history-table { width: 100%; border-collapse: collapse; max-height: 200px; overflow-y: auto; }"
        + ".history-table th, .history-table td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; font-size: 13px; }"
        + ".history-table th { background-color: #f8f9fa; color: #555; position: sticky; top: 0; }"
        + ".history-table tr:hover { background-color: #f5f5f5; }"
        + ".history-table td { color: #333; }"
        + ".history-empty { color: #888; font-style: italic; }";
  }

  private String buildHistoryTable(List<String> history, boolean showTitle) {
    if (history.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("<div class='history-section'>");
    if (showTitle) {
      sb.append("<h3>Le tue conversioni</h3>");
    }
    sb.append("<table class='history-table'>");
    sb.append("<thead><tr><th>#</th><th>Conversione</th></tr></thead>");
    sb.append("<tbody>");
    for (int i = 0; i < history.size(); i++) {
      sb.append("<tr><td>").append(i + 1).append("</td><td>").append(history.get(i)).append("</td></tr>");
    }
    sb.append("</tbody>");
    sb.append("</table>");
    sb.append("</div>");
    return sb.toString();
  }
}
