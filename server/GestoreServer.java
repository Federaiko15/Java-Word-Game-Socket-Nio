// classe in cui gestisco la lista nella quale inserire i vari handler dei client per interagire con loro e tenerli aggiornati.

package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import java.util.Map;

public class GestoreServer {
    int PORT;
    private final List<GestoreClient> clients = new CopyOnWriteArrayList<>(); // lista nella quale inserisco tutti i gestori dei client per tenere conto del numero di client collegati
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); //scheduler che mi permette di runnare un thread periodicamente
    public volatile Map<String, String> mappaPartitaAttuale = new ConcurrentHashMap<>(); // mappa nella quale inserisco una parola come chiave e il suo tema
    private final Gson gson = new Gson(); // creo il gson per inviare al clietn file json
    private volatile String paroleAttualiStringa = ""; // variabile che mi serve per wsalvare le parole da inviare al client.
    private final GestoreUtenti gestoreUtenti;  //inizializzo anche il gestoreUtenti, classe nella quale sono definite tutte le variabili per gestire i comandi che arrivano dal client.
    private LettoreJson taskThreadLettore;  // Runnable eseguito dallo scheduler che server a leggere periodicamente il file delle partite
    private long timestampScadenzaPartita = 0;   // variabile che mi serve per tenere traccia del tempo trascorso
    private int DURATA_PARTITA; //il valore lo leggeremo nel file di configurazione
    private final GestoreStorico gestoreStorico; // inizializzo anche l'oggetto che mi permette di gestire le partite sia in corso che passate.


    public GestoreServer(int port, String pathStorico, String pathUtenti, int secondiPartita) {
        this.PORT = port;
        this.DURATA_PARTITA = secondiPartita;
        this.gestoreUtenti = new GestoreUtenti(pathUtenti);
        this.gestoreStorico = new GestoreStorico(pathStorico);
    } //creo immediatamente il gestoreUtenti e il gestoreStorico
    
    public void start() {//la funzione start viene chiamata dal servermain
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { //provo a creare il server socket sulla porta letta dal file di conf
            System.out.println("Server socket collegato alla porta " + PORT);

            this.taskThreadLettore = new LettoreJson(this, "Connections_Data.json"); //creo il runnable che verrà eseguito periodicamente
            //passando il nome del file e anche il gestoreServer.
            this.scheduler.scheduleWithFixedDelay(taskThreadLettore, 0, DURATA_PARTITA, TimeUnit.SECONDS);//e imposto il tempo di inizio, 0, e ogni quanto il task deve essere esguito, 5 minuti.
            System.out.println("Timer partita avviato");

            ExecutorService executor = Executors.newCachedThreadPool(); // creo anche un pool per gestire ogni client con un thread diverso. utilizzo il cachedThreadPool così che si regoli da solo quando eliminare un thread e non ho problemi se dovessero arrivare tanti client
            while (true) {//ciclo infinito che permette al server di mettersi sempre in ascolto per ricevere le richieste dei client
                Socket client = serverSocket.accept();//quando arriva un client collego a questo una socket che verrà utilizzata per la comunicazione
                System.out.println("Nuovo Client collegato al server");
                
                GestoreClient handler = new GestoreClient(client, this); // creo il runnable che gestirà le funzioni utilizzate per la comunicazione
                clients.add(handler); //aggiungo nella lista per tenere traccia dei client attivi
                executor.execute(handler); //e eseguo il runnable tramite il pool
            }
        } catch (IOException e) {
            System.err.println("Errore nella creazione delle socket lato server");
        }
    }

    public int IdPartita() {//funzione che mi permette di reperire l'id della partita corrente.
        return this.taskThreadLettore.getIdPartita();
    }

    public long getSecondiRimanenti() { //funzione che mi calcola quanto tempo rimane al termine di una partita in corso
        long adesso = System.currentTimeMillis();
        long diff = timestampScadenzaPartita - adesso;

        if(diff < 0) return 0;
        return diff/1000; //converto millisecondi in secondi;
    }

    public synchronized void partitaVinta(String nomeVincitore) {//funzione che gestisce la notifica a tutti i client per avvisare che un utente ha concluso la partita vincendola.
        System.out.println("Vittoria, l'utente " +nomeVincitore+ " ha vinto la partita");

        RispostaJson messaggio = new RispostaJson("NOTIFY", "L'utente " +nomeVincitore+ " ha vinto la partita", null);
        broadcastAggiornamentoPartita(gson.toJson(messaggio)); //invio il messaggio a tutti i client che un utente ha vinto la partita.
    }

    public void broadcastAggiornamentoPartita(String messaggio) {
        System.out.println("La partita deve cambiare, quindi avviso tutti i client connessi");
        for(GestoreClient client : clients) {
            client.messaggioAlClient(messaggio);
        }//chiamo la funzione definita nel gestoreClient che manda direttamente un messaggio al client tramite la socket
    }

    public String invioParole() { //funzione che prende dalla mappa contente le associazioni parole temi tutte le parole della parita corrente per inviarle al client
        System.out.println("Richiesta parole");
        List<String> paroleDaInviare = new ArrayList<>(mappaPartitaAttuale.keySet()); //tramite la funzioni keyset inserisco le chiavi in una lista.
        Collections.shuffle(paroleDaInviare); // randomizzo la lista
        String payload = String.join(",", paroleDaInviare); //da questa creo una stringa dividendo gli elementi della lista con una virgola 
        System.out.println(payload); // e mando la stringa al chiamante che verrà utilizzata poi per essere mandata al client
        return payload;
    }
    // funzioni get per utilizzarle lato client.
    public GestoreUtenti getGestoreUtenti() {
        return this.gestoreUtenti;
    }

    public GestoreStorico getGestoreStorico() {
        return this.gestoreStorico;
    }

    public void rimuoviClient(GestoreClient client) {
        clients.remove(client);
        System.out.println("Client rimosso. Clients rimanenti: " + clients.size());
    }

    public void aggiornaPartitaCorrente(Map<String, String> mappaParolePartita) {//funzione che aggiorna la partita quando, dopo 5 minuti, cambia.
        if(!this.mappaPartitaAttuale.isEmpty()){
            gestoreUtenti.aggiornaTimeoutUtenti(this.IdPartita());
            archiviaPartitaConclusa();//chiamo la funzione che salva sul file la partita appena finita.
        }
        this.timestampScadenzaPartita = System.currentTimeMillis() + (DURATA_PARTITA * 1000); //calcolo il tempo per aggiornare la variabile che tiene conto di quando avverà il prossimo aggiornamento.
        this.mappaPartitaAttuale = mappaParolePartita; //aggiorno anche la mappa con le nuove parole che vengono passate come argomento alla funzione 
        List<String> paroleDaInviare = new ArrayList<>(mappaParolePartita.keySet()); //e la stringa contente le parole separate da virgola da inviare al client

        Collections.shuffle(paroleDaInviare);
        this.paroleAttualiStringa = String.join(",", paroleDaInviare);

       for(GestoreClient client: clients) { //invio le parole a tutti i client collegati tramite un stringa json
            client.resettaPartita();
            String messaggio = gson.toJson(new RispostaJson("OK", "Nuova Partita Iniziata!", paroleAttualiStringa));
            client.messaggioAlClient(messaggio);
       }
    }

    public String controlloSoluzione(String[] proposta) { //funzione che controlla la soluzione proposta dal client
        if(proposta == null || proposta.length != 4) {
            return "Errore"; //controllo subito se il client ha mandato 4 parole
        }
        String tema = ""; //altrimenti faccio un for per controllare tutte le parole e mi salvo il tema se tutte queste appartangono allo stesso
        for(int i = 0; i < proposta.length; i++) {
            String parola = proposta[i].trim().toUpperCase();
            String temaCorrente = mappaPartitaAttuale.get(parola);
            if(temaCorrente == null) {
                return "Errore";
            } else {
                if(tema.equals("")) {
                    tema = temaCorrente;
                } else {
                    if(!tema.equals(mappaPartitaAttuale.get(parola))) {
                        return "";
                    }
                }
            }
        }
        return tema; //nel caso in cui le parole appartengano allo stesso tema, non invio una stringa vuota
    }

    public void archiviaPartitaConclusa() {//funzione che mi serve per salvare all'interno del file storico_partite la partita appena conclusa
        Map<String, List<String>> datiPartitaConcluso = new HashMap<>(); //inizializzo una mappa che questa volta ha come chaive il tema e come valore una lista contente le parole appartententi a quello specifico tema
        for(Map.Entry<String, String> entry: mappaPartitaAttuale.entrySet()) {//per ogni entry della mappa contente le mie parole della partita corrente
            String parola = entry.getKey(); //mi segno la parola che è la chiave
            String tema = entry.getValue();  // mi segno il tema che è il valore associato a quella chiave
            datiPartitaConcluso.computeIfAbsent(tema, k -> new ArrayList<>()).add(parola);
            //e aggiungo il tema se ancora non è presente all'interno della mappa finale con anche la parola aggiunta alla lista
        }

        Map<String, StatisticheUtente> statsGiocatori = new HashMap<>(); //creo una mappa che contenga, per ogni utente, le statistiche

        for(GestoreClient client: clients) { //e per ogni client collegato
            StatisticheUtente stats = new StatisticheUtente(client.getIndiceNuovoTema(), client.getNumErrori(), client.getPunteggioCorrente(), client.haVinto());// creo l'oggetto stats per ogni client, che quindi rispecchia le statistiche personali
            statsGiocatori.put(client.getUtenteCorrente(), stats); // e inserisco tutto nella mappa, con chiave lo username
        }
        // creo anche l'oggetto che passerò alla funzione di aggiornamento presente all'interno del gestoreStorico che mi consente alla fine di salvare tutti i dati relativi alla partita appena conclusa. utilizzo id - 1 perch+ essendo già aggiornato per la partita successiva devo quindi sottrarre 1.
        DatiPartitaPassata recordFinale = new DatiPartitaPassata(this.IdPartita() - 1, datiPartitaConcluso, statsGiocatori);
        this.gestoreStorico.aggiungiPartitaAlloStorico(recordFinale);
    }

    public String getStatisticheCorrenti() {
        long tempoRimasto = getSecondiRimanenti();
    
        int giocatoriTotali = 0;       // Tutti quelli loggati
        int giocatoriInCorso = 0;      // Stanno ancora giocando
        int giocatoriConclusi = 0;     // Hanno finito la scheda (indipendentemente dal risultato)
        int vittorie = 0;              // Hanno finito E vinto (in questo gioco coincidono, ma le separiamo per chiarezza)

        // Iteriamo su tutti i thread client attivi
        for (GestoreClient c : clients) {
            // Consideriamo solo chi ha fatto il login (utenteCorrente != null)
            if (c.getUtenteCorrente() != null) {
                giocatoriTotali++;
                
                if (c.haVinto()) {
                    // Se haVinto è true, significa che ha completato i 4 gruppi
                    giocatoriConclusi++;
                    vittorie++;
                } else {
                    // Se non ha vinto, sta ancora provando
                    giocatoriInCorso++;
                }
            }
        }

        // Costruiamo la stringa di risposta formattata
        StringBuilder sb = new StringBuilder();
        sb.append("--- STATISTICHE PARTITA CORRENTE (ID ").append(IdPartita()).append(") ---\n");
        sb.append("Tempo rimanente: ").append(tempoRimasto).append(" secondi\n");
        sb.append("Giocatori totali online: ").append(giocatoriTotali).append("\n");
        sb.append("Giocatori ancora in gioco: ").append(giocatoriInCorso).append("\n");
        sb.append("Giocatori che hanno concluso: ").append(giocatoriConclusi).append("\n");
        sb.append(" - di cui Vittorie: ").append(vittorie).append("\n");
        
        return sb.toString();
    }
    //funzione che mi permette di capire se un utente è già connesso o meno. (la utilizzo come controllo in più nel login utente)
    public synchronized boolean isUtenteConnesso(String username) {
        for(GestoreClient client: clients) {
            String nomeUtente = client.getUtenteCorrente();
            if(nomeUtente != null && nomeUtente.equals(username)) {
                return true;
            }
        }
        return false;
    }

}
