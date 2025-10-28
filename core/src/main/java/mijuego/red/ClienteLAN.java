package mijuego.red;

import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mijuego.picadoh.Principal;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Cliente TCP para juego LAN de Pi-Ca-Doh!
 * Gestiona conexión, lectura asíncrona, envío JSON y eventos de red.
 */
public class ClienteLAN {

    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final Gson gson = new Gson();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private Consumer<JsonObject> onMessage; // Listener general

    private volatile boolean running = false;

    // Referencia al juego principal (puede ser null si se usa fuera de LWJGL)
    private final Principal juego;

    // ====== Constructores ======
    public ClienteLAN(String host, int port) {
        this(null, host, port);
    }

    public ClienteLAN(Principal juego, String host, int port) {
        this.juego = juego;
        this.host = host;
        this.port = port;
    }

    // ====== Estado de conexión ======
    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ====== Conexión al servidor ======
    public boolean connect() {
        try {
            if (isConnected()) {
                System.out.println("[ClienteLAN] Ya conectado. Reusando conexión.");
                return true;
            }

            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);

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

    // ====== Hilo lector asíncrono ======
    private void readerLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                System.out.println("[ClienteLAN-RECV] " + line);
                JsonObject obj = gson.fromJson(line, JsonObject.class);

                // Procesamiento interno de tipos básicos
                handleMessage(obj);

                // Listener externo (PantallaBatalla u otra clase)
                if (onMessage != null) onMessage.accept(obj);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[ClienteLAN] Error en readerLoop: " + e.getMessage());
            }
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
                    Gdx.app.postRunnable(() -> {
                        juego.setScreen(new mijuego.picadoh.PantallaSeleccionTropa(juego));
                    });
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
                // Otros tipos (START, UPDATE_STATE, REVEAL, etc.) se manejan externamente
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

    // ====== Métodos de envío predefinidos ======

    /** Unirse a la cola de emparejamiento */
    public void joinQueue() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "JOIN_QUEUE");
        sendJson(o);
        System.out.println("[ClienteLAN] Enviado JOIN_QUEUE");
    }

    /** Enviar tropas elegidas */
    public void sendTroopReady(List<String> classNames) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "TROOP_READY");
        JsonArray arr = new JsonArray();
        classNames.forEach(arr::add);
        o.add("tropas", arr);
        sendJson(o);
        System.out.println("[ClienteLAN] Enviadas tropas: " + classNames);
    }

    /** Enviar efectos elegidos */
    public void sendEffectReady(List<String> classNames) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "EFFECT_READY");
        JsonArray arr = new JsonArray();
        classNames.forEach(arr::add);
        o.add("efectos", arr);
        sendJson(o);
        System.out.println("[ClienteLAN] Enviados efectos: " + classNames);
    }

    /** Avisar invocación de tropa (slot 0..4) */
    public void sendInvoke(int slot, String className) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVOKE");
        o.addProperty("slot", slot);
        o.addProperty("class", className);
        sendJson(o);
        System.out.println("[ClienteLAN] INVOKE slot " + slot + " -> " + className);
    }

    /** Avisar invocación de efecto */
    public void sendInvokeEffect(String className) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVOKE_EFFECT");
        o.addProperty("class", className);
        sendJson(o);
        System.out.println("[ClienteLAN] INVOKE_EFFECT -> " + className);
    }

    /** Avisar que el jugador presionó PLAY */
    public void sendPlay() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "PLAY");
        sendJson(o);
        System.out.println("[ClienteLAN] PLAY enviado.");
    }

    // ====== Listener de mensajes externos ======
    public void setOnMessage(Consumer<JsonObject> listener) {
        this.onMessage = listener;
    }

    // ====== Cierre de conexión ======
    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        socket = null;
        out = null;
        in = null;
        pool.shutdownNow();
        System.out.println("[ClienteLAN] Conexión cerrada.");
    }
}
