package client;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class ClientMain {

    public static class Configurazione {
        String ip = "";
        int numero_porta = -1;
    }
    public static void main(String[] args) {

        String filePathConfClient = "fileConfigurazioneClient.txt";
        Configurazione variabili_conf = letturaFileConfigurazioneClient(filePathConfClient);

        if(variabili_conf.numero_porta == -1 || variabili_conf == null) {
            System.out.println("Errore nelle variabili di conf lato client");
            return;
        }

        NioClient nioClient = new NioClient(variabili_conf.ip, variabili_conf.numero_porta); //creo un oggetto NioClient al quale passo il numero di porta e l'indirizzo ip
        new Thread(nioClient).start(); //e lo passo ad un thread che gestirà l'interazione con il server tramite il channel e il buffer

        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();

        try {
            while (true) {
                System.out.print("> "); 
                String inputLine = scanner.nextLine().trim();

                if (inputLine.isEmpty()) continue;
                String jsonDaInviare = "";

                // --- COMANDI DI AUTENTICAZIONE E GESTIONE ---
                if (inputLine.startsWith("login")) {
                    String[] parti = inputLine.split(" ");
                    if (parti.length == 3) {
                        RichiestaUtente req = new RichiestaUtente("login", parti[1], parti[2]);
                        jsonDaInviare = gson.toJson(req);
                    }
                } 
                else if (inputLine.startsWith("register")) {
                    String[] parti = inputLine.split(" ");
                    if (parti.length == 3) {
                        RichiestaUtente req = new RichiestaUtente("register", parti[1], parti[2]);
                        jsonDaInviare = gson.toJson(req);
                    }
                } 
                else if (inputLine.startsWith("logout")) {
                     RichiestaUtente req = new RichiestaUtente("logout", null, null);
                     jsonDaInviare = gson.toJson(req);
                } 
                else if (inputLine.startsWith("requestPlayerStats")) {
                    RichiestaUtente req = new RichiestaUtente("requestPlayerStats", null, null);
                    jsonDaInviare = gson.toJson(req);
                } 
                else if (inputLine.startsWith("updateCredentials")) {
                    String[] parti = inputLine.split(" ");
                    if (parti.length == 5) {
                        AggiornamentoCredenziali req = new AggiornamentoCredenziali("updateCredentials", parti[1], parti[2], parti[3], parti[4]);
                        jsonDaInviare = gson.toJson(req);
                    }
                }
                else if (inputLine.startsWith("requestGameInfo")) {
                    String[] parti = inputLine.split(" ");
                    if(parti.length == 1) {
                        RichiestaPartita req = new RichiestaPartita("requestGameInfo", -1);
                        jsonDaInviare = gson.toJson(req);
                    } else if (parti.length == 2) {
                        try {
                            int id = Integer.parseInt(parti[1]);
                            RichiestaPartita req = new RichiestaPartita("requestGameInfo", id);
                            jsonDaInviare = gson.toJson(req);
                        } catch (NumberFormatException e) {
                            System.out.println("ID non valido.");
                        }
                    }
                } 
                else if(inputLine.startsWith("requestGameStats")) {
                    String[] parti = inputLine.split(" ");
                    if(parti.length == 1) {
                        RichiestaPartita req = new RichiestaPartita("requestGameStats", -1);
                        jsonDaInviare = gson.toJson(req);
                    } else if (parti.length == 2) {
                        try {
                            int id = Integer.parseInt(parti[1]);
                            RichiestaPartita req = new RichiestaPartita("requestGameStats", id);
                            jsonDaInviare = gson.toJson(req);
                        } catch (NumberFormatException e) {
                            System.out.println("ID non valido.");
                        }
                    }
                } else if (inputLine.startsWith("submitProposal")) {
                    // Rimuovo il comando "submitProposal" dalla stringa per isolare le parole e utilizzo trim per ripulire la stringa da spazi
                    // Esempio input: "submitProposal parola1,parola2,parola3,parola4"
                    // parole diventa: "parola1,parola2,parola3,parola4"
                    String parole = inputLine.replaceFirst("submitProposal", "").trim();
                    
                    if(parole.isEmpty()) {
                         System.out.println("Sintassi errata. Devi inserire le parole."); 
                         System.out.println("Esempio: submitProposal p1,p2,p3,p4");
                    } else {
                        String[] parti = parole.split(",");
                        
                        if(parti.length == 4) {
                            // Pulizia: rimuovo spazi extra attorno alle parole
                            for(int i=0; i<4; i++) parti[i] = parti[i].trim();
                            
                            PropostaSoluzione proposta = new PropostaSoluzione("submitProposal", parti);
                            jsonDaInviare = gson.toJson(proposta);
                        } else {
                             System.out.println("Formato errato: devi inserire esattamente 4 parole separate da virgola dopo il comando.");
                        }
                    }
                } else if(inputLine.startsWith("requestLeaderboard")) {
                    String argomenti = inputLine.replaceFirst("requestLeaderboard", "").trim();
                    RichiestaClassifica reqClassifica = null;

                    if(argomenti.isEmpty()) {//se il messaggio contiene quindi solamente requestLeaderboard, vuol dire che devo mandare la classifica completa
                        reqClassifica = new RichiestaClassifica("requestLeaderboard", -1, null);
                    } else {
                        try {
                            int k = Integer.parseInt(argomenti);
                            reqClassifica = new RichiestaClassifica("requestLeaderboard", k, null);
                        } catch (NumberFormatException e) {
                            reqClassifica = new RichiestaClassifica("requestLeaderboard", 0, argomenti);
                        }
                    }
                    jsonDaInviare = gson.toJson(reqClassifica);
                }
                else {
                    System.out.println("Comando non riconosciuto.");
                }
                if (!jsonDaInviare.isEmpty()) {
                    nioClient.accodaMessaggio(jsonDaInviare); //quando ho trovato il messaggio da inviare e aver creto il json appropriato per l'operazione, accodo il messaggio per gestire la scrittura tramite il nioClient
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        scanner.close();
    }
    public static Configurazione letturaFileConfigurazioneClient(String filePath) {
        Configurazione conf = new Configurazione();

        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String rigaLetta;
            while ((rigaLetta = reader.readLine()) != null) {
                 if (rigaLetta.trim().isEmpty() || rigaLetta.startsWith("#")) continue;

                // Dividiamo la riga alla prima occorrenza di ":"
                // Usiamo il limite 2 per evitare problemi se i percorsi file contengono ":" (es. C:\...)
                String[] rigaSpezzata = rigaLetta.split(":", 2);

                if (rigaSpezzata.length > 1) {
                    String chiave = rigaSpezzata[0].trim();
                    String valore = rigaSpezzata[1].trim();

                    switch (chiave) {
                        case "server_port":
                            try {
                                conf.numero_porta = Integer.parseInt(valore);
                            } catch (Exception e) {
                                System.err.println("Errore formato porta nel file di conf.");
                            }
                            break;
                        case "server_ip":
                            conf.ip = valore;
                            break;
                        default:
                            System.out.println("Chiave non riconosciuta nel conf: " + chiave);
                    }
                }
            }
            return conf;
        } catch(IOException e) {
            System.out.println("Problemi nella lettura del file di conf lato client");
            return null;
        }
    }
}
// in questo file è presente il controllo dei messaggi mandati dal client tramite il terminale. Dipende il tipo di operazione da eseguire vengono create le apposite stringhe json tramite anche le opportune classi per mandare le informazioni necessarie al server per rispondere poi correttamente alle richieste