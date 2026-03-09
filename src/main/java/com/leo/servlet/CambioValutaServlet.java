package com.leo.servlet;

import com.leo.servlet.service.CurrencyRateService;
import com.leo.servlet.util.ConversionHistory;
import com.leo.servlet.util.CookieManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/CambioValuta")
public class CambioValutaServlet extends HttpServlet {

    private CurrencyRateService rateService;
    private ConversionHistory historyManager;

    @Override
    public void init() throws ServletException {
        rateService = new CurrencyRateService();
        historyManager = new ConversionHistory(getServletContext());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String importoStr = request.getParameter("importo");
        String valuta = request.getParameter("valuta");

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

        if (valuta == null || (!valuta.equals("USD") && !valuta.equals("GBP"))) {
            valuta = "GBP";
        }

        Map<String, Double> rates;
        try {
            rates = rateService.getRates();
        } catch (IOException e) {
            sendError(response, e.getMessage());
            return;
        }

        Double tasso = rates.get(valuta);
        if (tasso == null) {
            sendError(response, "Valuta non supportata");
            return;
        }

        double risultato = importo * tasso;
        String userId = CookieManager.getUserId(request, response);
        historyManager.saveConversion(userId, importo, valuta, risultato);
        List<String> conversionHistory = historyManager.getHistory(userId);

        sendResultPage(response, importo, valuta, tasso, risultato, conversionHistory);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String userId = CookieManager.getUserId(request, response);
        List<String> conversionHistory = historyManager.getHistory(userId);

        sendFormPage(response, conversionHistory);
    }

    private void sendFormPage(HttpServletResponse response, List<String> history) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append(buildHtmlHead("Cambio Valuta - Convertitore EUR"));
        html.append(buildFormStyles());
        
        if (!history.isEmpty()) {
            html.append(historyTableStyles());
        }
        
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<h1>Cambio Valuta</h1>");
        html.append("<form action='CambioValuta' method='POST'>");
        html.append("<div class='form-group'>");
        html.append("<label for='importo'>Importo in Euro (EUR):</label>");
        html.append("<input type=\"number\" id=\"importo\" name=\"importo\" step=\"0.01\" min=\"0.01\" required placeholder=\"Inserisci l'importo in euro\">");
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
        
        if (!history.isEmpty()) {
            html.append(buildHistoryTable(history, false));
        }
        
        html.append("</div></body></html>");
        response.getWriter().write(html.toString());
    }

    private void sendResultPage(HttpServletResponse response, double importo, String valuta, 
            double tasso, double risultato, List<String> history) throws IOException {
        
        StringBuilder html = new StringBuilder();
        html.append(buildHtmlHead("Risultato Conversione"));
        html.append(buildResultStyles());
        html.append(historyTableStyles());
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<h1>Risultato Conversione</h1>");
        html.append("<div class='result'>");
        html.append("<h2>").append(formatCurrency(risultato, valuta)).append("</h2>");
        html.append("<p><strong>Importo originale:</strong> ").append(formatCurrency(importo, "EUR")).append("</p>");
        html.append("<p><strong>Valuta di destinazione:</strong> ")
            .append(valuta.equals("USD") ? "Dollaro Statunitense" : "Sterlina").append("</p>");
        html.append("<p><strong>Tasso di cambio:</strong> 1 EUR = ").append(String.format("%.4f", tasso)).append(" ")
            .append(valuta).append("</p>");
        
        String lastUpdate = rateService.getLastUpdate();
        if (lastUpdate != null) {
            html.append("<p><strong>Tasso aggiornato al:</strong> ").append(lastUpdate).append("</p>");
        }
        
        html.append("</div>");
        
        if (!history.isEmpty()) {
            html.append(buildHistoryTable(history, true));
        }
        
        html.append("<a href='CambioValuta' class='back-link'>Nuova conversione</a>");
        html.append("</div></body></html>");
        
        response.getWriter().write(html.toString());
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append(buildHtmlHead("Errore"));
        html.append(buildErrorStyles());
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<h1>Errore</h1>");
        html.append("<div class='error'>").append(message).append("</div>");
        html.append("<a href='CambioValuta' class='back-link'>Torna al form</a>");
        html.append("</div></body></html>");
        response.getWriter().write(html.toString());
    }

    private String buildHtmlHead(String title) {
        return "<!DOCTYPE html><html lang='it'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<title>" + title + "</title><style>";
    }

    private String buildFormStyles() {
        return "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }" +
               ".container { background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 500px; width: 100%; }" +
               "h1 { color: #333; text-align: center; margin-bottom: 30px; }" +
               ".form-group { margin-bottom: 20px; }" +
               "label { display: block; margin-bottom: 5px; color: #555; font-weight: bold; }" +
               "input[type='number'], select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px; font-size: 16px; box-sizing: border-box; }" +
               "input[type='number']:focus, select:focus { outline: none; border-color: #007bff; }" +
               "button { width: 100%; padding: 12px; background-color: #007bff; color: white; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; transition: background-color 0.3s; }" +
               "button:hover { background-color: #0056b3; }";
    }

    private String buildResultStyles() {
        return buildFormStyles() +
               ".result { margin-top: 20px; padding: 15px; background-color: #e7f3ff; border-radius: 5px; border-left: 4px solid #007bff; }" +
               ".result h2 { margin: 0 0 10px 0; color: #007bff; font-size: 24px; }" +
               ".result p { margin: 5px 0; color: #555; }" +
               ".back-link { display: block; margin-top: 20px; text-align: center; color: #007bff; text-decoration: none; }" +
               ".back-link:hover { text-decoration: underline; }";
    }

    private String buildErrorStyles() {
        return buildFormStyles() +
               ".error { margin-top: 20px; padding: 15px; background-color: #ffe7e7; border-radius: 5px; border-left: 4px solid #dc3545; color: #dc3545; }" +
               ".back-link { display: block; margin-top: 20px; text-align: center; color: #007bff; text-decoration: none; }" +
               ".back-link:hover { text-decoration: underline; }";
    }

    private String historyTableStyles() {
        return ".history-section { margin-top: 20px; }" +
               ".history-section h3 { color: #333; margin-bottom: 10px; font-size: 16px; }" +
               ".history-table { width: 100%; border-collapse: collapse; max-height: 200px; overflow-y: auto; display: block; }" +
               ".history-table th, .history-table td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; font-size: 13px; }" +
               ".history-table th { background-color: #f8f9fa; color: #555; position: sticky; top: 0; }" +
               ".history-table tr:hover { background-color: #f5f5f5; }" +
               ".history-table td { color: #333; }";
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

    private String formatCurrency(double amount, String currency) {
        String symbol = "";
        switch (currency) {
            case "USD": symbol = "$"; break;
            case "GBP": symbol = "£"; break;
            case "EUR": symbol = "€"; break;
        }
        return String.format("%s%.2f", symbol, amount);
    }
}
