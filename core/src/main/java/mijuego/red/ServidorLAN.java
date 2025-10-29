package mijuego.red;

import com.google.gson.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Servidor TCP sincronizado para partidas 1v1.
 * Controla las conexiones, emparejamiento y sincroniza invocaciones, efectos y vidas.
 *
 * Protocolo:
 *  - JOIN_QUEUE / TROOP_READY / EFFECT_READY / INVOKE / INVOKE_EFFECT / PLAY
 *  - REVEAL replica invocaciones y vidas
 *
 * ✅ Preparado para LAN (varias PCs):
 *  - Muestra todas las IPs locales del servidor
 *  - UTF-8 en IO, TCP_NODELAY y keepAlive en sockets
 *  - Puerto configurable (-Dserver.port / SERVER_PORT)
 */
public class ServidorLAN {

    // ======== Config ========
    private static int resolvePort(int defaultPort) {
        // Propiedad del sistema: -Dserver.port=5000
        String p = System.getProperty("server.port");
        if (p != null && !p.isBlank()) {
            try { return Integer.parseInt(p.trim()); } catch (NumberFormatException ignored) {}
        }
        // Variable de entorno: SERVER_PORT=5000
        p = System.getenv("SERVER_PORT");
        if (p != null && !p.isBlank()) {
            try { return Integer.parseInt(p.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultPort;
    }

    private static final int DEFAULT_PORT = 5000;

    private final ServerSocket server;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "srv-worker-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });
    private final Gson gson = new Gson();

    private final Queue<ClientHandler> waiting = new ConcurrentLinkedQueue<>();
    private final Map<ClientHandler, Match> matches = new ConcurrentHashMap<>();

    // =================== MATCH ===================
    private static class Match {
        final ClientHandler a;
        final ClientHandler b;
        int vidaA = 80;
        int vidaB = 80;
        boolean partidaIniciada = false;

        // vidas reportadas este turno
        Integer vidaA_prop = null;
        Integer vidaA_enemy = null;
        Integer vidaB_prop = null;
        Integer vidaB_enemy = null;

        Match(ClientHandler a, ClientHandler b) {
            this.a = a;
            this.b = b;
        }

        void clearTurnReports() {
            vidaA_prop = vidaA_enemy = vidaB_prop = vidaB_enemy = null;
        }
    }

    // =================== CONSTRUCTOR ===================
    public ServidorLAN(int port) throws IOException {
        // Bind explícito a todas las interfaces (0.0.0.0)
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress("0.0.0.0", port), 100);
        this.server = ss;

        System.out.println("[ServidorLAN] Servidor iniciado en puerto " + port);
        listarIPsLocales(port);
        mostrarVentanaServidor();
    }

    /** Imprime las IPs locales útiles para que los clientes se conecten */
    private void listarIPsLocales(int port) {
        System.out.println("[ServidorLAN] === IPs locales disponibles (conéctense a una de estas) ===");
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) continue;

                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a.isLoopbackAddress() || !(a instanceof Inet4Address)) continue;
                    System.out.printf("[ServidorLAN]  - %s  (%s:%d)%n",
                        a.getHostAddress(), nic.getDisplayName(), port);
                }
            }
        } catch (SocketException e) {
            System.out.println("[ServidorLAN] No se pudieron listar interfaces: " + e.getMessage());
        }
        System.out.println("[ServidorLAN] ============================================================");
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
                // Ajustes útiles para LAN
                try {
                    s.setTcpNoDelay(true);
                    s.setKeepAlive(true);
                } catch (SocketException ignored) {}
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
        volatile boolean playedThisTurn = false;

        ClientHandler(Socket s) throws IOException {
            this.socket = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                out.println(gson.toJson(Map.of("type", "WELCOME")));
                System.out.println("[ServidorLAN] Cliente conectado desde " + socket.getRemoteSocketAddress());

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.isBlank()) continue;
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

                        case "PLAY": {
                            this.playedThisTurn = true;
                            if (match != null) {
                                int vidaP = safeGetInt(obj, "vidaP", -1);
                                int vidaE = safeGetInt(obj, "vidaE", -1);
                                boolean soyA = (match.a == this);

                                if (vidaP >= 0 && vidaE >= 0) {
                                    if (soyA) {
                                        match.vidaA_prop = vidaP;
                                        match.vidaA_enemy = vidaE;
                                    } else {
                                        match.vidaB_prop = vidaP;
                                        match.vidaB_enemy = vidaE;
                                    }
                                }
                                checkAndSendReveal(match);
                            }
                            break;
                        }

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

            JsonObject msgA = buildStart(this, rival, match.vidaA, match.vidaB);
            JsonObject msgB = buildStart(rival, this, match.vidaB, match.vidaA);

            match.a.send(gson.toJson(msgA));
            match.b.send(gson.toJson(msgB));

            System.out.println("[ServidorLAN] START enviado a ambos jugadores.");
        }

        private JsonObject buildStart(ClientHandler player, ClientHandler enemy, int vidaP, int vidaE) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "START");
            o.add("playerTropas", listToJsonArray(player.tropas));
            o.add("playerEfectos", listToJsonArray(player.efectos));
            o.add("enemyTropas", listToJsonArray(enemy.tropas));
            o.add("enemyEfectos", listToJsonArray(enemy.efectos));
            o.addProperty("vidaP", vidaP);
            o.addProperty("vidaE", vidaE);
            return o;
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
        if (waiting.size() >= 2) {
            ClientHandler j1 = waiting.poll();
            ClientHandler j2 = waiting.poll();
            if (j1 == null || j2 == null) return;
            Match m = new Match(j1, j2);
            matches.put(j1, m);
            matches.put(j2, m);
            j1.match = m;
            j2.match = m;
            j1.send(gson.toJson(Map.of("type", "MATCHED")));
            j2.send(gson.toJson(Map.of("type", "MATCHED")));
            System.out.println("[ServidorLAN] Jugadores emparejados!");
        }
    }

    // =================== REVEAL ===================
    private synchronized void checkAndSendReveal(Match m) {
        ClientHandler A = m.a;
        ClientHandler B = m.b;
        if (!A.playedThisTurn || !B.playedThisTurn) return;

        // Usar valores del cliente A como referencia global
        if (m.vidaA_prop != null && m.vidaA_enemy != null) {
            m.vidaA = m.vidaA_prop;
            m.vidaB = m.vidaA_enemy;
        } else if (m.vidaB_prop != null && m.vidaB_enemy != null) {
            m.vidaA = m.vidaB_enemy;
            m.vidaB = m.vidaB_prop;
        }

        // Construir mensajes REVEAL
        JsonObject revealA = buildReveal(A, B, m.vidaA, m.vidaB);
        JsonObject revealB = buildReveal(B, A, m.vidaB, m.vidaA);

        A.send(gson.toJson(revealA));
        B.send(gson.toJson(revealB));

        System.out.println("[ServidorLAN] REVEAL enviado. Vidas => A:" + m.vidaA + " / B:" + m.vidaB);

        // Reset de estado del turno
        A.pendingInvokes = new JsonArray();
        A.pendingEffectInvokes = new JsonArray();
        A.playedThisTurn = false;

        B.pendingInvokes = new JsonArray();
        B.pendingEffectInvokes = new JsonArray();
        B.playedThisTurn = false;

        m.clearTurnReports();
    }

    private JsonObject buildReveal(ClientHandler player, ClientHandler enemy, int vidaP, int vidaE) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "REVEAL");
        o.add("playerInvokes", player.pendingInvokes.deepCopy());
        o.add("enemyInvokes", enemy.pendingInvokes.deepCopy());
        o.add("playerEffectInvokes", player.pendingEffectInvokes.deepCopy());
        o.add("enemyEffectInvokes", enemy.pendingEffectInvokes.deepCopy());
        o.addProperty("vidaP", vidaP);
        o.addProperty("vidaE", vidaE);
        return o;
    }

    private List<String> jsonArrayToList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr != null) for (JsonElement e : arr) out.add(e.getAsString());
        return out;
    }

    private JsonArray listToJsonArray(List<String> list) {
        JsonArray a = new JsonArray();
        if (list != null) for (String s : list) a.add(s);
        return a;
    }

    private int safeGetInt(JsonObject o, String key, int def) {
        try { return o.has(key) ? o.get(key).getAsInt() : def; } catch (Exception ignored) { return def; }
    }

    // =================== MAIN ===================
    public static void main(String[] args) throws Exception {
        int port = resolvePort(DEFAULT_PORT);
        ServidorLAN s = new ServidorLAN(port);
        s.start();
    }
}
