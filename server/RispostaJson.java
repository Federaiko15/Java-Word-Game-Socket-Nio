package server;

public class RispostaJson {
    public String status;      // "OK", "ERROR", "NOTIFY"
    public String message;     // Messaggio leggibile per l'utente
    public String gameWords;   // Stringa delle parole (opzionale)
    public String operation;   // Tipo di operazione (opzionale, es. "login_reply")

    // Costruttore vuoto per Gson
    public RispostaJson() {}

    // Costruttore comodo per messaggi veloci
    public RispostaJson(String status, String message, String gameWords) {
        this.status = status;
        this.message = message;
        this.gameWords = gameWords;
    }
}