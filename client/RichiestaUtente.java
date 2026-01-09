package client;

public class RichiestaUtente {
    String operation;
    String userName;
    String password;

    public RichiestaUtente(String op, String userName, String password) {
        this.operation = op;
        this.userName = userName;
        this.password = password;
    }
}
