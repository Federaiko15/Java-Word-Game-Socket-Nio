package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NioClient implements Runnable {
    private final String ip;
    private final int port;
    private SocketChannel clientChannel;
    private Selector selector;
    private final GestoreProtocollo protocollo = new GestoreProtocollo();
    
    // Coda per gestire i messaggi da inviare in modo thread-safe
    private final BlockingQueue<String> codaMessaggi = new LinkedBlockingQueue<>();
    private volatile boolean running = true; //variabile che mi aiuta a capire se un client è attivo o meno

    public NioClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            // creo il selector e configuro il channel in modalità non bloccante, così da poter gestire più operazion insieme.
            selector = Selector.open();
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress(ip, port));
            clientChannel.register(selector, SelectionKey.OP_CONNECT);

            System.out.println("Client di rete avviato...");

            while (running) {
                // Controllo se c'è qualcosa da scrivere (inviato dal ClientMain)
                while (!codaMessaggi.isEmpty()) {
                    String msg = codaMessaggi.poll();
                    scriviSuSocket(msg); //se la coda contiente un messaggio lo prendo dalla coda e chiamo la funzione di invio
                }

                // selectNow() non blocca, o select(100) aspetta poco. 
                // Usiamo select(100) per dare tempo al loop di girare e controllare la codaMessaggi
                if (selector.select(100) == 0) continue;

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); //dal selector prendo le selection key che si sono registrate e le inserisco in un iteratore
                while (iterator.hasNext()) { //fino a quando l'iteratore contiene elementi, le selection key
                    SelectionKey key = iterator.next();
                    iterator.remove();
                //e controllo la modalità in cui si trova la key, perchè se è appena arrivata allora chiamo la funzione handleConnect, invece se si trova in modalità lettura chiamo handleRead. la funzione di scrittura la gestisco manualmente perchè ho bisogno di poter gestire i messaggi del server che non siano le risposte, tipo i messaggi di aggiornamento delle partite
                    if (!key.isValid()) continue;
                    if (key.isConnectable()) handleConnect(key);
                    else if (key.isReadable()) handleRead(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo pubblico usato dal Main per inviare dati
    public void accodaMessaggio(String msg) {
        codaMessaggi.add(msg);
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        System.out.println("--- CONNESSO AL SERVER ---");
        channel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        int bytesRead;

        try {
            bytesRead = channel.read(buffer);
        } catch (IOException e) {
            bytesRead = -1;
        }

        if (bytesRead == -1) {
            System.out.println("\nIl server si è disconnesso");
            System.out.println("Chiusura dell'applicazione in corso...");
            running = false;
            channel.close();
            System.exit(0);
            return;
        }

        buffer.flip();
        String jsonRicevuto = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
        
        // Delego al protocollo SOLO la visualizzazione/parsing, NON l'input utente
        protocollo.gestisciRispostaServer(jsonRicevuto);
    }

    private void scriviSuSocket(String msg) throws IOException {
        if (clientChannel != null && clientChannel.isConnected()) {
            // Aggiungiamo \n perché il server usa readLine()
            ByteBuffer buffer = ByteBuffer.wrap((msg + "\n").getBytes(StandardCharsets.UTF_8));
            clientChannel.write(buffer);
        }
    }
}