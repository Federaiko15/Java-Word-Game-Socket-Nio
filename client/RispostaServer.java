package client;

// Classe per mappare la risposta JSON del server
public class RispostaServer {
    public String status;      // "OK" o "ERROR"
    public String message;     // Messaggio descrittivo
    public String gameWords;   // "CANE,GATTO,..." (opzionale)
    public String operation;   // Per capire che tipo di messaggio è (opzionale)
}