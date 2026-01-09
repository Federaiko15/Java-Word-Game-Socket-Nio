# Java Word Game (Socket NIO)

Progetto finale per il corso di Laboratorio 3.
Applicazione Client-Server multithread per un gioco di parole, sviluppata in Java.

## Autore
Federico Gueli

## Caratteristiche Principali

### Server
* **Architettura:** Multithreaded con Thread Pool (CachedThreadPool).
* **Gestione Concorrenza:** Uso di strutture thread-safe (`ConcurrentHashMap`, `CopyOnWriteArrayList`).
* **Persistenza:** Salvataggio stato utenti e storico partite su file JSON con strategia di salvataggio ottimizzata (write-behind).
* **Pattern:** Singleton (Managers), Observer (per aggiornamenti stato).

### Client
* **Tecnologia:** Java NIO (Non-blocking I/O) con `SocketChannel` e `Selector`.
* **Architettura:** Loop di rete separato dal thread UI (CLI).
* **Protocollo:** Messaggi JSON customizzati.

## Requisiti
* Java JDK 8+
* Libreria Gson (inclusa in `lib/`)

## Come Eseguire

### Compilazione
```bash
javac -d bin -cp ".:lib/*" server/*.java client/*.java
### Eseguire
java -cp ".:lib/*:bin" server.ServerMain
java -cp ".:lib/*bin" client.ClientMain

