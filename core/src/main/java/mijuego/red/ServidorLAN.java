package mijuego.red;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor TCP simple con matchmaking 1v1.
 * Protocolo JSON (líneas \n):
 * - Cliente -> Servidor:
 *   { "type":"TROOP_READY", "tropas":[ "pkg.ClassName", ... ] }
 *   { "type":"EFFECT_READY", "efectos":[ "pkg.ClassName", ... ] }
 * - Servidor -> Cliente:
 *   { "type":"START", "playerTropas":[...], "playerEfectos":[...],
 *     "enemyTropas":[...], "enemyEfectos":[...], "vidaP":80, "vidaE":80 }
 */
public class ServidorLAN {
    private static final int PORT = 5000;
    private final ServerSocket server;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Queue<ClientHandler> waiting = new ConcurrentLinkedQueue<>();
    private final Gson gson = new Gson();

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
        try { server.close(); } catch (IOException ignored) {}
        pool.shutdownNow();
        System.out.println("[Servidor] cerrado.");
    }

    private class Match {
        final ClientHandler a;
        final ClientHandler b;
        Match(ClientHandler a, ClientHandler b) { this.a = a; this.b = b; }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private Match match; // cuando asignado
        private List<String> tropas; // clases seleccionadas
        private List<String> efectos;

        ClientHandler(Socket s) throws IOException {
            this.socket = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(s.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                out.println(gson.toJson(Map.of("type","WELCOME")));
                enqueueAndMatch(this);

                String line;
                while ((line = in.readLine()) != null) {
                    JsonObject obj = gson.fromJson(line, JsonObject.class);
                    String type = obj.get("type").getAsString();
                    System.out.println("[Servidor] msg type=" + type + " from " + socket.getRemoteSocketAddress());

                    if ("TROOP_READY".equals(type)) {
                        tropas = jsonArrayToList(obj.getAsJsonArray("tropas"));
                        tryStartIfReady();
                    } else if ("EFFECT_READY".equals(type)) {
                        efectos = jsonArrayToList(obj.getAsJsonArray("efectos"));
                        tryStartIfReady();
                    } else if ("PING".equals(type)) {
                        out.println(gson.toJson(Map.of("type","PONG")));
                    } else {
                        // ignore / extend
                    }
                }
            } catch (IOException ex) {
                System.out.println("[Servidor] desconexión: " + ex.getMessage());
            } finally {
                close();
            }
        }

        private void tryStartIfReady() {
            if (match == null) return;
            if (tropas == null || efectos == null) return;

            ClientHandler rival = (match.a == this) ? match.b : match.a;
            if (rival.tropas == null || rival.efectos == null) return;

            // Ambos listos: construir START para ambos
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
                // notificar rival (opcional)
                ClientHandler other = match.a == this ? match.b : match.a;
                try { other.send(gson.toJson(Map.of("type","OPPONENT_DISCONNECTED"))); } catch (Exception ignored) {}
            } else {
                waiting.remove(this);
            }
        }

        void send(String msg) {
            out.println(msg);
        }
    }

    private void enqueueAndMatch(ClientHandler ch) {
        waiting.add(ch);
        // si hay al menos 2, hacer match
        if (waiting.size() >= 2) {
            ClientHandler a = waiting.poll();
            ClientHandler b = waiting.poll();
            if (a != null && b != null) {
                Match m = new Match(a, b);
                a.match = m;
                b.match = m;
                // opcional: avisar emparejamiento
                a.send(gson.toJson(Map.of("type","MATCHED")));
                b.send(gson.toJson(Map.of("type","MATCHED")));
                System.out.println("[Servidor] Match creado entre " +
                    a.socket.getRemoteSocketAddress() + " y " + b.socket.getRemoteSocketAddress());
            } else {
                // si algo falló, reenqueue
                if (a != null) waiting.add(a);
                if (b != null) waiting.add(b);
            }
        }
    }

    private List<String> jsonArrayToList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        arr.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    private JsonArray listToJsonArray(List<String> list) {
        JsonArray a = new JsonArray();
        if (list != null) list.forEach(a::add);
        return a;
    }

    public static void main(String[] args) throws Exception {
        ServidorLAN s = new ServidorLAN(PORT);
        s.start();
    }
}
