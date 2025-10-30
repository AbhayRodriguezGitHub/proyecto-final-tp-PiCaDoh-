package mijuego.red;

import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mijuego.picadoh.Principal;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Cliente TCP con descubrimiento automático del servidor en la LAN.
 * Si no se pasa host, hace broadcast UDP y se conecta solo.
 */
public class ClienteLAN {

    private static final int DEFAULT_TCP_PORT = 5000;
    private static final int DISCOVERY_PORT   = 5001; // Debe coincidir con el servidor
    private static final String DISCOVER_MAGIC = "PICADOH_DISCOVER";
    private static final String DISCOVER_REPLY = "DISCOVER_REPLY";

    private String host; // puede quedar null → discovery
    private final int port;

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
        this.host = (host == null || host.isBlank()) ? System.getProperty("server.host", System.getenv("SERVER_HOST")) : host;
        this.port = (port > 0) ? port : DEFAULT_TCP_PORT;
    }

    // ====== Estado de conexión ======
    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ====== Conexión (con autodiscovery si hace falta) ======
    public boolean connect() {
        try {
            if (isConnected()) {
                System.out.println("[ClienteLAN] Ya conectado. Reusando conexión.");
                return true;
            }

            // 1️⃣ Descubrir si no hay host seteado
            if (host == null || host.isBlank()) {
                System.out.println("[ClienteLAN] Sin host especificado. Buscando servidor en la LAN...");

                // Intento 1: archivo cache
                File ipFile = new File("server_ip.txt");
                if (ipFile.exists()) {
                    host = new BufferedReader(new FileReader(ipFile)).readLine().trim();
                    System.out.println("[ClienteLAN] Cargada IP previa del servidor: " + host);
                }

                // Intento 2: broadcast UDP
                if (host == null || host.isBlank()) {
                    ServerHint hint = discoverServer(1800, 3);
                    if (hint != null) {
                        host = hint.host;
                        System.out.println("[ClienteLAN] Servidor hallado en " + host + ":" + hint.port);
                        try (FileWriter fw = new FileWriter(ipFile)) { fw.write(host); } catch (IOException ignored) {}
                    } else {
                        System.err.println("[ClienteLAN] No se encontró servidor en la LAN.");
                        return false;
                    }
                }
            }

            // 2️⃣ Conectar TCP
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, port), 2000);

            out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
            ), true);
            in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );

            running = true;
            pool.submit(this::readerLoop);

            System.out.println("[ClienteLAN] Conectado a " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("[ClienteLAN] Error al conectar: " + e.getMessage());
            return false;
        }
    }

    // ====== Descubrimiento UDP ======
    private static class ServerHint {
        final String host;
        final int port;
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
                DatagramPacket probe255 = new DatagramPacket(payload, payload.length,
                    InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                ds.send(probe255);

                // 2) Broadcast por interfaz
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

                // 3) Esperar respuesta
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
            if (running)
                System.err.println("[ClienteLAN] Error en readerLoop: " + e.getMessage());
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
                break;
        }
    }

    // ====== Envío general ======
    public synchronized void sendJson(JsonObject obj) {
        if (out != null) {
            out.println(gson.toJson(obj));
            out.flush();
        } else System.err.println("[ClienteLAN] No hay conexión activa para enviar JSON.");
    }

    public synchronized void sendPlain(String s) {
        if (out != null) {
            out.println(s);
            out.flush();
        } else System.err.println("[ClienteLAN] No hay conexión activa para enviar texto plano.");
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
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        socket = null;
        out = null;
        in = null;
        pool.shutdownNow();
        System.out.println("[ClienteLAN] Conexión cerrada.");
    }
}
