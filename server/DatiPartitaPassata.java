package server;

import java.util.List;
import java.util.Map;

public class DatiPartitaPassata {
    public int idPartita;
    public Map<String, List<String>> soluzione; 
    public Map<String, StatisticheUtente> statisticheGiocatori;

    public DatiPartitaPassata(int idPartita, Map<String, List<String>> soluzione, Map<String, StatisticheUtente> stats) {
        this.idPartita = idPartita;
        this.soluzione = soluzione;
        this.statisticheGiocatori = stats;
    }
}

class StatisticheUtente {
    public int gruppiIndovinati;
    public int errori;
    public int punteggio;
    public boolean haVinto;

    public StatisticheUtente(int gruppiIndovinati, int errori, int punteggio, boolean haVinto) {
        this.gruppiIndovinati = gruppiIndovinati;
        this.errori = errori;
        this.punteggio = punteggio;
        this.haVinto = haVinto;
    }
}