Esercitazione di Laboratorio: Java Servlet
Modalità: Gruppi da 2 persone
Ogni gruppo dovrà realizzare una Web Application dinamica che utilizzi:
Fase 0: Progettazione (Documento README)
Prima di aprire Eclipse e creare il progetto, ogni gruppo deve elaborare un file di testo chiamato
README, Questo file deve contenere la "mappa" della vostra applicazione:

1. Analisi dei Dati: Elenca quali informazioni l'utente deve inserire nel form (es. nome, età,
   scelta menu).
2. Parametri HTTP: Specifica i name che userete nei tag <input> (es. name="txtNome") e
   come verranno recuperati nella Servlet.
3. Flusso di Controllo: Descrivi cosa succede se i dati sono errati (es. se l'utente inserisce un
   numero negativo, la Servlet deve mostrare un messaggio di errore invece di procedere).
   Esempio di struttura del README:
    Input: utente (String), importo (int).
    Action Form: GestoreConto.
    Variabile Sessione: saldoAttuale (per accumulare i bonifici).
    Output previsto: Una tabella HTML che riassume l'operazione appena fatta.
   Fase 1
   Frontend: Una pagina index.html con un form di inserimento.
   Backend: Una Servlet Java per l'elaborazione dei dati.
   Stato: Utilizzo dell'oggetto HttpSession per memorizzare o aggiornare i dati tra diverse richieste.

Tracce Assegnate
Gruppo Titolo Progetto Descrizione Sintetica

1 Voli Low-Cost

Form con destinazione e data. La Servlet calcola un prezzo e lo salva in
sessione per la conferma.

2 Home Banking

Simulazione di un bonifico: inserisci importo, sottrailo dal saldo memorizzato
in sessione.

3 Carrello Spesa

Selezione prodotti tramite checkbox. La Servlet aggiunge i nomi a una List
in sessione.

Gruppo Titolo Progetto Descrizione Sintetica

4 Quiz Game

Domanda a risposta multipla. La Servlet verifica se è corretta e incrementa il
punteggio in sessione.

5
Registro
Presenze

Inserimento nomi studenti. La Servlet accumula i nomi in una lista
visualizzata ad ogni invio.

6 Cambio Valuta
Convertitore Euro -> USD/GBP. La Servlet salva l'ultima conversione
effettuata in sessione.

7 Prenota Tavolo

Controllo disponibilità: se persone > 10, rifiuta; altrimenti conferma e salva il
nome.

8

Feedback
Evento

Sistema di votazione (1-5). La Servlet calcola la media dei voti totali (usando
ServletContext).

1. Collegamento Form-Servlet
   Nel file HTML:
   HTML

<form action="MiaServlet" method="POST">
<input type="text" name="utente">
<button type="submit">Invia</button>
</form>
Nella Servlet Java:
Java
@WebServlet("/MiaServlet") // Deve corrispondere all'action del form
public class MiaServlet extends HttpServlet {
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ... {
String nome = request.getParameter("utente"); // Deve corrispondere al name dell'input
// Logica qui...
}
}
3. Gestione della Sessione
   Per "ricordare" i dati tra un invio e l'altro:
   Java
   HttpSession session = request.getSession();
   // Per salvare un dato:
   session.setAttribute("nomeDato", valore);
   // Per recuperare un dato:
   Tipo valore = (Tipo) session.getAttribute("nomeDato");

Tabella di Debug

Errore Significato Cosa fare?

404 Not Found

Risorsa non
trovata.

Controlla se l'URL nel browser è corretto e se il file HTML è in
webapp.

405 Method Not
Allowed

Metodo sbagliato.

Hai scritto method="POST" nel form ma nella Servlet c'è
solo doGet?

500 Internal Error

Errore nel codice
Java.

Guarda la Console di Eclipse (testo rosso): cerca la riga che
ha causato l'errore.

Server non parte Conflitto di porte. Chiudi altre istanze di Tomcat o riavvia Eclipse.
