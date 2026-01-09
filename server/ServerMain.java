package server;

import java.io.*;

public class ServerMain {

    public static class Configurazione {
        public int port = -1; //default invalido, così da poter fare il controllo sulla correttezza della lettura.
        public String filePathUtenti = "";
        public String filePathStorico = "";
        public int secondiPartita = 200; //durata di default se manca nel file di conf
    }
    public static void main(String[] args){
        String fileConfigurazioneServer = "fileConfigurazioneServer.txt";

        Configurazione conf = letturaFileConfigurazioneServer(fileConfigurazioneServer);
        if(conf == null || conf.port == -1) {
            System.out.println("Errore nella lettura del file di conf");
            return;
        }

        System.out.println("Configurazione caricata:");
        System.out.println("Porta: " + conf.port);
        System.out.println("Utenti: " + conf.filePathUtenti);
        System.out.println("Storico: " + conf.filePathStorico);
        System.out.println("Durata: " + conf.secondiPartita);

        new GestoreServer(conf.port, conf.filePathStorico, conf.filePathUtenti, conf.secondiPartita).start();
    }

   public static Configurazione letturaFileConfigurazioneServer(String fileInput) {
        Configurazione config = new Configurazione();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileInput))) {
            String rigaLetta;
            
            while ((rigaLetta = reader.readLine()) != null) {
                // Ignoriamo righe vuote o commenti
                if (rigaLetta.trim().isEmpty() || rigaLetta.startsWith("#")) continue;

                // Dividiamo la riga alla prima occorrenza di ":"
                // Usiamo il limite 2 per evitare problemi se i percorsi file contengono ":" (es. C:\...)
                String[] rigaSpezzata = rigaLetta.split(":", 2);

                if (rigaSpezzata.length > 1) {
                    String chiave = rigaSpezzata[0].trim();
                    String valore = rigaSpezzata[1].trim();

                    switch (chiave) {
                        case "port":
                            try {
                                config.port = Integer.parseInt(valore);
                            } catch (Exception e) {
                                System.err.println("Errore formato porta nel file di config.");
                            }
                            break;
                        case "file_path_utenti":
                            config.filePathUtenti = valore;
                            break;
                        case "file_path_storico_partite":
                            config.filePathStorico = valore;
                            break;
                        case "durata_partita":
                            try {
                                config.secondiPartita = Integer.parseInt(valore);    
                            } catch (Exception e) {
                                System.err.println("Errore formato durata partita nel file di config.");
                            }
                            break;
                        default:
                            System.out.println("Chiave non riconosciuta nel config: " + chiave);
                    }
                }
            }
            return config;
            
        } catch (IOException e) {
            System.err.println("Errore IO lettura config: " + e.getMessage());
            return null; 
        }
    }
}
// in questo file si trova semplicemente la lettura del file di configurazione, nel quale è presente il numero di porta da utilizzare per il collegamento TCT sulla socket. Preso il numero inizializzo un'oggetto di tipo GestoreServer che come dice il nome gestirà le funzioni principali utilizzate nella comunicazione tra client e server.