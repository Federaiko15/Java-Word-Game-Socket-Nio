package client;

public class RichiestaClassifica {
    public String operation;
    public int limite; // -1 = Tutti, >0 = Top K
    public String usernameTarget; // Se diverso da null, cerchiamo il rank di questo utente

    public RichiestaClassifica(String operation, int limite, String usernameTarget) {
        this.operation = operation;
        this.limite = limite;
        this.usernameTarget = usernameTarget;
    }
}