package client;

public class AggiornamentoCredenziali {
    String operation;
    String vecchioUsername;
    String vecchiaPassword;
    String nuovoUsername;
    String nuovaPassword;

    public AggiornamentoCredenziali(String op, String vu, String vp, String nu, String np) {
        this.operation = op;
        this.vecchioUsername = vu;
        this.vecchiaPassword = vp;
        this.nuovoUsername = nu;
        this.nuovaPassword = np; 
    }
}
