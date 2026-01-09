package server;

public class RichiestaUtente {
    String operation;
    String userName;
    String password;

    public RichiestaUtente(String op, String nome, String pass) {
        this.operation = op;
        this.userName = nome;
        this.password = pass;
    }
}
