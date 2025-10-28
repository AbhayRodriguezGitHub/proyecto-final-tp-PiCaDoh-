package mijuego.red;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado de batalla determinístico y seguro para turnos por slots (0..4).
 * El servidor mantiene este estado como "fuente de verdad".
 * Los clientes solo muestran lo que el servidor envía.
 */
public class BattleState {

    // ======= Atributos principales =======
    public int vidaA = 80;
    public int vidaB = 80;

    // Tableros de cada jugador (5 slots cada uno)
    private final CardState[] campoA = new CardState[5];
    private final CardState[] campoB = new CardState[5];

    // Últimas invocaciones registradas (para REVEAL)
    private List<CardState> lastTurnInvA = new ArrayList<>();
    private List<CardState> lastTurnInvB = new ArrayList<>();

    // ======= Métodos principales =======

    /**
     * Registra las invocaciones del turno para ambos jugadores.
     * Corrige slots fuera de rango, pisa el contenido anterior,
     * y almacena copias inmutables en lastTurnInvA/B.
     */
    public synchronized void resolverTurno(List<CardState> invA, List<CardState> invB) {
        lastTurnInvA = new ArrayList<>();
        lastTurnInvB = new ArrayList<>();

        // Aplicar invocaciones del jugador A
        if (invA != null) {
            for (CardState c : invA) {
                CardState clean = sanitize(c);
                int slot = clampSlot(clean.slot);
                campoA[slot] = clean.copy();
                lastTurnInvA.add(clean.copy());
            }
        }

        // Aplicar invocaciones del jugador B
        if (invB != null) {
            for (CardState c : invB) {
                CardState clean = sanitize(c);
                int slot = clampSlot(clean.slot);
                campoB[slot] = clean.copy();
                lastTurnInvB.add(clean.copy());
            }
        }
    }

    /** Copia inmutable de lo invocado por A en el último turno */
    public synchronized List<CardState> getLastTurnInvA() {
        return copyList(lastTurnInvA);
    }

    /** Copia inmutable de lo invocado por B en el último turno */
    public synchronized List<CardState> getLastTurnInvB() {
        return copyList(lastTurnInvB);
    }

    /** Exporta el estado completo en formato JSON */
    public synchronized JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("vidaA", vidaA);
        o.addProperty("vidaB", vidaB);
        o.add("tropasA", boardToJson(campoA));
        o.add("tropasB", boardToJson(campoB));
        return o;
    }

    /** Limpia el tablero sin modificar las vidas */
    public synchronized void clearBoard() {
        for (int i = 0; i < 5; i++) {
            campoA[i] = null;
            campoB[i] = null;
        }
        lastTurnInvA = new ArrayList<>();
        lastTurnInvB = new ArrayList<>();
    }

    // ======= Helpers =======

    private static int clampSlot(int slot) {
        return Math.max(0, Math.min(4, slot));
    }

    private static CardState sanitize(CardState c) {
        if (c == null) return new CardState("Desconocido", 0, 0, 0);
        String clase = (c.clase == null) ? "Desconocido" : c.clase.trim();
        int atk = Math.max(0, c.ataque);
        int def = Math.max(0, c.defensa);
        int slot = clampSlot(c.slot);
        return new CardState(clase, atk, def, slot);
    }

    private static List<CardState> copyList(List<CardState> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<CardState> out = new ArrayList<>(list.size());
        for (CardState c : list) out.add(c.copy());
        return Collections.unmodifiableList(out);
    }

    /** Convierte el tablero a JSON, usando JsonNull donde no hay carta */
    private static JsonArray boardToJson(CardState[] board) {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < 5; i++) {
            if (board[i] != null) arr.add(board[i].toJson());
            else arr.add(JsonNull.INSTANCE);
        }
        return arr;
    }

    // ======= Clase interna: CardState =======

    public static class CardState {
        public String clase;
        public int ataque;
        public int defensa;
        public int slot;

        public CardState() {}

        public CardState(String clase, int ataque, int defensa, int slot) {
            this.clase = clase;
            this.ataque = ataque;
            this.defensa = defensa;
            this.slot = slot;
        }

        public CardState copy() {
            return new CardState(clase, ataque, defensa, slot);
        }

        /** Serializa una carta a JSON */
        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("class", clase);
            o.addProperty("atk", ataque);
            o.addProperty("def", defensa);
            o.addProperty("slot", slot);
            return o;
        }

        /** Construye una carta desde JSON */
        public static CardState fromJson(JsonObject o) {
            if (o == null) return new CardState("Desconocido", 0, 0, 0);
            String clase = o.has("class") ? o.get("class").getAsString() : "Desconocido";
            int atk = o.has("atk") ? o.get("atk").getAsInt() : 0;
            int def = o.has("def") ? o.get("def").getAsInt() : 0;
            int slot = o.has("slot") ? o.get("slot").getAsInt() : 0;
            return new CardState(clase, atk, def, slot);
        }
    }
}
