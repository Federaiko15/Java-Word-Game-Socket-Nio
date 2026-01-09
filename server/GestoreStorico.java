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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//file che gestisce il salvataggio e la gestione delle partite.
public class GestoreStorico {
    private static String FILE_PATH;
    private ConcurrentHashMap<Integer, DatiPartitaPassata> archivioPartite; //mappa in cui mi salvo le partite, avendo come chiave l'indice di queste ultime
    private final Gson gson;

    public GestoreStorico(String filePathStorico) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.archivioPartite = new ConcurrentHashMap<>();
        FILE_PATH = filePathStorico;
        caricaStorico();//carico subito la funzione di caricamento dal file, come avviene anche per la gestione degli utenti
    }

    private void caricaStorico() {
        if(!Files.exists(Paths.get(FILE_PATH))) {
            System.out.println("File storico partite non trovato, ne verrà creato uno nuovo");
        }
        try (Reader reader = new FileReader(FILE_PATH)){
            Type lisType = new TypeToken<ArrayList<DatiPartitaPassata>>(){}.getType();
            List<DatiPartitaPassata> lista = gson.fromJson(reader, lisType);
            //Definiamo che stiamo leggento una Lista di partite
            if(lista != null) { //ogni partita trovata la inserisco nella mappa 
                for(DatiPartitaPassata partita: lista) {
                    archivioPartite.put(partita.idPartita, partita);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void scriviSuFile() {//la funzione di scrittura prende gli elementi della mappa e li scrive all'interno del file
        try(Writer writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            List<DatiPartitaPassata> lista = new ArrayList<>(archivioPartite.values());
            gson.toJson(lista, writer);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public String recuperaInfoPartita(int idPartita, String usernameUtente) {
        //questa funzione mi serve per prendere dalla mappa la partita con un id specifico e ritornare una string formattata tramite StringBuilder che contiene delle informazioni utili per l'utente
        DatiPartitaPassata partita = archivioPartite.get(idPartita);
        if(partita == null) {
            return "Partita con ID "+idPartita+" non trovata nello storico";
        }
        StringBuilder messaggioFinale = new StringBuilder();
        messaggioFinale.append("STORICO PARTITA ").append(idPartita).append("\n");
        messaggioFinale.append("--SOLUZIONE--\n");

        for(Map.Entry<String,List<String>> entry: partita.soluzione.entrySet()) {
            messaggioFinale.append("Tema: ").append(entry.getKey()).append("-> ").append(entry.getValue()).append("\n");
        }

        StatisticheUtente stats = partita.statisticheGiocatori.get(usernameUtente);
        messaggioFinale.append("\n--I TUOI RISULTATI--\n");
        if(stats != null) {
            messaggioFinale.append("Gruppi indovinati: ").append(stats.gruppiIndovinati).append("/4\n");
            messaggioFinale.append("Errori commessi: ").append(stats.errori).append("\n");
            messaggioFinale.append("Punteggio ottenuto: ").append(stats.punteggio).append("\n");
        } else {
            messaggioFinale.append("Non hai partecipato a questa partita (o eri disconnesso al termine).\n");
        }
        return messaggioFinale.toString();
    }

    public void aggiungiPartitaAlloStorico(DatiPartitaPassata partitaDaSalvare) {
        archivioPartite.put(partitaDaSalvare.idPartita, partitaDaSalvare);
        scriviSuFile();//quando aggiungo una partita la inserisco subito nella mappa e chiamo la funzione di scrittura per avere anche il file aggiornato subito
    }

    public String recuperaStatisticheGlobali(int idPartita) {
    // Recuperiamo l'oggetto, la partita, dalla mappa
        DatiPartitaPassata dati = archivioPartite.get(idPartita);
        
        if (dati == null) {
            return "Errore: Nessuna statistica trovata per la partita ID " + idPartita;
        }

        // Inizializziamo i contatori che mi serviranno per tenere traccia delle statistiche di una singola partita
        int partecipanti = dati.statisticheGiocatori.size();
        int conclusi = partecipanti; // In una partita storica, per definizione tutti hanno concluso (tempo scaduto)
        int vittorie = 0;
        double sommaPunteggi = 0;

        // Iteriamo sui valori della mappa (le statistiche dei singoli utenti)
        for (StatisticheUtente s : dati.statisticheGiocatori.values()) {
            if (s.haVinto) {
                vittorie++;
            }
            sommaPunteggi += s.punteggio;
        }

        // Calcolo della media (gestendo il caso divisione per zero)
        double mediaPunti = (partecipanti > 0) ? (sommaPunteggi / partecipanti) : 0.0;

        // E utilizzo anche qui lo StringBuilder per formattare la stringa da mandare al client
        StringBuilder sb = new StringBuilder();
        sb.append("--- STATISTICHE STORICHE PARTITA ").append(idPartita).append(" ---\n");
        sb.append("Partecipanti totali: ").append(partecipanti).append("\n");
        sb.append("Hanno concluso la partita: ").append(conclusi).append("\n");
        sb.append(" - Numero di Vittorie: ").append(vittorie).append("\n");
        sb.append(" - Numero di Sconfitte/Non completati: ").append(partecipanti - vittorie).append("\n");
        // %.2f serve a stampare il double con 2 cifre decimali
        sb.append("Punteggio Medio dei giocatori: ").append(String.format("%.2f", mediaPunti)).append("\n");

        return sb.toString();
    }
}
