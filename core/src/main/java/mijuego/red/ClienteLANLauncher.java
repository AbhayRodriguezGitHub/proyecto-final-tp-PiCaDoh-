package mijuego.red;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.Scanner;

/**
 * Launcher simple para probar ClienteLAN desde la consola.
 * - Uso: ejecutar esta clase (puedes pasar host y puerto como args: host port)
 * - Permite enviar comandos de texto:
 *    help> muestra ayuda
 *    send TROOP name1,name2,> envía TROOP_READY con esa lista
 *    send EFFECT name1,name2,-> envía EFFECT_READY con esa lista
 *    raw {"type":"PING"}-> envía JSON crudo
 *    quit-> desconecta y sale
 */
public class ClienteLANLauncher {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        ClienteLAN cliente = new ClienteLAN(host, port);
        boolean ok = cliente.connect();
            cliente.joinQueue();
        if (ok) {



        System.err.println("[Launcher] No se pudo conectar al servidor " + host + ":" + port);
            return;
        }

        // Listener: imprime mensajes del servidor
        cliente.setOnMessage(json -> {
            System.out.println("[RECV] " + json.toString());
        });

        System.out.println("[Launcher] Conectado a " + host + ":" + port);
        System.out.println("Escribe 'help' para ver comandos.");

        // Console loop en otro hilo para no bloquear la readerLoop interna del cliente
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            Scanner sc = new Scanner(System.in);
            try {
                while (true) {
                    System.out.print("> ");
                    String line = sc.nextLine();
                    if (line == null) break;
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if ("help".equalsIgnoreCase(line)) {
                        System.out.println("Comandos:");
                        System.out.println("  help");
                        System.out.println("  send TROOP name1,name2,...");
                        System.out.println("  send EFFECT name1,name2,...");
                        System.out.println("  raw <JSON>   -> envía JSON crudo (una línea)");
                        System.out.println("  quit");
                        continue;
                    }

                    if ("quit".equalsIgnoreCase(line) || "exit".equalsIgnoreCase(line)) {
                        break;
                    }

                    if (line.startsWith("send ")) {
                        String rest = line.substring(5).trim();
                        if (rest.toUpperCase().startsWith("TROOP ")) {
                            String payload = rest.substring(6).trim();
                            List<String> lista = parseCsv(payload);
                            cliente.sendTroopReady(lista);
                            System.out.println("[SENT] TROOP_READY " + lista);
                        } else if (rest.toUpperCase().startsWith("EFFECT ")) {
                            String payload = rest.substring(7).trim();
                            List<String> lista = parseCsv(payload);
                            cliente.sendEffectReady(lista);
                            System.out.println("[SENT] EFFECT_READY " + lista);
                        } else {
                            System.out.println("Formato inválido. Usa: send TROOP a,b,c  o  send EFFECT x,y,z");
                        }
                        continue;
                    }

                    if (line.startsWith("raw ")) {
                        String json = line.substring(4).trim();
                        try {
                            // Simplemente enviar la línea (tu ClienteLAN tiene sendPlain/sendJson)
                            cliente.sendPlain(json);
                            System.out.println("[SENT RAW] " + json);
                        } catch (Exception e) {
                            System.err.println("Error al enviar raw: " + e.getMessage());
                        }
                        continue;
                    }

                    System.out.println("Comando desconocido. Escribe 'help'.");
                }
            } finally {
                sc.close();
                cliente.close();
                exec.shutdownNow();
                System.out.println("[Launcher] Salida. Cliente desconectado.");
            }
        });
    }

    private static List<String> parseCsv(String s) {
        if (s == null || s.isBlank()) return Collections.emptyList();
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
