package mijuego.red;

import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mijuego.picadoh.Principal;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Cliente TCP con descubrimiento automático del servidor en la LAN.
 * Flujo:
 *  1) Si hay host por parámetro/propiedad/archivo -> conectar directo.
 *  2) Discovery UDP (broadcast).
 *  3) Fallback: barrido TCP de la(s) subred(es) local(es) para hallar el puerto 5000 abierto.
 *  4) Si conecta, cachea IP en server_ip.txt para reconexión rápida.
 */
public class ClienteLAN {

    private static final int DEFAULT_TCP_PORT = 5000;
    private static final int DISCOVERY_PORT   = 5001; // Debe coincidir con el servidor
    private static final String DISCOVER_MAGIC = "PICADOH_DISCOVER";
    private static final String DISCOVER_REPLY = "DISCOVER_REPLY";

    private String host; // puede quedar null → discovery/barrido
    private int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final Gson gson = new Gson();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private Consumer<JsonObject> onMessage;

    private volatile boolean running = false;
    private final Principal juego;

    // ====== Constructores ======
    public ClienteLAN(String host, int port) {
        this(null, host, port);
    }

    public ClienteLAN(Principal juego, String host, int port) {
        this.juego = juego;

        // Orden de preferencia para host inicial: arg -> system prop -> env -> archivo cache -> null
        String cliHost = (host != null && !host.isBlank()) ? host : null;
        String sysHost = System.getProperty("server.host");
        String envHost = System.getenv("SERVER_HOST");

        this.host = firstNonBlank(cliHost, sysHost, envHost, readCachedHost());
        this.port = (port > 0) ? port : DEFAULT_TCP_PORT;

        // Permitir override de puerto por -Dserver.port o SERVER_PORT (opcional)
        String sysPort = System.getProperty("server.port");
        if (sysPort != null && !sysPort.isBlank()) {
            try { this.port = Integer.parseInt(sysPort.trim()); } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try { this.port = Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
        }
    }

    private static String firstNonBlank(String... xs) {
        for (String s : xs) if (s != null && !s.isBlank()) return s.trim();
        return null;
    }

    // ====== Estado de conexión ======
    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ====== Conexión (auto discovery + fallback scan) ======
    public boolean connect() {
        try {
            if (isConnected()) {
                System.out.println("[ClienteLAN] Ya conectado. Reusando conexión.");
                return true;
            }

            // 1) Si tengo host, intentar directo
            if (host != null && !host.isBlank()) {
                if (tryConnect(host, port, 2000)) {
                    cacheHost(host);
                    return true;
                } else {
                    System.err.println("[ClienteLAN] Falló conectar a host cacheado/especificado: " + host + ":" + port);
                    host = null; // forzar discovery
                }
            }

            // 2) Discovery UDP
            System.out.println("[ClienteLAN] Sin host válido. Buscando servidor por UDP broadcast...");
            ServerHint hint = discoverServer(1800, 3);
            if (hint != null) {
                host = hint.host;
                port = hint.port > 0 ? hint.port : port;
                if (tryConnect(host, port, 2000)) {
                    cacheHost(host);
                    return true;
                } else {
                    System.err.println("[ClienteLAN] El servidor respondió discovery pero el TCP falló: " + host + ":" + port);
                }
            } else {
                System.out.println("[ClienteLAN] Discovery UDP sin respuestas. Activando fallback: barrido TCP en la(s) subred(es)...");
            }

            // 3) Fallback: barrido TCP de subred local (rápido, /24 típicamente)
            String found = scanLocalSubnetsForOpenServer(port, 120); // timeout por host en ms
            if (found != null) {
                host = found;
                if (tryConnect(host, port, 2000)) {
                    cacheHost(host);
                    return true;
                }
            }

            System.err.println("[ClienteLAN] No se pudo localizar servidor en la LAN.");
            return false;

        } catch (Exception e) {
            System.err.println("[ClienteLAN] Error general en connect(): " + e.getMessage());
            return false;
        }
    }

    private boolean tryConnect(String h, int p, int timeoutMs) {
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(h, p), timeoutMs);

            out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
            ), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            running = true;
            pool.submit(this::readerLoop);

            System.out.println("[ClienteLAN] Conectado a " + h + ":" + p);
            return true;
        } catch (IOException e) {
            // Liberar si falló
            safeClose(socket);
            socket = null;
            out = null;
            in = null;
            return false;
        }
    }

    private void safeClose(Closeable c) {
        if (c == null) return;
        try { c.close(); } catch (IOException ignored) {}
    }

    // ====== Descubrimiento UDP ======
    private static class ServerHint {
        final String host; final int port;
        ServerHint(String h, int p) { host = h; port = p; }
    }

    private ServerHint discoverServer(int timeoutMillis, int tries) {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setBroadcast(true);
            ds.setSoTimeout(timeoutMillis);

            byte[] payload = DISCOVER_MAGIC.getBytes(StandardCharsets.UTF_8);

            for (int attempt = 1; attempt <= tries; attempt++) {
                System.out.println("[ClienteLAN] Discovery intento " + attempt + "...");

                // 1) Broadcast global
                try {
                    DatagramPacket probe255 = new DatagramPacket(payload, payload.length,
                        InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                    ds.send(probe255);
                } catch (Exception ignored) {}

                // 2) Broadcast por interfaz (mejor en routers que bloquean 255.255.255.255)
                try {
                    for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                        if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                            InetAddress b = ia.getBroadcast();
                            if (b == null || !(b instanceof Inet4Address)) continue;
                            DatagramPacket pkt = new DatagramPacket(payload, payload.length, b, DISCOVERY_PORT);
                            ds.send(pkt);
                        }
                    }
                } catch (Exception ignored) {}

                // 3) Esperar una respuesta
                byte[] buf = new byte[512];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
                try {
                    ds.receive(resp);
                } catch (SocketTimeoutException ste) {
                    System.out.println("[ClienteLAN] Sin respuesta. Reintentando...");
                    continue;
                }

                String json = new String(resp.getData(), resp.getOffset(), resp.getLength(), StandardCharsets.UTF_8).trim();
                try {
                    JsonObject o = gson.fromJson(json, JsonObject.class);
                    if (o != null && DISCOVER_REPLY.equals(o.get("type").getAsString())) {
                        String h = o.get("host").getAsString();
                        int p = o.get("port").getAsInt();
                        return new ServerHint(h, p);
                    }
                } catch (Exception ignored) {}
            }

        } catch (IOException e) {
            System.err.println("[ClienteLAN] Error discovery UDP: " + e.getMessage());
        }
        return null;
    }

    // ====== Fallback: barrido TCP de subred ======
    private String scanLocalSubnetsForOpenServer(int tcpPort, int perHostTimeoutMs) {
        try {
            List<String> candidates = computeSubnetHosts();
            System.out.println("[ClienteLAN] Barrido TCP sobre " + candidates.size() + " hosts...");
            for (String ip : candidates) {
                if (tryProbeTcp(ip, tcpPort, perHostTimeoutMs)) {
                    System.out.println("[ClienteLAN] Puerto " + tcpPort + " abierto en " + ip + ". Posible servidor.");
                    return ip;
                }
            }
        } catch (Exception e) {
            System.err.println("[ClienteLAN] Error en barrido TCP: " + e.getMessage());
        }
        return null;
    }

    private boolean tryProbeTcp(String ip, int tcpPort, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, tcpPort), timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Calcula todos los hosts IPv4 de las subredes locales (solo /24 o más grandes),
     * excluyendo loopback y direcciones sin broadcast. Devuelve lista de IPs tipo "192.168.0.1"...".254".
     */
    private List<String> computeSubnetHosts() throws SocketException {
        List<String> out = new ArrayList<>();
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface ni = nics.nextElement();
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                InetAddress addr = ia.getAddress();
                if (!(addr instanceof Inet4Address)) continue;
                short prefix = ia.getNetworkPrefixLength();
                if (prefix <= 0 || prefix > 30) continue; // evitar rangos raros o enormes
                if (prefix < 24) prefix = 24;             // limitar a /24 para mantener rápido

                int mask = prefixToMask(prefix);
                int ip = bytesToInt(addr.getAddress());
                int net = ip & mask;
                int bcast = net | ~mask;

                // Iterar host range (excluyendo net y broadcast)
                int start = (net + 1);
                int end   = (bcast - 1);
                for (int cur = start; cur <= end; cur++) {
                    String hostIp = intToIPv4(cur);
                    // Evitar probarnos a nosotros mismos
                    if (!hostIp.equals(((Inet4Address) addr).getHostAddress())) {
                        out.add(hostIp);
                    }
                }
            }
        }
        return out;
    }

    private int prefixToMask(int prefix) {
        return (int) (0xFFFFFFFFL << (32 - prefix));
    }

    private int bytesToInt(byte[] b) {
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    private String intToIPv4(int i) {
        return ((i >>> 24) & 0xFF) + "." + ((i >>> 16) & 0xFF) + "." + ((i >>> 8) & 0xFF) + "." + (i & 0xFF);
    }

    // ====== Cache IP ======
    private String readCachedHost() {
        File ipFile = new File("server_ip.txt");
        if (!ipFile.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(ipFile))) {
            String line = br.readLine();
            return (line == null) ? null : line.trim();
        } catch (IOException ignored) { return null; }
    }

    private void cacheHost(String h) {
        try (FileWriter fw = new FileWriter("server_ip.txt")) {
            fw.write(h);
        } catch (IOException ignored) {}
    }

    // ====== Hilo lector asíncrono ======
    private void readerLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                System.out.println("[ClienteLAN-RECV] " + line);
                JsonObject obj = gson.fromJson(line, JsonObject.class);

                handleMessage(obj);
                if (onMessage != null) onMessage.accept(obj);
            }
        } catch (IOException e) {
            if (running) System.err.println("[ClienteLAN] Error en readerLoop: " + e.getMessage());
        } finally {
            System.out.println("[ClienteLAN] Hilo lector finalizado.");
            close();
        }
    }

    // ====== Procesamiento de mensajes estándar ======
    private void handleMessage(JsonObject obj) {
        if (obj == null || !obj.has("type")) return;
        String type = obj.get("type").getAsString();
        switch (type) {
            case "WELCOME":
                System.out.println("[ClienteLAN] Bienvenido al servidor LAN!");
                break;
            case "MATCHED":
                System.out.println("[ClienteLAN] ¡Rival encontrado! Abriendo selección de tropas...");
                if (juego != null && Gdx.app != null) {
                    Gdx.app.postRunnable(() ->
                        juego.setScreen(new mijuego.picadoh.PantallaSeleccionTropa(juego))
                    );
                }
                break;
            case "ERROR":
                String msg = obj.has("msg") ? obj.get("msg").getAsString() : "(sin mensaje)";
                System.err.println("[ClienteLAN] ERROR desde servidor: " + msg);
                break;
            case "OPPONENT_DISCONNECTED":
                System.err.println("[ClienteLAN] Tu oponente se desconectó.");
                break;
            default:
                // START / REVEAL / otros los maneja la pantalla vía onMessage
                break;
        }
    }

    // ====== Envío general ======
    public synchronized void sendJson(JsonObject obj) {
        if (out != null) {
            out.println(gson.toJson(obj));
            out.flush();
        } else {
            System.err.println("[ClienteLAN] No hay conexión activa para enviar JSON.");
        }
    }

    public synchronized void sendPlain(String s) {
        if (out != null) {
            out.println(s);
            out.flush();
        } else {
            System.err.println("[ClienteLAN] No hay conexión activa para enviar texto plano.");
        }
    }

    // ====== Métodos predefinidos ======
    public void joinQueue() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "JOIN_QUEUE");
        sendJson(o);
        System.out.println("[ClienteLAN] Enviado JOIN_QUEUE");
    }

    public void sendTroopReady(List<String> classNames) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "TROOP_READY");
        JsonArray arr = new JsonArray();
        classNames.forEach(arr::add);
        o.add("tropas", arr);
        sendJson(o);
        System.out.println("[ClienteLAN] Enviadas tropas: " + classNames);
    }

    public void sendEffectReady(List<String> classNames) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "EFFECT_READY");
        JsonArray arr = new JsonArray();
        classNames.forEach(arr::add);
        o.add("efectos", arr);
        sendJson(o);
        System.out.println("[ClienteLAN] Enviados efectos: " + classNames);
    }

    public void sendInvoke(int slot, String className) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVOKE");
        o.addProperty("slot", slot);
        o.addProperty("class", className);
        sendJson(o);
        System.out.println("[ClienteLAN] INVOKE slot " + slot + " -> " + className);
    }

    public void sendInvokeEffect(String className) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVOKE_EFFECT");
        o.addProperty("class", className);
        sendJson(o);
        System.out.println("[ClienteLAN] INVOKE_EFFECT -> " + className);
    }

    public void sendPlay(int vidaPropia, int vidaEnemiga) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "PLAY");
        o.addProperty("vidaP", vidaPropia);
        o.addProperty("vidaE", vidaEnemiga);
        sendJson(o);
        System.out.println("[ClienteLAN] PLAY enviado con vidas -> propia=" + vidaPropia + " / enemiga=" + vidaEnemiga);
    }

    public void sendPlay() {
        sendPlay(-1, -1);
    }

    // ====== Listener ======
    public void setOnMessage(Consumer<JsonObject> listener) {
        this.onMessage = listener;
    }

    // ====== Cierre ======
    public void close() {
        running = false;
        safeClose(socket);
        socket = null;
        out = null;
        in = null;
        pool.shutdownNow();
        System.out.println("[ClienteLAN] Conexión cerrada.");
    }
}
