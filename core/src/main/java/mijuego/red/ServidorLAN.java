package mijuego.red;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor TCP simple con matchmaking 1v1 y soporte de INVOKE / PLAY / REVEAL.
 *
 * Protocolos (JSON por línea):
 * - Cliente -> Servidor:
 *   { "type":"TROOP_READY", "tropas":[ "pkg.ClassName", ... ] }
 *   { "type":"EFFECT_READY", "efectos":[ "pkg.ClassName", ... ] }
 *   { "type":"INVOKE", "slot":<0..4>, "class":"full.ClassName" }
 *   { "type":"PLAY" }
 *
 * - Servidor -> Cliente:
 *   { "type":"WELCOME" }
 *   { "type":"MATCHED" }
 *   { "type":"START", ... }   // como antes
 *   { "type":"REVEAL", "playerInvokes":[{slot,class},...], "enemyInvokes":[...] }
 *   { "type":"OPPONENT_DISCONNECTED" }
 */
public class ServidorLAN {
    private static final int PORT = 5000;
    private final ServerSocket server;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    // Representa una partida entre dos clientes
    private static class Match {
        final ClientHandler a;
        final ClientHandler b;

        Match(ClientHandler a, ClientHandler b) {
            this.a = a;
            this.b = b;
        }
    }

    private final Queue<ClientHandler> waiting = new ConcurrentLinkedQueue<>();

    public ServidorLAN(int port) throws IOException {
        this.server = new ServerSocket(port);
        System.out.println("[Servidor] escuchando en puerto " + port);
    }

    public void start() {
        try {
            while (!server.isClosed()) {
                Socket s = server.accept();
                ClientHandler ch = new ClientHandler(s);
                pool.submit(ch);
            }
        } catch (IOException e) {
            System.err.println("[Servidor] error accept: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        try {
            server.close();
        } catch (IOException ignored) {}
        pool.shutdownNow();
        System.out.println("[Servidor] cerrado.");
    }

    private class ClientHandler implements Runnable {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;

        Match match; // asignado cuando hay un match

        // Para START flow (selección previa)
        List<String> tropas;
        List<String> efectos;

        // Para la fase por-turno INVOKE/PLAY
        JsonArray pendingInvokes = new JsonArray(); // array de {slot:..., class:"..."}
        boolean playedThisTurn = false;

        ClientHandler(Socket s) throws IOException {
            this.socket = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(s.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                // bienvenida
                out.println(gson.toJson(Map.of("type", "WELCOME")));
                enqueueAndMatch(this);

                String line;
                while ((line = in.readLine()) != null) {
                    JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                    String type = obj.has("type") ? obj.get("type").getAsString() : "";

                    System.out.println("[Servidor] msg type=" + type + " from " + socket.getRemoteSocketAddress());

                    switch (type) {
                        case "TROOP_READY":
                            this.tropas = jsonArrayToList(obj.getAsJsonArray("tropas"));
                            tryStartIfReady();
                            break;
                        case "EFFECT_READY":
                            this.efectos = jsonArrayToList(obj.getAsJsonArray("efectos"));
                            tryStartIfReady();
                            break;
                        case "INVOKE":
                            // Añadir invoke al array pendiente (no revelar)
                            JsonObject invokeObj = new JsonObject();
                            if (obj.has("slot")) invokeObj.add("slot", obj.get("slot"));
                            if (obj.has("class")) invokeObj.add("class", obj.get("class"));
                            this.pendingInvokes.add(invokeObj);
                            System.out.println("[Servidor] Stored INVOKE from " + socket.getRemoteSocketAddress() + " -> " + invokeObj);
                            break;
                        case "PLAY":
                            this.playedThisTurn = true;
                            System.out.println("[Servidor] PLAY recibido de " + socket.getRemoteSocketAddress());
                            if (match != null) {
                                checkAndRevealTurn(match);
                            }
                            break;
                        case "PING":
                            out.println(gson.toJson(Map.of("type", "PONG")));
                            break;
                        default:
                            System.out.println("[Servidor] Mensaje no manejado: " + line);
                            break;
                    }
                }
            } catch (IOException ex) {
                System.out.println("[Servidor] desconexión: " + ex.getMessage());
            } finally {
                close();
            }
        }

        private void tryStartIfReady() {
            // Si ya hay match, ver si podemos enviar START
            if (match == null) return;
            if (tropas == null || efectos == null) return;

            ClientHandler rival = (match.a == this) ? match.b : match.a;
            if (rival.tropas == null || rival.efectos == null) return;

            // Construir START para ambos
            JsonObject msgA = new JsonObject();
            msgA.addProperty("type", "START");
            msgA.add("playerTropas", listToJsonArray(this.tropas));
            msgA.add("playerEfectos", listToJsonArray(this.efectos));
            msgA.add("enemyTropas", listToJsonArray(rival.tropas));
            msgA.add("enemyEfectos", listToJsonArray(rival.efectos));
            msgA.addProperty("vidaP", 80);
            msgA.addProperty("vidaE", 80);

            JsonObject msgB = new JsonObject();
            msgB.addProperty("type", "START");
            msgB.add("playerTropas", listToJsonArray(rival.tropas));
            msgB.add("playerEfectos", listToJsonArray(rival.efectos));
            msgB.add("enemyTropas", listToJsonArray(this.tropas));
            msgB.add("enemyEfectos", listToJsonArray(this.efectos));
            msgB.addProperty("vidaP", 80);
            msgB.addProperty("vidaE", 80);

            match.a.send(gson.toJson(msgA));
            match.b.send(gson.toJson(msgB));

            System.out.println("[Servidor] START enviado a la partida entre " +
                match.a.socket.getRemoteSocketAddress() + " y " + match.b.socket.getRemoteSocketAddress());
        }

        private void close() {
            try { socket.close(); } catch (IOException ignored) {}
            if (match != null) {
                ClientHandler other = match.a == this ? match.b : match.a;
                try { other.send(gson.toJson(Map.of("type", "OPPONENT_DISCONNECTED"))); } catch (Exception ignored) {}
            } else {
                waiting.remove(this);
            }
        }

        void send(String msg) {
            out.println(msg);
        }
    } // ClientHandler

    private void enqueueAndMatch(ClientHandler ch) {
        waiting.add(ch);
        if (waiting.size() >= 2) {
            ClientHandler a = waiting.poll();
            ClientHandler b = waiting.poll();
            if (a != null && b != null) {
                Match m = new Match(a, b);
                a.match = m;
                b.match = m;

                // avisar emparejamiento
                a.send(gson.toJson(Map.of("type", "MATCHED")));
                b.send(gson.toJson(Map.of("type", "MATCHED")));
                System.out.println("[Servidor] Match creado entre " +
                    a.socket.getRemoteSocketAddress() + " y " + b.socket.getRemoteSocketAddress());
            } else {
                if (a != null) waiting.add(a);
                if (b != null) waiting.add(b);
            }
        }
    }

    private List<String> jsonArrayToList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement e : arr) out.add(e.getAsString());
        return out;
    }

    private JsonArray listToJsonArray(List<String> list) {
        JsonArray a = new JsonArray();
        if (list != null) for (String s : list) a.add(s);
        return a;
    }

    /**
     * Si ambos jugadores en el match hicieron PLAY, hacemos REVEAL:
     * - construimos payload con playerInvokes (para cada receptor) y enemyInvokes
     * - enviamos REVEAL a ambos
     * - limpiamos pendingInvokes y playedThisTurn para la siguiente ronda
     */
    private void checkAndRevealTurn(Match m) {
        if (m == null) return;
        ClientHandler A = m.a;
        ClientHandler B = m.b;

        if (!A.playedThisTurn || !B.playedThisTurn) return;

        // Ambos jugaron: construir mensajes REVEAL
        JsonObject msgA = new JsonObject();
        msgA.addProperty("type", "REVEAL");
        msgA.add("playerInvokes", A.pendingInvokes.deepCopy());
        msgA.add("enemyInvokes", B.pendingInvokes.deepCopy());

        JsonObject msgB = new JsonObject();
        msgB.addProperty("type", "REVEAL");
        msgB.add("playerInvokes", B.pendingInvokes.deepCopy());
        msgB.add("enemyInvokes", A.pendingInvokes.deepCopy());

        A.send(gson.toJson(msgA));
        B.send(gson.toJson(msgB));

        System.out.println("[Servidor] REVEAL enviado. A invokes=" + A.pendingInvokes + " | B invokes=" + B.pendingInvokes);

        // limpiar para la siguiente ronda (nueva lista vacía)
        A.pendingInvokes = new JsonArray();
        B.pendingInvokes = new JsonArray();
        A.playedThisTurn = false;
        B.playedThisTurn = false;
    }

    public static void main(String[] args) throws Exception {
        ServidorLAN s = new ServidorLAN(PORT);
        s.start();
    }
}
