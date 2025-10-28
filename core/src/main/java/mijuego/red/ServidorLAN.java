package mijuego.red;

import com.google.gson.*;

import javax.swing.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Font;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Servidor TCP sincronizado para partidas 1v1.
 * Envía MATCHED, START y REVEAL (con invocaciones y efectos del turno).
 */
public class ServidorLAN {

    private static final int PORT = 5000;
    private final ServerSocket server;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    private final Queue<ClientHandler> waiting = new ConcurrentLinkedQueue<>();
    private final Map<ClientHandler, Match> matches = new ConcurrentHashMap<>();

    // =================== MATCH ===================
    private static class Match {
        final ClientHandler a;
        final ClientHandler b;
        final BattleState state = new BattleState();
        boolean partidaIniciada = false;

        Match(ClientHandler a, ClientHandler b) {
            this.a = a;
            this.b = b;
        }
    }

    // =================== CONSTRUCTOR ===================
    public ServidorLAN(int port) throws IOException {
        this.server = new ServerSocket(port);
        System.out.println("[ServidorLAN] Servidor iniciado en puerto " + port);
        mostrarVentanaServidor();
    }

    private void mostrarVentanaServidor() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SERVIDOR LAN");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            String ruta = "lwjgl3/assets/lan/SERVIDOR.png";
            File file = new File(ruta);
            JLabel label;
            if (file.exists()) {
                label = new JLabel(new ImageIcon(file.getAbsolutePath()));
            } else {
                label = new JLabel("SERVIDOR LAN ACTIVO", SwingConstants.CENTER);
                label.setFont(new Font("Arial", Font.BOLD, 18));
                label.setForeground(Color.WHITE);
                label.setBackground(Color.DARK_GRAY);
                label.setOpaque(true);
            }
            frame.add(label);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // =================== START SERVER ===================
    public void start() {
        try {
            while (!server.isClosed()) {
                Socket s = server.accept();
                ClientHandler ch = new ClientHandler(s);
                pool.submit(ch);
            }
        } catch (IOException e) {
            System.err.println("[ServidorLAN] Error accept: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        try { server.close(); } catch (IOException ignored) {}
        pool.shutdownNow();
        System.out.println("[ServidorLAN] Cerrado.");
    }

    // =================== CLIENT HANDLER ===================
    private class ClientHandler implements Runnable {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;

        Match match;
        List<String> tropas;
        List<String> efectos;

        JsonArray pendingInvokes = new JsonArray();
        JsonArray pendingEffectInvokes = new JsonArray();
        boolean playedThisTurn = false;

        ClientHandler(Socket s) throws IOException {
            this.socket = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(s.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                out.println(gson.toJson(Map.of("type", "WELCOME")));
                System.out.println("[ServidorLAN] Cliente conectado desde " + socket.getRemoteSocketAddress());

                String line;
                while ((line = in.readLine()) != null) {
                    JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                    String type = obj.has("type") ? obj.get("type").getAsString() : "";

                    switch (type) {
                        case "JOIN_QUEUE":
                            enqueueAndMatch(this);
                            break;

                        case "TROOP_READY":
                            this.tropas = jsonArrayToList(obj.getAsJsonArray("tropas"));
                            tryStartIfReady();
                            break;

                        case "EFFECT_READY":
                            this.efectos = jsonArrayToList(obj.getAsJsonArray("efectos"));
                            tryStartIfReady();
                            break;

                        case "INVOKE":
                            JsonObject inv = new JsonObject();
                            if (obj.has("slot")) inv.add("slot", obj.get("slot"));
                            if (obj.has("class")) inv.add("class", obj.get("class"));
                            pendingInvokes.add(inv);
                            break;

                        case "INVOKE_EFFECT":
                            JsonObject eff = new JsonObject();
                            if (obj.has("class")) eff.add("class", obj.get("class"));
                            pendingEffectInvokes.add(eff);
                            break;

                        case "PLAY":
                            this.playedThisTurn = true;
                            if (match != null) checkAndSendReveal(match);
                            break;

                        default:
                            System.out.println("[ServidorLAN] Mensaje desconocido: " + type);
                    }
                }
            } catch (IOException ex) {
                System.out.println("[ServidorLAN] desconexión: " + ex.getMessage());
            } finally {
                close();
            }
        }

        private void tryStartIfReady() {
            if (match == null || match.partidaIniciada) return;
            if (tropas == null || efectos == null) return;

            ClientHandler rival = (match.a == this) ? match.b : match.a;
            if (rival.tropas == null || rival.efectos == null) return;

            match.partidaIniciada = true;

            JsonObject msgA = new JsonObject();
            msgA.addProperty("type", "START");
            msgA.add("playerTropas", listToJsonArray(this.tropas));
            msgA.add("playerEfectos", listToJsonArray(this.efectos));
            msgA.add("enemyTropas", listToJsonArray(rival.tropas));
            msgA.add("enemyEfectos", listToJsonArray(rival.efectos));
            msgA.addProperty("vidaP", match.state.vidaA);
            msgA.addProperty("vidaE", match.state.vidaB);

            JsonObject msgB = new JsonObject();
            msgB.addProperty("type", "START");
            msgB.add("playerTropas", listToJsonArray(rival.tropas));
            msgB.add("playerEfectos", listToJsonArray(rival.efectos));
            msgB.add("enemyTropas", listToJsonArray(this.tropas));
            msgB.add("enemyEfectos", listToJsonArray(this.efectos));
            msgB.addProperty("vidaP", match.state.vidaB);
            msgB.addProperty("vidaE", match.state.vidaA);

            match.a.send(gson.toJson(msgA));
            match.b.send(gson.toJson(msgB));

            System.out.println("[ServidorLAN] START enviado a ambos jugadores.");
        }

        private void close() {
            try { socket.close(); } catch (IOException ignored) {}
            if (match != null) {
                ClientHandler other = (match.a == this) ? match.b : match.a;
                try { other.send(gson.toJson(Map.of("type", "OPPONENT_DISCONNECTED"))); } catch (Exception ignored) {}
                matches.remove(this);
                matches.remove(other);
            } else {
                waiting.remove(this);
            }
        }

        void send(String msg) { out.println(msg); }
    }

    // =================== MATCHMAKING ===================
    private synchronized void enqueueAndMatch(ClientHandler ch) {
        if (waiting.contains(ch)) return;
        waiting.add(ch);

        System.out.println("[ServidorLAN] Cliente pide unirse a la cola... Total en espera: " + waiting.size());

        if (waiting.size() >= 2) {
            ClientHandler jugador1 = waiting.poll();
            ClientHandler jugador2 = waiting.poll();
            if (jugador1 == null || jugador2 == null) return;

            Match match = new Match(jugador1, jugador2);
            matches.put(jugador1, match);
            matches.put(jugador2, match);
            jugador1.match = match;
            jugador2.match = match;

            jugador1.send(gson.toJson(Map.of("type", "MATCHED")));
            jugador2.send(gson.toJson(Map.of("type", "MATCHED")));

            System.out.println("[ServidorLAN] Jugadores emparejados!");
        }
    }

    // =================== REVEAL ===================
    private void checkAndSendReveal(Match m) {
        ClientHandler A = m.a;
        ClientHandler B = m.b;
        if (!A.playedThisTurn || !B.playedThisTurn) return;

        // Construir y enviar REVEAL para A (player = A, enemy = B)
        JsonObject revealA = new JsonObject();
        revealA.addProperty("type", "REVEAL");
        revealA.add("playerInvokes", A.pendingInvokes.deepCopy());
        revealA.add("enemyInvokes", B.pendingInvokes.deepCopy());
        revealA.add("playerEffectInvokes", A.pendingEffectInvokes.deepCopy());
        revealA.add("enemyEffectInvokes", B.pendingEffectInvokes.deepCopy());
        A.send(gson.toJson(revealA));

        // Construir y enviar REVEAL para B (player = B, enemy = A)
        JsonObject revealB = new JsonObject();
        revealB.addProperty("type", "REVEAL");
        revealB.add("playerInvokes", B.pendingInvokes.deepCopy());
        revealB.add("enemyInvokes", A.pendingInvokes.deepCopy());
        revealB.add("playerEffectInvokes", B.pendingEffectInvokes.deepCopy());
        revealB.add("enemyEffectInvokes", A.pendingEffectInvokes.deepCopy());
        B.send(gson.toJson(revealB));

        System.out.println("[ServidorLAN] REVEAL enviado a ambos jugadores.");

        // Reset turno
        A.pendingInvokes = new JsonArray();
        B.pendingInvokes = new JsonArray();
        A.pendingEffectInvokes = new JsonArray();
        B.pendingEffectInvokes = new JsonArray();
        A.playedThisTurn = false;
        B.playedThisTurn = false;
    }

    // =================== HELPERS ===================
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

    // =================== MAIN ===================
    public static void main(String[] args) throws Exception {
        ServidorLAN s = new ServidorLAN(PORT);
        s.start();
    }
}
