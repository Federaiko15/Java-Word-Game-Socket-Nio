package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;
import java.net.*;

public class GestoreClient implements Runnable{
    Socket client; //variabile per la socket che collega server e client
    GestoreServer server;
    private final Gson gson;  //l'oggetto gson che mi serve per la lettura e per la scrittura dei file json
    private BufferedReader server_input; //il reader per leggere dalla socket
    private PrintWriter server_output; // il writer per scrivere nella socket
    private String utenteCorrente = null; //variabile nella quale salvo dell'utente collegato a questo collegamento client-server
    private String[] temiIndovinati = new String[4]; // un array lungo 4 nella quale salvo i temi indovinati
    private int indiceNuovoTema = 0;//l'indice che mi fa inserire il tema appena indovinato nella posizione giusta
    private boolean haVinto = false; // variabili booleane persapere se un utente è registrato,loggato,havinto,haperso
    private boolean Loggato_Registrato = false;
    private boolean haPerso = false;
    private List<String> paroleDaIndovinare; //lista nella quale salvo le parole ancora da indovinare.
    private String[] paroleIndovinate = new String[16]; //invece in questo array mi salvo le parole che l'utente ha già raggruppato correttamente 
    private int indiceParoleIndovinate = 0; //l'indice per inserire le parole nella posizione corretta
    private int numErrori = 0; // variabile in qui salvo il numero di errori
    private int punteggioCorrente = 0; // variabile in qui salvo il punteggio corrente, collegato alla partita(non punteggio totale dell'utente)

    public GestoreClient(Socket client, GestoreServer server) {
        this.client = client;
        this.server = server;
        this.gson = new Gson();
        this.paroleDaIndovinare = new ArrayList<>(server.mappaPartitaAttuale.keySet()); //nella lista delle parole da indovinare inserisco le parole che avevo trovato con il server.
    }
    
    @Override
    public void run() {
        try {
            server_input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            server_output = new PrintWriter(client.getOutputStream(), true);
            //inizializzo il buffer per la lettura dall'inputstream della socket e il writer per scrivere.

            String messaggioDiSpiegazione = 
                "BENVENUTO NEL GIOCO DI PAROLE!\n" +
                "Comandi disponibili:\n" +
                "1.  login <user> <pass>           -> Accedi al gioco\n" +
                "2.  register <user> <pass>        -> Crea un nuovo account\n" +
                "3.  logout                        -> Disconnetti\n" +
                "4.  submitProposal <p1>,<p2>...   -> Invia soluzione (4 parole, separate da virgola)\n" +
                "5.  requestGameInfo [id]          -> Info sulla partita (corrente o ID specifico)\n" +
                "6.  requestGameStats [id]         -> Statistiche globali partita\n" +
                "7.  requestPlayerStats            -> Le tue statistiche personali\n" +
                "8.  requestLeaderboard            -> Mostra classifica completa\n" +
                "9.  requestLeaderboard <K>        -> Mostra i primi K giocatori\n" +
                "10. requestLeaderboard <user>     -> Mostra la posizione di un utente\n" +
                "11. updateCredentials <oldUser> <oldPass> <newUser> <newPass>\n" +
                "\nPer iniziare, identificati con login o register.";

            RispostaJson messaggioBenvenuto = new RispostaJson("OK", messaggioDiSpiegazione, null);
            server_output.println(gson.toJson(messaggioBenvenuto)); //e invio subito un messaggio di benvenuto
           
            String messaggio_client_JSON;//variabile in cui salvo il messaggio json che leggo dalla socket
            while((messaggio_client_JSON = server_input.readLine()) != null) {
               String risposta = gestione_comando(messaggio_client_JSON, gson); // se la lettura è andata a buon fine, chiamo la funzione che gestisce il messaggio
               server_output.println(risposta); // e invio la risposta basato sul messaggio ricevuto
            }
        } catch(IOException e) {
            System.err.println("Errore nell'apertura dell'input outoput lato server");
        } finally {
            server.rimuoviClient(this);
        }
    }
    //funzioni get che utilizzo nella gestione delle statistiche richieste dal client.
    public String getUtenteCorrente() {return this.utenteCorrente;}
    public int getIndiceNuovoTema() {return this.indiceNuovoTema;}
    public int getNumErrori() {return this.numErrori;}
    public int getPunteggioCorrente() {return this.punteggioCorrente;}
    public boolean haVinto() {return this.haVinto;}

    public void messaggioAlClient(String messaggio) {
        if(server_output != null) {
            server_output.println(messaggio);
        }// funzione che manda direttamente un messaggio al client, utilizzata per l'aggiornamento della partita per avvisare ogni client
    }

    public void resettaPartita() {
        this.haVinto = false;
        this.haPerso = false;
        this.indiceNuovoTema = 0;
        this.temiIndovinati = new String[4];
        this.paroleDaIndovinare = new ArrayList<>(server.mappaPartitaAttuale.keySet());
        this.paroleIndovinate = new String[16];
        this.indiceParoleIndovinate = 0;
        this.numErrori = 0;
        this.punteggioCorrente = 0;
        //quando aggiorna la partita chiamo anche una funzione che mi permette di resettare tutte le variabili che caratterizzano lo stato corrente del giocatore
    }

    public String gestione_comando(String comando, Gson gson) {
        // utilizzo il tree model per deserializzare il comando, così da poter ricavare subito il tipo di operazione.
    
        JsonObject root = JsonParser.parseString(comando).getAsJsonObject();
        if(!root.has("operation")) {
            System.out.println("Errore nella lettura del comando mandato dal client");

            return "Comando non riconosciuto";
        }

        String operazioneMessaggio = root.get("operation").getAsString();//trovata quindi l'operazione, utilizzo lo switch per capire come rispondere

        switch (operazioneMessaggio) {
            case "register": //se viene richiesta una operazine di registrazione
                if(!this.Loggato_Registrato) {// controllo se è già stata fatta l'identificazione
                    RichiestaUtente nuovoUtenteReg = gson.fromJson(root, RichiestaUtente.class);
                    //dal json estraggo gli elementi e li inserisco nell'oggetto con la classe corrispettiva
                    boolean registrato = server.getGestoreUtenti().registraUtente(nuovoUtenteReg.userName, nuovoUtenteReg.password);
                    // e vedo se l'utente può fare l'accesso con quelle credenziali
                    if(registrato) { // se il controllo andato a buon fine, costruisco la risposta e setto le variabili nel modo corretto
                        this.Loggato_Registrato = true;
                        this.utenteCorrente = nuovoUtenteReg.userName;
                        String paroleAttuali = server.invioParole();
                        long sec = server.getSecondiRimanenti();
                        System.out.println("Regitrazione dell'utente "+nuovoUtenteReg.userName+" avvenuta con successo");
                        return gson.toJson(new RispostaJson("OK", "Registrazione avvenuta con successo, id partita corrente: "+server.IdPartita()+" con "+sec+" secondi rimanenti", paroleAttuali));
                    } else {
                        return gson.toJson(new RispostaJson("ERROR", "Registrazione non avvenuta", null));
                    }
                } else {
                    return gson.toJson(new RispostaJson("ERROR", "Hai già fatto la registrazione", null));
                }
            case "login": //faccio le stesse cose anche qui (le stesse cose del caso di register), in più però controllo se un utente è già collegato con le credenziali fornite, se l'utente aveva già partecipato alla partita ancora in corso, così da poter recuperare le informazioni che aveva registrato durante la partita.
                if(!this.Loggato_Registrato) {
                    RichiestaUtente nuovoUtenteLog = gson.fromJson(root, RichiestaUtente.class);
                    if(server.isUtenteConnesso(nuovoUtenteLog.userName)) {
                        return gson.toJson(new RispostaJson("ERROR", "L'utente con queste credenziali è attivo in partita in questo momento", null));
                    }

                    boolean loginAccettato = server.getGestoreUtenti().loginUtente(nuovoUtenteLog.userName, nuovoUtenteLog.password);
                    if(loginAccettato) {
                        this.utenteCorrente = nuovoUtenteLog.userName;
                        String paroleAttuali = server.invioParole();
                        System.out.println("Login dell'utente " +nuovoUtenteLog.userName + " avvenuto con successo");
                        this.Loggato_Registrato = true;

                        //recupero l'utente per capire se aveva già partecipato alla partita o meno
                        Utente utente = server.getGestoreUtenti().ottieniInfoUtente(this.utenteCorrente);
                        int id = server.IdPartita();
                        if(utente.idPartitaCorrente != id) {
                            //vuold dire che nell'utente avrò le statistiche di partite passate, quindi resetto sia lo stato temporaneo e anche la partita, aggiorno lo stato salvando tutti sul file degli utenti
                            utente.resettaStatoTemporaneo(id);
                            this.resettaPartita();
                            server.getGestoreUtenti().aggiornaStatoPartitaCorrente(this.utenteCorrente, id, new ArrayList<>(), new ArrayList<>(), 0, 0, false, false);
                        } else {
                            // devo caricare i dati appartenti all'utente
                            System.out.println("Ripristino dati per l'utente "+this.utenteCorrente);
                            this.indiceNuovoTema = utente.temiIndovinatiTemp.size();
                            this.temiIndovinati = utente.temiIndovinatiTemp.toArray(new String[0]);
                            this.paroleIndovinate = utente.paroleIndovinateTemp.toArray(new String[0]);
                            this.indiceParoleIndovinate = utente.paroleIndovinateTemp.size();
                            this.numErrori = utente.erroriTemp;
                            this.punteggioCorrente = utente.punteggioTemp;
                            this.haPerso = utente.haPersoTemp;
                            this.haVinto = utente.haVintoTemp;
                            //ricostruisco le parole da indovinare togliendo quelle già trovate
                            this.paroleDaIndovinare = new ArrayList<>(server.mappaPartitaAttuale.keySet());
                            for(String parolaTrovata: utente.paroleIndovinateTemp) {
                                this.paroleDaIndovinare.removeIf(parola -> parola.equalsIgnoreCase(parolaTrovata));
                            }
                        }
                        long sec = server.getSecondiRimanenti();
                        return gson.toJson(new RispostaJson("OK", "Login avvenuto con successo, id partita corrente: "+id+" con "+sec+" secondi rimanenti", paroleAttuali));
                    } else {
                        return gson.toJson(new RispostaJson("ERROR", "Login non avvenuto", null));
                    }
                } else {
                    return gson.toJson(new RispostaJson("ERROR", "Hai già fatto il login", null));
                } 
            case "logout": //setto la variaile a false e mando un messaggio di Disconessione per far capire al client cosa deve fare
                System.out.println("client disconnesso");
                server.getGestoreUtenti().salvaUtenteAllLogout();
                this.Loggato_Registrato = false;
                RispostaJson rispostaOut = new RispostaJson("OK", "Disconnesso.", null);
                return gson.toJson(rispostaOut);
            case "submitProposal": // se invece viene richiesta l'operazione di proposta di una soluzione di raggruppamento
                if(this.haVinto) { // guardo se ha già vinto o ha già perso, avvisandolo che dovrà aspettare la fine della partita per poter fare altre proposte
                    long sec = server.getSecondiRimanenti();
                    return gson.toJson(new RispostaJson("OK", "Hai già completato la partita, attendi " +sec+ " per il prossimo turno", null));
                }
                if(this.haPerso) {
                    return gson.toJson(new RispostaJson("OK", "Hai perso la partita perchè hai raggiunto -16 punti", null));
                }
                if(this.Loggato_Registrato) { // altrimenti se l'utente si è identificato
                    PropostaSoluzione proposta = gson.fromJson(root, PropostaSoluzione.class);
                    String temaSoluzioneCorretta = server.controlloSoluzione(proposta.words);
                    // preparo l'oggetto nel quale posso inserire i dati mandati dall'utente e controllo la proposta di soluzione analizzando le 4 parole mandate 
                    if(temaSoluzioneCorretta.equals("Errore")) {//se il controllo ritorna Errore vuol dire che è presente un errore grammaticale nella soluzione, che quindi non calcolo come una proposta errata
                         return gson.toJson(new RispostaJson("OK", "Proposta errata. Inserisci solo parole appartenenti alla partita attuale", null));
                    }
                    if(temaSoluzioneCorretta.equals("")) { // se il controllo ha ritornato una stringa vuota
                        this.punteggioCorrente -= 4; //decremento il punteggio e aumento il numero di errori, modificando anche la variabile complessiva del punteggio collegato all'utente
                        this.numErrori++;
                        server.getGestoreUtenti().aggiornaPunteggio(this.utenteCorrente, -4);
                        if(this.punteggioCorrente <= -16) {
                            this.haPerso = true; // e se ha raggiungo un punteggio più basso di -16 setto la variabile haperso a true
                        }
                        //aggiorno lo stato dell'utente ad ogni proposta, però il salvataggio su file avverrà solo se l'utente ha vinto o perso
                        server.getGestoreUtenti().aggiornaStatoPartitaCorrente(this.utenteCorrente, server.IdPartita(), Arrays.asList(this.paroleIndovinate).subList(0, indiceParoleIndovinate), Arrays.asList(this.temiIndovinati).subList(0, indiceNuovoTema), this.punteggioCorrente, this.numErrori, false, haPerso);

                        return gson.toJson(new RispostaJson("OK", "Sbagliata", null));
                    } else {
                        for(int i = 0; i < indiceNuovoTema; i++) {
                            if(temaSoluzioneCorretta.equals(temiIndovinati[i])) {
                                return gson.toJson(new RispostaJson("OK", "Soluzione già proposta", null));
                            } //altrimenti se il tema che è stato ricavando analizzando la proposta si trova già nell'array, avviso che l'utente ha mandato una soluzione già proposta
                        }
                        // se arrivo qui vuol dire che è arrivata una nuova proposta corretta e non già data,
                        // quindi mi salvo le parole proposte e il loro tema.
                        for(int i = 0; i < proposta.words.length; i++) {
                            String parolaPulita = proposta.words[i].trim().toUpperCase();
                            this.paroleIndovinate[indiceParoleIndovinate] = parolaPulita;
                            this.indiceParoleIndovinate++;
                            //utilizzo removeIf per sicurezza, così posso utilizzare la funzione che ignora differenze come spazi.
                            this.paroleDaIndovinare.removeIf(p -> p.equalsIgnoreCase(parolaPulita));
                        }

                        this.temiIndovinati[this.indiceNuovoTema] = temaSoluzioneCorretta;
                        this.indiceNuovoTema++;
                        this.punteggioCorrente += 6;
                        server.getGestoreUtenti().aggiornaPunteggio(this.utenteCorrente, 6);

                        if(this.indiceNuovoTema == 3) {//controllo se l'utente ha indovinato 3 gruppi su 4, perchè in questo caso l'ultima soluzione sarebbe scontata e chiudo prima la partita per l'utente
                            System.out.println("Sono stati indovinati 3 gruppi su 4, quindi facciamo concludere la partita automaticamente");
                            List<String> ultimeParole = new ArrayList<>(this.paroleDaIndovinare);
                            String ultimoTema = "";
                            if(!ultimeParole.isEmpty()) {
                                ultimoTema = server.mappaPartitaAttuale.get(ultimeParole.get(0));//guardiamo la prima parola per avere il tema
                            }//prendo le ultime parole rimaste da indovinare dalla lista e li inserisco in una lista di supporto dalla quale estraggo la prima parola per prendere il tema e salvarlo e poi inserisco le parole presenti nel mio array che conterrà quindi tutte le parole indovinate
                            for(String parola: ultimeParole) {
                                this.paroleIndovinate[indiceParoleIndovinate] = parola;
                                this.indiceParoleIndovinate++;
                            }
                            this.paroleDaIndovinare.clear();
                            temiIndovinati[indiceNuovoTema] = ultimoTema;
                            indiceNuovoTema++;
                        }
                        //se adesso i temi indovinati sono quattro, vuol dire che l'utente ha vinto la partita corrente, quindi chiamo la funzione del server che avviserà tutti i client
                        if(this.indiceNuovoTema == 4) {
                            this.haVinto = true;
                        }

                        server.getGestoreUtenti().aggiornaStatoPartitaCorrente(this.utenteCorrente, server.IdPartita(), Arrays.asList(this.paroleIndovinate).subList(0, indiceParoleIndovinate), Arrays.asList(this.temiIndovinati).subList(0, indiceNuovoTema), this.punteggioCorrente, this.numErrori, this.haVinto, false);

                        if(this.haVinto) {
                            server.partitaVinta(this.utenteCorrente);
                            long sec = server.getSecondiRimanenti();
                            return gson.toJson(new RispostaJson("OK", "COMPLIMENTI HAI VINTO. Attendi "+sec+" per il prossimo turno", null));
                        }
                        return gson.toJson(new RispostaJson("OK", "Corretta", null));
                    }
                } else {
                    return gson.toJson(new RispostaJson("OK", "Per proporre una soluzione devi prima identificarti", null));
                }
            case "requestPlayerStats"://funzione che mi mostra il punteggio totale, non quello corrente, di un giocatore
                if(!this.Loggato_Registrato) {
                    return gson.toJson(new RispostaJson("ERROR", "Prima devi identificarti", null));
                }
                String messaggioStats = server.getGestoreUtenti().ottieniStats(this.utenteCorrente, server.IdPartita());
                RispostaJson risposta = new RispostaJson("OK", messaggioStats, null);
                return gson.toJson(risposta);
            case "updateCredentials"://opzione di operazione di aggiornamento delle credenziali
                //inserisco nella classe apposita le informazioni mandate dal client e chiamo la funzione di loginUtente del server per vedere se le prime credenziali mandate corrispondo effettivamente ad un utente. nel caso in cui si riscontra un match positivo, chiamo la funzione di aggiornamento delle credenziali
                AggiornamentoCredenziali update = gson.fromJson(root, AggiornamentoCredenziali.class);
                boolean utenteRegistrato = server.getGestoreUtenti().loginUtente(update.vecchioUsername, update.vecchiaPassword);
                if(utenteRegistrato) {
                    this.utenteCorrente = update.nuovoUsername;
                    server.getGestoreUtenti().aggiornaCredenziali(this.utenteCorrente, update.nuovoUsername, update.nuovaPassword);
                        RispostaJson rispostaAggiornamento = new RispostaJson("OK", "Cambio credenziali avvenuto con successo", null);
                        return gson.toJson(rispostaAggiornamento);
                } else {
                    RispostaJson rispostaAggiornamento = new RispostaJson("ERROR", "Credenziali utente non valide", null);
                    return gson.toJson(rispostaAggiornamento);
                }
            case "requestGameInfo":
                if(!this.Loggato_Registrato) {
                    return gson.toJson(new RispostaJson("ERROR", "Prima devi identificarti", null));
                }
                RichiestaPartita partitaInfo = gson.fromJson(root, RichiestaPartita.class);
                if(partitaInfo.idPartita == -1) { //questo valore, il -1, viene mandato automaticamente dal client quando vingono richieste le info
                    long sec = server.getSecondiRimanenti(); //sulla partita corrente.
                    int soluzioniCorrette = indiceNuovoTema;
                    String paroleMancanti = String.join(", ", this.paroleDaIndovinare);
                    String messaggio = "Info Partita Corrente:\nTempo rimanente al termine della partita: " +sec+" secondi\nProposte corrette: " +soluzioniCorrette+" \nParole ancora da raggruppare: "+paroleMancanti+" \nNumero errori: "+numErrori+" \nPunteggio corrente: "+punteggioCorrente+"\n";
                    return gson.toJson(new RispostaJson("OK", messaggio, null));
                } else if(partitaInfo.idPartita > server.IdPartita()) {
                    return gson.toJson(new RispostaJson("OK", "Partita ancora da giocare", null));
                } else {
                    String infoStorico = server.getGestoreStorico().recuperaInfoPartita(partitaInfo.idPartita, this.utenteCorrente);
                    return gson.toJson(new RispostaJson("OK", infoStorico, null));
                }
            case "requestGameStats":
                if(!this.Loggato_Registrato) {
                    return gson.toJson(new RispostaJson("ERROR", "Prima devi identificarti", null));
                }
                RichiestaPartita reqStats = gson.fromJson(root, RichiestaPartita.class);
                int idRichiesta = reqStats.idPartita;
                int idCorrente = server.IdPartita();

                if(idRichiesta == -1) {
                    String statsCorrenti = server.getStatisticheCorrenti();
                    return gson.toJson(new RispostaJson("OK", statsCorrenti, null));
                } else if(idRichiesta > idCorrente) {
                    return gson.toJson(new RispostaJson("ERROR", "Partita non ancora esistente", null));
                } else {
                    String statsStoriche = server.getGestoreStorico().recuperaStatisticheGlobali(idRichiesta);
                    return gson.toJson(new RispostaJson("OK", statsStoriche, null));
                }
            case "requestLeaderboard":
                //la classifica è pubblica, quindi non richiedo che l'utente debba essere identificato per richiedere la classifica
                RichiestaClassifica reqClassifica = gson.fromJson(root, RichiestaClassifica.class);
                String risultatoClassifica = server.getGestoreUtenti().calcolaClassifica(reqClassifica.limite, reqClassifica.usernameTarget);
                return gson.toJson(new RispostaJson("OK", risultatoClassifica, null));
            default:
                return "Comando non ancora implementato";
        }
        // le ultime due funzioni invece sono richieste di informazioni/statistiche riguardanti o le partite passate o la partita corrente. controllo infatti se l'indice mandato sia: -1, significa partita corrente, maggiore dell'id della partita corrente, quindi sarebbe un riferimento ad una partita ancora da giocare, oppure un indice inferiore quindi si deve andare a ricercare la partita nel file storico_partite.json
    }
}
