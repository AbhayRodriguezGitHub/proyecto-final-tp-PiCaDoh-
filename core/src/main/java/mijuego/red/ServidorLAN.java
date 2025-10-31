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
 * Servidor TCP sincronizado para partidas 1v1 con descubrimiento en LAN.
 *
 * Protocolo TCP:
 *  - JOIN_QUEUE / TROOP_READY / EFFECT_READY / INVOKE / INVOKE_EFFECT / PLAY
 *  - REVEAL replica invocaciones y vidas
 *
 * Extras LAN (modo colegio):
 *  - Muestra IPs locales disponibles
 *  - Responde discovery UDP (broadcast y unicast) y multicast opcional
 *  - Crea server_ip.txt con la IP a la que deben conectarse los clientes
 *  - Best-effort: crea reglas de firewall en Windows para TCP:5000 y UDP:5001/5002
 *  - UTF-8, TCP_NODELAY, keepAlive
 *  - Puertos e interfaz configurables (-Dserver.port / -Dserver.bind) o env SERVER_PORT / SERVER_BIND
 */
public class ServidorLAN {

    // ========= Configuración =========
    private static int resolvePort(int defaultPort) {
        String p = System.getProperty("server.port");
        if (p != null && !p.isBlank()) {
            try { return Integer.parseInt(p.trim()); } catch (NumberFormatException ignored) {}
        }
        p = System.getenv("SERVER_PORT");
        if (p != null && !p.isBlank()) {
            try { return Integer.parseInt(p.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultPort;
    }

    private static String resolveBind(String defaultBind) {
        String b = System.getProperty("server.bind");
        if (b != null && !b.isBlank()) return b.trim();
        b = System.getenv("SERVER_BIND");
        if (b != null && !b.isBlank()) return b.trim();
        return defaultBind;
    }

    private static final int DEFAULT_TCP_PORT = 5000;

    // Descubrimiento (debe coincidir con el cliente)
    private static final int    DISCOVERY_PORT  = 5001;
    private static final String DISCOVER_MAGIC  = "PICADOH_DISCOVER";
    private static final String DISCOVER_REPLY  = "DISCOVER_REPLY";

    // Multicast opcional (algunas WiFi bloquean broadcast pero permiten multicast)
    private static final String MC_ADDR = "239.255.255.250";
    private static final int    MC_PORT = 5002;

    private final ServerSocket server;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "srv-worker-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });
    private final Gson gson = new Gson();
    private volatile boolean running = true;

    private final Queue<ClientHandler> waiting = new ConcurrentLinkedQueue<>();
    private final Map<ClientHandler, Match> matches = new ConcurrentHashMap<>();

    // =================== MATCH ===================
    private static class Match {
        final ClientHandler a;
        final ClientHandler b;
        int vidaA = 80;
        int vidaB = 80;
        boolean partidaIniciada = false;

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
    public ServidorLAN(int port, String bindAddr) throws IOException {
        // Si no especifican bind, detecto la primera IPv4 útil para fijarla
        if (bindAddr == null || bindAddr.isBlank() || "0.0.0.0".equals(bindAddr)) {
            String detected = getFirstIPv4();
            if (!"127.0.0.1".equals(detected)) {
                bindAddr = detected; // mejora UX en redes escolares
            } else {
                bindAddr = "0.0.0.0";
            }
        }

        // Bind explícito a la interfaz indicada (0.0.0.0 = todas)
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(bindAddr, port), 100);
        this.server = ss;

        System.out.println("[ServidorLAN] Servidor TCP escuchando en " + bindAddr + ":" + port);
        listarIPsLocales(port);
        escribirServerIpFile(bindAddr, port);       // <-- crea server_ip.txt
        ensureWindowsFirewall(bindAddr, port);      // <-- intenta abrir firewall si es Windows
        mostrarVentanaServidor();

        // Threads de discovery
        pool.submit(this::runDiscoveryResponderUDP);
        pool.submit(this::runDiscoveryResponderMulticast);
    }

    /** Crea/actualiza server_ip.txt con la IP que deben usar los clientes. */
    private void escribirServerIpFile(String bindAddr, int port) {
        // Si el bind fue 0.0.0.0, intento grabar la primera IPv4 util
        String ipToWrite = bindAddr;
        if ("0.0.0.0".equals(bindAddr)) {
            ipToWrite = getFirstIPv4();
        }
        File f = new File("server_ip.txt");
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(ipToWrite);
            System.out.println("[ServidorLAN] server_ip.txt generado con: " + ipToWrite);
        } catch (IOException e) {
            System.out.println("[ServidorLAN] No pude escribir server_ip.txt: " + e.getMessage());
        }
    }

    /** Best-effort para crear reglas de firewall en Windows. No detiene el server si falla. */
    private void ensureWindowsFirewall(String bindAddr, int port) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            System.out.println("[Firewall] Sistema no Windows. Saltando configuración de firewall.");
            return;
        }
        try {
            // Regla TCP (puerto del servidor)
            runNetsh(new String[]{
                "netsh","advfirewall","firewall","add","rule",
                "name=Picadoh-TCP-"+port,"dir=in","action=allow","protocol=TCP","localport="+port
            });

            // Regla UDP discovery
            runNetsh(new String[]{
                "netsh","advfirewall","firewall","add","rule",
                "name=Picadoh-UDP-"+DISCOVERY_PORT,"dir=in","action=allow","protocol=UDP","localport="+DISCOVERY_PORT
            });

            // Regla UDP multicast (5002)
            runNetsh(new String[]{
                "netsh","advfirewall","firewall","add","rule",
                "name=Picadoh-UDP-"+MC_PORT,"dir=in","action=allow","protocol=UDP","localport="+MC_PORT
            });

            System.out.println("[Firewall] Reglas solicitadas a Windows. Si no sos admin, puede que no surtan efecto.");
        } catch (Exception e) {
            System.out.println("[Firewall] No pude crear reglas (ok): " + e.getMessage());
        }
    }

    private void runNetsh(String[] cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Log mínimo
            }
        }
        p.waitFor();
    }

    /** Responder discovery por UDP (broadcast y unicast directo al server) */
    private void runDiscoveryResponderUDP() {
        System.out.println("[Discovery/UDP] Responder en puerto " + DISCOVERY_PORT);
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(null);
            ds.setReuseAddress(true);
            ds.bind(new InetSocketAddress(DISCOVERY_PORT));
            byte[] buf = new byte[512];

            while (running) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                ds.receive(pkt);

                String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8).trim();
                if (!DISCOVER_MAGIC.equals(msg)) continue;

                InetAddress requester = pkt.getAddress();
                InetAddress localIP = pickLocalAddressFor(requester);
                if (localIP == null) localIP = InetAddress.getByName(getFirstIPv4());

                JsonObject reply = new JsonObject();
                reply.addProperty("type", DISCOVER_REPLY);
                reply.addProperty("host", localIP.getHostAddress());
                reply.addProperty("port", server.getLocalPort());

                byte[] out = reply.toString().getBytes(StandardCharsets.UTF_8);
                DatagramPacket resp = new DatagramPacket(out, out.length, requester, pkt.getPort());
                ds.send(resp);

                System.out.printf("[Discovery/UDP] → %s  host=%s port=%d%n",
                    requester.getHostAddress(), localIP.getHostAddress(), server.getLocalPort());
            }
        } catch (SocketException se) {
            System.out.println("[Discovery/UDP] Socket cerrado: " + se.getMessage());
        } catch (IOException ioe) {
            if (running) System.out.println("[Discovery/UDP] Error: " + ioe.getMessage());
        } finally {
            if (ds != null) ds.close();
        }
    }

    /** Responder discovery por Multicast (opcional) */
    private void runDiscoveryResponderMulticast() {
        System.out.println("[Discovery/MC] Intentando unir a " + MC_ADDR + ":" + MC_PORT + " (opcional)...");
        MulticastSocket ms = null;
        try {
            ms = new MulticastSocket(MC_PORT);
            ms.setReuseAddress(true);
            InetAddress grp = InetAddress.getByName(MC_ADDR);
            ms.joinGroup(grp);

            byte[] buf = new byte[512];
            while (running) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                ms.receive(pkt);

                String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8).trim();
                if (!DISCOVER_MAGIC.equals(msg)) continue;

                InetAddress requester = pkt.getAddress();
                InetAddress localIP = pickLocalAddressFor(requester);
                if (localIP == null) localIP = InetAddress.getByName(getFirstIPv4());

                JsonObject reply = new JsonObject();
                reply.addProperty("type", DISCOVER_REPLY);
                reply.addProperty("host", localIP.getHostAddress());
                reply.addProperty("port", server.getLocalPort());

                byte[] out = reply.toString().getBytes(StandardCharsets.UTF_8);
                DatagramPacket resp = new DatagramPacket(out, out.length, requester, pkt.getPort());
                ms.send(resp);

                System.out.printf("[Discovery/MC] → %s  host=%s port=%d%n",
                    requester.getHostAddress(), localIP.getHostAddress(), server.getLocalPort());
            }
        } catch (IOException ioe) {
            // Multicast puede no estar habilitado; lo dejamos informativo
            System.out.println("[Discovery/MC] No disponible (" + ioe.getMessage() + "). Continuamos sin multicast.");
        } finally {
            if (ms != null) {
                try { ms.leaveGroup(InetAddress.getByName(MC_ADDR)); } catch (Exception ignored) {}
                ms.close();
            }
        }
    }

    /** Elige la IP local adecuada para hablar con una IP remota (evita 127.0.0.1) */
    private InetAddress pickLocalAddressFor(InetAddress remote) {
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(remote, 9); // puerto dummy
            InetAddress la = probe.getLocalAddress();
            if (la instanceof Inet4Address && !la.isLoopbackAddress()) return la;
            // fallback: primera IPv4 util
            String first = getFirstIPv4();
            return InetAddress.getByName(first);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Primer IPv4 no-loopback disponible (fallback) */
    private String getFirstIPv4() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
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
                try {
                    s.setTcpNoDelay(true);
                    s.setKeepAlive(true);
                } catch (SocketException ignored) {}

                System.out.println("[ServidorLAN] Cliente entrante: " + s.getRemoteSocketAddress());
                ClientHandler ch = new ClientHandler(s);
                pool.submit(ch);
            }
        } catch (IOException e) {
            if (running) System.err.println("[ServidorLAN] Error accept: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        running = false;
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
                                    if (soyA) { match.vidaA_prop = vidaP; match.vidaA_enemy = vidaE; }
                                    else      { match.vidaB_prop = vidaP; match.vidaB_enemy = vidaE; }
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

        // Usar valores del cliente A como referencia global (o del B si A no reportó)
        if (m.vidaA_prop != null && m.vidaA_enemy != null) {
            m.vidaA = m.vidaA_prop;
            m.vidaB = m.vidaA_enemy;
        } else if (m.vidaB_prop != null && m.vidaB_enemy != null) {
            m.vidaA = m.vidaB_enemy;
            m.vidaB = m.vidaB_prop;
        }

        JsonObject revealA = buildReveal(A, B, m.vidaA, m.vidaB);
        JsonObject revealB = buildReveal(B, A, m.vidaB, m.vidaA);

        A.send(gson.toJson(revealA));
        B.send(gson.toJson(revealB));

        System.out.println("[ServidorLAN] REVEAL enviado. Vidas => A:" + m.vidaA + " / B:" + m.vidaB);

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
        int port = resolvePort(DEFAULT_TCP_PORT);
        String bind = resolveBind("0.0.0.0");
        ServidorLAN s = new ServidorLAN(port, bind);
        s.start();
    }
}
