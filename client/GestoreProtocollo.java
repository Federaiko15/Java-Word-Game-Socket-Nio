package client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class GestoreProtocollo {
    private final Gson gson = new Gson();

    // Metodo chiamato dal Thread di Rete quando arriva un messaggio
    public void gestisciRispostaServer(String json) {
        try {
            // Tentiamo di parsare la risposta generica
            RispostaServer risposta = gson.fromJson(json, RispostaServer.class);

            if (risposta.gameWords != null && !risposta.gameWords.isEmpty()) {
                // CASO: AGGIORNAMENTO PARTITA
                System.out.println("\n[SERVER] " +risposta.message);
                stampaTavoloGioco(risposta.gameWords);
            } else {
                // CASO: MESSAGGIO GENERICO (Login OK, Errore, Benvenuto)
                System.out.println("\n[SERVER] " + risposta.message);
            }
            // logica di chiusura.
            if(risposta.message != null && risposta.message.toLowerCase().contains("disconnesso")) {
                System.out.println("Chiusura applicazione...");
                System.exit(0); // termino il programma.
            }
            // Ristampiamo un prompt visivo per l'utente
            System.out.print("> "); 

        } catch (JsonSyntaxException e) {
            System.out.println("\n[SERVER] " + json);
        }
    }

    private void stampaTavoloGioco(String paroleCsv) {
        String[] parole = paroleCsv.split(",");
        System.out.println("\n========================================");
        System.out.println("         PAROLE DISPONIBILI");
        System.out.println("========================================");
        
        // Stampa le parole in una griglia 4x4
        for (int i = 0; i < parole.length; i++) {
            System.out.printf("| %-15s ", parole[i]);
            if ((i + 1) % 4 == 0) System.out.println("|");
        }
        System.out.println("========================================");
        System.out.println("Scrivi le 4 parole separate da virgola (es: P1,P2,P3,P4) per tentare.");
    }
}