package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.Comparator;

public class GestoreUtenti {
    private static String FILE_PATH;
    // utilizzo una hasmap per accedere direttamente ad un utente presente all'iterno per capire se registrato o meno.
    private ConcurrentHashMap<String, Utente> databaseUtenti;
    private final Gson gson;

    public GestoreUtenti(String filePathUtenti) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.databaseUtenti = new ConcurrentHashMap<>();
        FILE_PATH = filePathUtenti;
        caricaUtentiDaFile();
    }

    // ritorna true se l'utente non aveva già un "account" collegato al nome utente utilizzato
    public synchronized boolean registraUtente(String username, String password) {
        if (databaseUtenti.containsKey(username)) {
            return false; // Utente già esistente
        }
        
        Utente nuovo = new Utente(username, password);
        databaseUtenti.put(username, nuovo);
        salvaUtentiSuFile(); //salviamo subito il nuovo utente
        return true;
    }

    // ritorno true se, oltre ad essere presente nella mappa, anche la password è quella corretta.
    public boolean loginUtente(String username, String password) {
        Utente utente = databaseUtenti.get(username);
        if (utente == null) {
            return false; // Utente non trovato
        }
        return utente.controlloPassword(password);
    }

    public void aggiornaCredenziali(String username, String nuovoUsername, String nuovaPass) {
        Utente utente = databaseUtenti.get(username);
        utente.cambioCredenziali(nuovoUsername, nuovaPass);
        salvaUtentiSuFile();
    } //prende dalla mappa l'utente collegato al nome utente e chiamo la funzione all'interno della classe utente per cambiare in nome e la password, dopo aver fatto questo chiamo la funzione per salvare le modifiche subito sul file

    private void caricaUtentiDaFile() {
        if (!Files.exists(Paths.get(FILE_PATH))) {
            System.out.println("File utenti non trovato, ne verrà creato uno nuovo.");
            return;
        }
        // leggo il file per caricare gli utenti all'interno nella mia mappa
        try (Reader reader = new FileReader(FILE_PATH)) {
            // Definiamo che stiamo leggendo una LISTA di Utenti
            Type listType = new TypeToken<ArrayList<Utente>>(){}.getType();
            List<Utente> listaCaricata = gson.fromJson(reader, listType);
            // e dalla lista risultante prendo ogni utente e lo inserisco nella mappa 
            if (listaCaricata != null) {
                for (Utente utente : listaCaricata) {
                    databaseUtenti.put(utente.getUsername(), utente);
                }
                System.out.println("Caricati " + databaseUtenti.size() + " utenti dal database.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void aggiornaPunteggio(String username, int punti) {
        Utente utente = databaseUtenti.get(username);

        if(utente != null) {
            utente.incrementaPunteggio(punti);
            System.out.println("Utente " +utente.getUsername()+ " con nuovo punteggio: " +utente.getPunteggio());
            salvaUtentiSuFile();
        }
    }

// In GestoreUtenti.java

public synchronized String ottieniStats(String username, int idPartitaAttualeServer) {
    Utente u = databaseUtenti.get(username);
    
    if (u == null) {
        return "Utente non trovato.";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\n========================================\n");
    sb.append("       STATISTICHE DI ").append(username.toUpperCase()).append("\n");
    sb.append("========================================\n");

    // --- SEZIONE 1: PARTITA CORRENTE ---
    sb.append("\n[ PARTITA CORRENTE (ID: ").append(idPartitaAttualeServer).append(") ]\n");
    
    // Controlliamo se i dati in memoria si riferiscono alla partita attuale
    if (u.idPartitaCorrente == idPartitaAttualeServer) {
        sb.append("Stato: ");
        if (u.haVintoTemp) sb.append("VITTORIA");
        else if (u.haPersoTemp) sb.append("SCONFITTA (Limiti errori)");
        else sb.append("IN CORSO");
        sb.append("\n");
        
        sb.append("Punteggio Attuale: ").append(u.punteggioTemp).append("\n");
        sb.append("Errori Commessi: ").append(u.erroriTemp).append("\n");
        sb.append("Gruppi Trovati: ").append(u.temiIndovinatiTemp.size()).append("/4\n");
    } else {
        sb.append("Nessuna partecipazione registrata per la partita corrente.\n");
    }

    // --- SEZIONE 2: STORICO ---
    sb.append("\n[ STORICO GLOBALE ]\n");
    sb.append("Partite Giocate: ").append(u.numPartiteTotali).append("\n");
    
    // Calcolo percentuali
    String winRate = String.format("%.2f", u.getWinRate());
    String lossRate = String.format("%.2f", u.getLossRate());
    
    sb.append("Vittorie: ").append(u.numPartiteVinte).append(" (").append(winRate).append("%)\n");
    sb.append("Sconfitte (4 errori): ").append(u.numPartitePerse).append(" (").append(lossRate).append("%)\n");
    sb.append("Timeout (Non finite): ").append(u.numPartiteNonFinite).append("\n");
    
    sb.append("Perfect Puzzles (0 err): ").append(u.vittoriePerfette).append("\n");
    sb.append("Streak Attuale: ").append(u.streakCorrenteVittorie).append("\n");
    sb.append("Streak Migliore: ").append(u.maxStreak).append("\n");

    // --- SEZIONE 3: ISTOGRAMMA ERRORI ---
    sb.append("\n[ DISTRIBUZIONE ERRORI (Vittorie) ]\n");
    for(int i = 0; i < 4; i++) {
        sb.append(i).append(" Errori: ");
        // Creiamo una piccola barra grafica
        for(int k=0; k<u.istogrammaErrori[i]; k++) sb.append("|");
        sb.append(" (").append(u.istogrammaErrori[i]).append(")\n");
    }
    
    sb.append("\n[ SCONFITTE ]\n");
    sb.append("Per Errori (>3): (").append(u.istogrammaErrori[4]).append(")\n");
    sb.append("Per Timeout:     (").append(u.istogrammaErrori[5]).append(")\n");
    
    sb.append("========================================\n");

    return sb.toString();
}

    public Utente ottieniInfoUtente(String username) {
        Utente utente = databaseUtenti.get(username);
        return utente;
    }

    // Synchronized per evitare che due thread scrivano sul file insieme corrompendolo
    private synchronized void salvaUtentiSuFile() {
        try (Writer writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            // Convertiamo la Mappa (Values) in una Lista per salvarla come Array JSON [ {..}, {..} ]
            List<Utente> listaDaSalvare = new ArrayList<>(databaseUtenti.values());
            gson.toJson(listaDaSalvare, writer);//e tramite il gson scrivo direttamente sul file la lista in formato json passando come argomento il writer aperto in precedenza
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio utenti: " + e.getMessage());
        }
    }

    //metodo synchronized per aggiornare le statistiche correnti dell'utente.
    public synchronized void aggiornaStatoPartitaCorrente(String username, int idPartita, List<String> paroleTrovate, List<String> temiTrovati, int punteggio, int errori, boolean vinto, boolean perso) {
        Utente utente = databaseUtenti.get(username);
        if(utente != null) {
            utente.idPartitaCorrente = idPartita;
            utente.haPersoTemp = perso;
            utente.haVintoTemp = vinto;
            utente.erroriTemp = errori;
            utente.punteggioTemp = punteggio;
            utente.paroleIndovinateTemp = paroleTrovate;
            utente.temiIndovinatiTemp = temiTrovati;

            if(vinto || perso) {
                utente.consolidaStatistichePartita(false); //non per timeout della partita in corso
                salvaUtentiSuFile(); // il salvataggio sul file lo faccio solo quando vinco,perdo, o l'utente fa il logout.
            }
            
        }
    }

    public synchronized void salvaUtenteAllLogout() {
        salvaUtentiSuFile();//quando un utente fa il logout, salvo subito le sue statistiche temporanee sul file.
    }
 
    //metodo chiamato dal server quando scade il timeout della partita per aggiornare lo stato degli utenti subito
    public synchronized void aggiornaTimeoutUtenti(int idPartitaScaduta) {
        for(Utente utente: databaseUtenti.values()) {
            //per ogni utente controllo se stava partecipando alla partita appena terminata e se non aveva giù vinto o perso
            if(utente.idPartitaCorrente == idPartitaScaduta && !utente.haPersoTemp && !utente.haVintoTemp) {
                utente.consolidaStatistichePartita(true); // true == timeout scattato
            }
        }
        salvaUtentiSuFile();//salvo sempre sul file per avere uno stato di consistenza dei dati
    }

    //metodo chiamato per mostrare al client la classifica o il punteggio di un utente in particolare
    public synchronized String calcolaClassifica(int limit, String targetUser) {
        // Convertiamo la mappa in lista per poterla ordinare
        List<Utente> listaUtenti = new ArrayList<>(databaseUtenti.values());

        // Ordiniamo la lista.
        // Criterio: Ordine decrescente di Puzzles Vinti (puzzlesWon).
        // In caso di parità, chi ha meno sconfitte (puzzlesLost).
        Collections.sort(listaUtenti, new Comparator<Utente>() {
            @Override
            public int compare(Utente u1, Utente u2) {
                if (u1.numPartiteVinte != u2.numPartiteVinte) {
                    return Integer.compare(u2.numPartiteVinte, u1.numPartiteVinte); // Decrescente
                }
                return Integer.compare(u1.numPartitePerse, u2.numPartitePerse); // Crescente (chi ne ha meno è meglio)
            }
        });

        StringBuilder sb = new StringBuilder();
        
        // Richiesta Rank Utente Specifico
        if (targetUser != null && !targetUser.isEmpty()) {
            for (int i = 0; i < listaUtenti.size(); i++) {
                if (listaUtenti.get(i).getUsername().equals(targetUser)) {
                    Utente u = listaUtenti.get(i);
                    sb.append("L'utente ").append(targetUser).append(" si trova in posizione #").append(i + 1)
                    .append("\nVittorie: ").append(u.numPartiteVinte)
                    .append(" | Sconfitte: ").append(u.numPartitePerse);
                    return sb.toString();
                }
            }
            return "Utente " + targetUser + " non presente in classifica.";
        }

        // Classifica Generale (Totale o Top K)
        sb.append("=== CLASSIFICA GENERALE ===\n");
        sb.append(String.format("%-4s %-20s %-10s %-10s\n", "POS", "UTENTE", "VITTORIE", "SCONFITTE"));
        sb.append("--------------------------------------------------\n");

        int max = (limit == -1) ? listaUtenti.size() : Math.min(limit, listaUtenti.size());

        for (int i = 0; i < max; i++) {
            Utente u = listaUtenti.get(i);
            sb.append(String.format("%-4d %-20s %-10d %-10d\n", 
                (i + 1), u.getUsername(), u.numPartiteVinte, u.numPartitePerse));
        }
        
        return sb.toString();
    }
}