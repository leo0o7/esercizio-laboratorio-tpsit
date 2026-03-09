# Progetto Laboratorio TPSIT: Cambio Valuta

## 1. Analisi dei Dati

L'applicazione permette la conversione di importi in Euro (EUR) verso valute straniere, specificamente Dollaro Statunitense (USD) e Sterlina (GBP).

### Dati di Input

- **Importo in Euro**: Campo numerico per l'inserimento dell'importo da convertire
- **Valuta di destinazione**: Selezione della valuta target (USD o GBP)
- **Cookie utente**: Identificativo univoco UUID per tracciare l'utente

### Dati di Output

- **Risultato conversione**: Importo convertito nella valuta selezionata
- **Tasso di cambio**: Informazione sul tasso utilizzato
- **Storico conversioni**: Lista delle ultime 10 conversioni effettuate dall'utente

## 2. Parametri HTTP

### Form HTML

```html
<form action="CambioValuta" method="POST">
  <input type="number" name="importo" step="0.01" required />
  <select name="valuta">
    <option value="USD">USD - Dollaro Statunitense</option>
    <option value="GBP">GBP - Sterlina</option>
  </select>
  <button type="submit">Converti</button>
</form>
```

### Parametri del Form

- **importo**: Numero decimale che rappresenta l'importo in Euro
- **valuta**: Stringa che indica la valuta di destinazione (USD/GBP)

### Recupero nella Servlet

```java
String importoStr = request.getParameter("importo");
String valuta = request.getParameter("valuta");
```

## 3. Flusso di Controllo

### Scenario 1: Utente senza cookie (Prima visita)

1. L'utente visita la pagina `index.html`
2. Non viene trovato nessun cookie esistente
3. L'utente compila il form con importo e valuta
4. Al submit, la Servlet:
   - Genera un nuovo UUID come cookie
   - Salva il mapping nel file di memoria
   - Visualizza il risultato della conversione

### Scenario 2: Utente con cookie (Visite successive)

1. L'utente visita la pagina
2. La Servlet rileva il cookie esistente
3. Recupera lo storico delle conversioni dal file di memoria
4. Visualizza lo storico in una tabella scrollabile sotto al form
5. L'utente può effettuare nuove conversioni che vengono aggiunte allo storico

### Scenario 3: Errore di validazione

- **Importo negativo**: Messaggio di errore "L'importo deve essere positivo"
- **Importo zero**: Messaggio di errore "Inserire un importo maggiore di zero"
- **Valuta non selezionata**: Selezione default GBP

### Gestione Errori

- Validazione lato client con HTML5
- Validazione lato server nella Servlet
- Messaggi di errore visualizzati nella pagina risultato
- Possibilità di ritentare la conversione

## 4. Architettura del Progetto

### Struttura delle Directory

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── leo/
│   │           └── servlet/
│   │               ├── CambioValutaServlet.java    (Servlet principale)
│   │               ├── service/
│   │               │   └── CurrencyRateService.java (API Frankfurter)
│   │               └── util/
│   │                   ├── CookieManager.java      (Gestione cookie)
│   │                   └── ConversionHistory.java   (Storage storico)
│   └── webapp/
│       ├── index.html
│       └── WEB-INF/
│           └── web.xml
├── test/
│   └── java/
│       └── com/
│           └── leo/
│               └── CambioValutaServletTest.java
```

### Componenti

1. **CambioValutaServlet.java**: Servlet principale che gestisce le richieste GET e POST
2. **CurrencyRateService.java**: Servizio per il recupero dei tassi di cambio live da API Frankfurter
3. **CookieManager.java**: Utilità per la gestione dei cookie utente (UUID)
4. **ConversionHistory.java**: Gestione del file di storage per lo storico conversioni
5. **index.html**: Pagina principale con form di conversione
6. **File di memoria**: Storage testuale per le conversioni precedenti

## 5. Storico Conversioni

### Funzionalità

L'applicazione memorizza le ultime 10 conversioni per ogni utente:

- **Visualizzazione**: Lo storico viene mostrato in una tabella scrollabile sotto al form
- **Ordine**: Le conversioni più recenti appaiono in alto
- **Persistenza**: Lo storico viene salvato in un file di testo (`conversion_memory.txt`)
- **Formato**: Ogni utente ha una riga nel formato `userId:conversione1|conversione2|...`

### Formato File di Memoria

```properties
userId1:100.00 EUR -> 85.67 GBP|50.00 EUR -> 54.28 USD
userId2:75.50 EUR -> 81.96 USD
```

### Interfaccia Utente

Lo storico viene visualizzato come:

- Tabella HTML con intestazioni (#, Conversione)
- Scrollabile verticalmente (max 200px di altezza)
- Sticky header per le intestazioni
- Effetto hover sulle righe

## 6. Tassi di Cambio

### API Live (Frankfurter)

L'applicazione utilizza l'API pubblica Frankfurter per ottenere i tassi di cambio in tempo reale:

- **URL**: `https://api.frankfurter.dev/v1/latest?from=EUR&symbols=USD,GBP`
- **Aggiornamento**: Ogni 10 richieste (caching lato server)
- **Fonte**: European Central Bank
- **Nessuna API key richiesta**

### Comportamento

1. Al primo accesso, vengono recuperati i tassi live dall'API
2. I tassi vengono memorizzati in cache
3. Ogni 10 richieste, i tassi vengono riaggiornati
4. Se l'API fallisce, viene mostrato un messaggio di errore

In caso di errore API, l'applicazione mostra un messaggio di errore.

### Tassi di Riferimento (solo in caso di errore)

- EUR → USD: 1.0856
- EUR → GBP: 0.8567

## 7. Requisiti Tecnici

- Java 17
- Apache Tomcat 10+
- Maven per la gestione delle dipendenze
- Servlet API 5.0

## 8. Istruzioni per l'Esecuzione

1. Compilare l'applicazione: `mvn clean compile`
2. Creare il WAR per Tomcat: `mvn clean package -DskipTests`
3. Aggiungere il WAR nella directory `/webapps/` di Tomcat
4. Accedere a: `http://localhost:8080/progettoLabTPSIT/CambioValuta`

## 9. Debug

### Errori Comuni

- **404**: Verificare che l'URL sia corretto e che il file HTML sia in webapp
- **405**: Assicurarsi che il form usi POST e la Servlet implementi doPost
- **500**: Controllare la console per errori Java
