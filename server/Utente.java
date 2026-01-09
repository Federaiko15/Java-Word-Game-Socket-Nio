package server;

import java.util.ArrayList;
import java.util.List;

public class Utente {
    private String username;
    private String password;
    private int punteggio;
    public int numPartiteTotali;
    public int numPartiteVinte;
    public int numPartitePerse;
    public int numPartiteNonFinite;
    public int streakCorrenteVittorie;
    public int maxStreak;
    public int vittoriePerfette; //senza errori
    //variabili per istogramma degli errori
    //[0] 0 errori, [1] 1 errore, [2] 2 errori, [3] 3 errori, [4] 4 errori, [5] finite per timeout
    public int[] istogrammaErrori = new int[6];

    //variabili per statistiche della partita corrente, utili per quando un utente si disconnette e riconnete nella stessa partita, così da non perdere i progressi.
    public int idPartitaCorrente = -1;
    public List<String> paroleIndovinateTemp = new ArrayList<>();
    public List<String> temiIndovinatiTemp = new ArrayList<>();
    public int erroriTemp = 0;
    public int punteggioTemp = 0;
    public boolean haVintoTemp = false;
    public boolean haPersoTemp = false;

    public Utente(String nome, String pass) {
        this.username = nome;
        this.password = pass;
        this.punteggio = 0;
    }

    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public int getPunteggio() {return punteggio;}

    public boolean controlloPassword(String pass) { //metodo che utilizzo quando un utente prova a fare il login per capire se inserisce
        return this.password.equals(pass);  // una password corretta.
    }
    public void incrementaPunteggio(int puntiDaAggiungere) {
        this.punteggio += puntiDaAggiungere;
    }
    public String statsGiocatore() {
        String messaggio = this.username + " hai ottenuto " + Integer.toString(punteggio)+ " punti fino ad ora";
        return messaggio;
    }
    public void cambioCredenziali(String nuovoUsername, String nuovaPass) {
        this.username = nuovoUsername;
        this.password = nuovaPass;
    }
    // quando cambia partita resetto tutte le variabili che tenevano conto delle statistiche della partita appena terminata
    public void resettaStatoTemporaneo(int idNuovaPartita) {
        this.idPartitaCorrente = idNuovaPartita;
        this.paroleIndovinateTemp.clear();
        this.temiIndovinatiTemp.clear();
        this.erroriTemp = 0;
        this.punteggioTemp = 0;
        this.haVintoTemp = false;
        this.haPersoTemp = false;
    }

    //metodo per aggiornare le statistiche a fine partita
    public void consolidaStatistichePartita(boolean partitaTerminata) {
        this.numPartiteTotali++;
        if(this.haVintoTemp) {
            this.numPartiteVinte++;
            this.streakCorrenteVittorie++;
            if(this.streakCorrenteVittorie > maxStreak) {
                this.maxStreak = streakCorrenteVittorie;
            }
            if(this.erroriTemp == 0) {
                this.vittoriePerfette++;
            }
            if(erroriTemp < 4) {
                this.istogrammaErrori[erroriTemp]++;
            }
        } else {
            this.streakCorrenteVittorie = 0;
            if(this.haPersoTemp) {
                this.numPartitePerse++;
                this.istogrammaErrori[4]++;
            } else if(partitaTerminata) {
                this.numPartiteNonFinite++;
                this.istogrammaErrori[5]++;
            }
        }
    }
    //getter per le percentuali di vittorie e sconfitte
    public double getWinRate() {
        return (numPartiteTotali == 0)? 0.0: ((double)numPartiteVinte / numPartiteTotali) * 100;
    }
    public double getLossRate() {
        return (numPartiteTotali == 0.0)? 0.0: ((double)numPartitePerse / numPartiteTotali) * 100;
    }
}
