package server;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.stream.JsonReader;

public class LettoreJson implements Runnable{
    GestoreServer server;
    String nomeFile;
    private int indicePartitaDaLeggere = 0;
    
    
    public LettoreJson(GestoreServer server, String nomeFile) {
        this.server = server;
        this.nomeFile = nomeFile;
    }
    
    public void run() {
        // ho diviso la logica del run con la funzione così dall'esterno posso chiamare la funzione direttamente, cosa che mi potrebbe servire nel caso in cui avessi la necessità di avviare una nuova partita manualmente
        caricaProssimaPartita();
    }

    public void caricaProssimaPartita() {
        // utilizzo una mappa parola-tema per poi facilmente, dalla parola, risalire al tema e correggere al meglio il tentativo del client.
        Map<String, String> soluzionePartita = new HashMap<>();
        boolean partitaTrovata = false;
        System.out.println("Inizio lettura file JSON");
        try(JsonReader reader = new JsonReader(new FileReader(nomeFile))) {
            reader.beginArray();
            int indiceCorrente = 0;

            while (reader.hasNext()) {
                if(indiceCorrente == indicePartitaDaLeggere) {
                    //Gli indici sono uguali, quindi abbiamo trovato nel file la partita che dobbiamo attivare.
                    System.out.println("Partita trovata con successo");
                    soluzionePartita = analizzaPartita(reader);// chiamo la funzione che mi permette di analizzare la partita giusta.
                    partitaTrovata = true;
                    break;
                } else {
                    reader.skipValue(); //altrimenti vado avanti nella ricerca.
                }
                indiceCorrente++;//in entrambi i casi aumento l'indice per la ricerca.
            }
            if(!partitaTrovata) { // controllo che mi permette di ricominciare da 0 se, dopo aver letto tutto il file, non ho trovato nessun game.
                indicePartitaDaLeggere = 0;
                return;
            } else {
                indicePartitaDaLeggere++;
            }
        } catch(Exception e) {
            System.out.println("Errore durante la lettura del file JSON delle partite: " +e.getMessage());
            return;
        }
        if(!soluzionePartita.isEmpty()) {
            System.out.println("Partita caricata. Invio mappa soluzioni al server...");
            server.aggiornaPartitaCorrente(soluzionePartita);
        }
    }

    public Map<String, String> analizzaPartita(JsonReader reader) throws IOException{
        Map<String, String> mappaParoleTema = new HashMap<>(); //mappa che mi servità per salvare le parole con il loro tema associato.
        System.out.println("Inizio analisi partita");
        reader.beginObject();//inizia l'oggetto all'interno del quale si trova l'array contente gli oggetti con tema e array parole.
        while (reader.hasNext()) {//leggo l'elemento, se questo è un gruppo chiamo la funzione che mi riempie la mappa con le parole.
            String nome = reader.nextName();
            if("groups".equals(nome)) {
                riempiMappaGruppi(reader, mappaParoleTema);
            } else {
                reader.skipValue(); //altrimenti passo al prossimo elemento
            }
        }
        reader.endObject(); // e termino la lettura con la chiusura dell'oggetto.
        return mappaParoleTema;
    }

    public void riempiMappaGruppi(JsonReader reader, Map<String, String> mappa) throws IOException{
        System.out.println("Inizio riempimento mappa contente le parole");
        reader.beginArray(); //inizia la funzione aprendo l'array che contiene gli oggetti.
        while (reader.hasNext()) { //entro all'interno dell'array
            String temaCorrente = null;
            List<String> listaParole = new ArrayList<>();
            //ed entro poi dentro l'oggetto, dove ho il tema e le parole
            reader.beginObject();
            while (reader.hasNext()) {
                String nome = reader.nextName();
                if("theme".equals(nome)) {
                    temaCorrente = reader.nextString();
                } else if("words".equals(nome)) {//se trovo l'array entro dento, con beginArray e leggo tutte le parole aggiungendole nella lista nella quale salverò tutte le parole
                    reader.beginArray();
                    while (reader.hasNext()) {
                        listaParole.add(reader.nextString());
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject(); //siamo arrivati alla fine dell'oggetto che racchiude un gruppo di parole.
            if(temaCorrente != null && !listaParole.isEmpty()) {
                for(String parola : listaParole) {
                    mappa.put(parola, temaCorrente);
                } // arrivato alla fine quindi, per ogni parola controllata all'interno di un singolo oggetto, associo nella mappa il tema appena letto
            }
        }
        reader.endArray();
    }

    public int getIdPartita() {
        return this.indicePartitaDaLeggere - 1;
    }
}
