package mijuego.red;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ClienteLAN {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private Consumer<JsonObject> onMessage; // listener

    public ClienteLAN(String host, int port) {
        this.host = host; this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pool.submit(this::readerLoop);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void readerLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                JsonObject obj = gson.fromJson(line, JsonObject.class);
                if (onMessage != null) onMessage.accept(obj);
            }
        } catch (IOException e) {
            System.out.println("[ClienteLAN] reader ended: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void setOnMessage(Consumer<JsonObject> listener) {
        this.onMessage = listener;
    }

    public void sendJson(JsonObject obj) {
        if (out != null) out.println(gson.toJson(obj));
    }

    public void sendPlain(String s) {
        if (out != null) out.println(s);
    }

    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        pool.shutdownNow();
    }

    // helpers para envíos comunes
    public void sendTroopReady(java.util.List<String> classNames) {
        JsonObject o = new JsonObject();
        o.addProperty("type","TROOP_READY");
        JsonArray arr = new JsonArray();
        classNames.forEach(arr::add);
        o.add("tropas", arr);
        sendJson(o);
    }
    public void sendEffectReady(java.util.List<String> classNames) {
        JsonObject o = new JsonObject();
        o.addProperty("type","EFFECT_READY");
        JsonArray arr = new JsonArray();
        classNames.forEach(arr::add);
        o.add("efectos", arr);
        sendJson(o);
    }
}
