package mijuego.picadoh.batalla;

import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.efectos.CartaEfecto;

import java.util.*;

/**
 * Contexto de batalla que mantiene el estado (tropas propias/enemigas, vida, flags de efectos)
 * y permite aplicar efectos desde la perspectiva del jugador o desde la perspectiva enemiga.
 *
 * IMPORTANT: cuando recibas un REVEAL desde la red, instanciá los efectos (RegistroEfectos.crear)
 * y llamá:
 *   contexto.applyEffectAsPlayer(efecto); // si el effect viene en playerEffectInvokes (local)
 *   contexto.applyEffectAsEnemy(efecto);  // si el effect viene en enemyEffectInvokes (rival)
 *
 * De ese modo no necesitás duplicar la lógica de los efectos: el mismo efecto actúa
 * sobre tropasPropias/tropasEnemigas de forma transparente.
 */
public class ContextoBatalla {
    private static final int MAX_TROPAS_EN_CAMPO = 5;
    private static final int VIDA_MAXIMA = 80;

    // listas internas (siempre guardadas en el orden "propias" y "enemigas" desde la creación)
    private final List<CartaTropa> tropasPropias;
    private final List<CartaTropa> tropasEnemigas;

    // estado de vida (almacenado como valores absolutos; getters pueden invertir)
    private int vidaPropia;
    private int vidaEnemiga;

    private CartaTropa tropaSeleccionada;

    // perspectiva: false = normal (getTropasPropias -> tropasPropias).
    // true = invertida (getTropasPropias -> tropasEnemigas), útil para aplicar efectos del rival.
    private boolean perspectivaInvertida = false;

    // Flags de efectos
    private boolean limpiarCampoSolicitado = false;      // Bombardrilo
    private boolean invocacionLibreEsteTurno = false;    // Anarquía de nivel
    private boolean invocacionesIlimitadasEsteTurno = false; // Avaricioso
    private boolean ataquesEnemigosAnuladosEsteTurno = false; // Agente de Tránsito

    private boolean purgaPorNivelSolicitada = false;     // Monarquía/Rebelión
    private final Set<Integer> nivelesAPurgar = new HashSet<>();

    private boolean intercambioSolicitado = false;

    // reversiones registradas por efectos temporales (se ejecutan en revertirEfectosTurno)
    private final List<Runnable> reversionesTurno = new ArrayList<>();

    // Constructores
    public ContextoBatalla(List<CartaTropa> propias, List<CartaTropa> enemigas) {
        this(propias, enemigas, VIDA_MAXIMA, VIDA_MAXIMA);
    }

    public ContextoBatalla(List<CartaTropa> propias, List<CartaTropa> enemigas, int vidaP, int vidaE) {
        this.tropasPropias = new ArrayList<>(propias);
        this.tropasEnemigas = new ArrayList<>(enemigas);
        this.vidaPropia = Math.max(0, Math.min(vidaP, VIDA_MAXIMA));
        this.vidaEnemiga = Math.max(0, Math.min(vidaE, VIDA_MAXIMA));
    }

    // -----------------
    // Perspectiva utils
    // -----------------
    /**
     * Ejecuta runnable con perspectiva invertida temporalmente (útil para aplicar efectos del rival).
     */
    public void withPerspectiveInverted(Runnable r) {
        boolean old = this.perspectivaInvertida;
        this.perspectivaInvertida = !old;
        try {
            r.run();
        } finally {
            this.perspectivaInvertida = old;
        }
    }

    public void setPerspectiveInvertida(boolean v) {
        this.perspectivaInvertida = v;
    }

    public boolean isPerspectiveInvertida() {
        return this.perspectivaInvertida;
    }

    // -----------------
    // Getters (respetan perspectiva)
    // -----------------
    /**
     * Retorna la lista de tropas que, desde la perspectiva actual, se consideran "propias".
     * Si perspectivaInvertida == true, devuelve la lista interna de 'tropasEnemigas'.
     */
    public List<CartaTropa> getTropasPropias() {
        return perspectivaInvertida ? tropasEnemigas : tropasPropias;
    }

    public List<CartaTropa> getTropasEnemigas() {
        return perspectivaInvertida ? tropasPropias : tropasEnemigas;
    }

    public int getVidaPropia() {
        return perspectivaInvertida ? vidaEnemiga : vidaPropia;
    }

    public int getVidaEnemiga() {
        return perspectivaInvertida ? vidaPropia : vidaEnemiga;
    }

    public int getVidaMaxima() {
        return VIDA_MAXIMA;
    }

    public CartaTropa getTropaSeleccionada() {
        return tropaSeleccionada;
    }

    // -----------------
    // Vida (respetan perspectiva)
    // -----------------
    public void restarVidaEnemiga(int cantidad) {
        if (perspectivaInvertida) {
            // desde perspectiva invertida, "vida enemiga" apunta a vidaPropia interna
            vidaPropia -= cantidad;
            if (vidaPropia < 0) vidaPropia = 0;
        } else {
            vidaEnemiga -= cantidad;
            if (vidaEnemiga < 0) vidaEnemiga = 0;
        }
    }

    public void restarVidaPropia(int cantidad) {
        if (perspectivaInvertida) {
            // desde perspectiva invertida, "vida propia" apunta a vidaEnemiga interna
            vidaEnemiga -= cantidad;
            if (vidaEnemiga < 0) vidaEnemiga = 0;
        } else {
            vidaPropia -= cantidad;
            if (vidaPropia < 0) vidaPropia = 0;
        }
    }

    public void setVidaPropia(int vida) {
        if (perspectivaInvertida) {
            this.vidaEnemiga = Math.min(vida, VIDA_MAXIMA);
            if (this.vidaEnemiga < 0) this.vidaEnemiga = 0;
        } else {
            this.vidaPropia = Math.min(vida, VIDA_MAXIMA);
            if (this.vidaPropia < 0) this.vidaPropia = 0;
        }
    }

    public void setVidaEnemiga(int vida) {
        if (perspectivaInvertida) {
            this.vidaPropia = Math.min(vida, VIDA_MAXIMA);
            if (this.vidaPropia < 0) this.vidaPropia = 0;
        } else {
            this.vidaEnemiga = Math.min(vida, VIDA_MAXIMA);
            if (this.vidaEnemiga < 0) this.vidaEnemiga = 0;
        }
    }

    // -----------------
    // Tropas (respetan perspectiva)
    // -----------------
    public boolean agregarTropaPropia(CartaTropa carta) {
        List<CartaTropa> listas = getTropasPropias();
        if (listas.size() < MAX_TROPAS_EN_CAMPO) {
            listas.add(carta);
            return true;
        }
        return false;
    }

    public boolean agregarTropaEnemiga(CartaTropa carta) {
        List<CartaTropa> listas = getTropasEnemigas();
        if (listas.size() < MAX_TROPAS_EN_CAMPO) {
            listas.add(carta);
            return true;
        }
        return false;
    }

    public boolean estaCampoLlenoPropio() { return getTropasPropias().size() >= MAX_TROPAS_EN_CAMPO; }
    public boolean estaCampoLlenoEnemigo() { return getTropasEnemigas().size() >= MAX_TROPAS_EN_CAMPO; }

    // Selección por objetivo
    public void setTropaSeleccionada(CartaTropa tropa) { this.tropaSeleccionada = tropa; }

    // -----------------
    // Flags de efectos (sin perspectiva — flags aplican a la "vista" actual del jugador)
    // -----------------
    public boolean isInvocacionLibreEsteTurno() { return invocacionLibreEsteTurno; }
    public void setInvocacionLibreEsteTurno(boolean v) { this.invocacionLibreEsteTurno = v; }

    public boolean isInvocacionesIlimitadasEsteTurno() { return invocacionesIlimitadasEsteTurno; }
    public void setInvocacionesIlimitadasEsteTurno(boolean v) { this.invocacionesIlimitadasEsteTurno = v; }

    public boolean isAtaquesEnemigosAnuladosEsteTurno() { return ataquesEnemigosAnuladosEsteTurno; }
    public void setAtaquesEnemigosAnuladosEsteTurno(boolean v) { this.ataquesEnemigosAnuladosEsteTurno = v; }

    public boolean isLimpiarCampoSolicitado() { return limpiarCampoSolicitado; }
    public void setLimpiarCampoSolicitado(boolean v) { this.limpiarCampoSolicitado = v; }

    public void solicitarPurgaPorNivel(int... niveles) {
        nivelesAPurgar.clear();
        for (int n : niveles) nivelesAPurgar.add(n);
        purgaPorNivelSolicitada = true;
    }
    public boolean isPurgaPorNivelSolicitada() { return purgaPorNivelSolicitada; }
    public Set<Integer> getNivelesAPurgar() { return Collections.unmodifiableSet(nivelesAPurgar); }
    public void limpiarPurgaPorNivelSolicitud() {
        purgaPorNivelSolicitada = false;
        nivelesAPurgar.clear();
    }

    public void solicitarIntercambio() { intercambioSolicitado = true; }
    public boolean isIntercambioSolicitado() { return intercambioSolicitado; }
    public void limpiarIntercambioSolicitud() { intercambioSolicitado = false; }

    // Reversiones de efectos temporales
    public void registrarReversionTurno(Runnable reversion) {
        if (reversion != null) reversionesTurno.add(reversion);
    }

    public void revertirEfectosTurno() {
        for (int i = reversionesTurno.size() - 1; i >= 0; i--) {
            try { reversionesTurno.get(i).run(); }
            catch (Exception ex) {
                System.out.println("[ContextoBatalla] Error revirtiendo efecto temporal: " + ex.getMessage());
            }
        }
        reversionesTurno.clear();

        // reset flags
        invocacionLibreEsteTurno = false;
        invocacionesIlimitadasEsteTurno = false;
        ataquesEnemigosAnuladosEsteTurno = false;
        intercambioSolicitado = false;
        limpiarCampoSolicitado = false;
        purgaPorNivelSolicitada = false;
        nivelesAPurgar.clear();
    }

    public void revertirEfectosTemporales() {
        revertirEfectosTurno();
    }

    // -----------------
    // Helpers para aplicar efectos con la perspectiva correcta
    // -----------------
    /**
     * Aplica un efecto como si lo hubiese jugado EL JUGADOR LOCAL (sin invertir la perspectiva).
     * El efecto puede registrar reversiones con contexto.registrarReversionTurno(...)
     */
    public void applyEffectAsPlayer(CartaEfecto efecto) {
        if (efecto == null) return;
        // Ejecutar con la perspectiva actual (por defecto false)
        efecto.aplicarEfecto(this);
    }

    /**
     * Aplica un efecto como si lo hubiese jugado EL RIVAL: invertimos la perspectiva temporalmente
     * para que el mismo efecto actúe sobre las tropas/enemigos correctos desde el punto de vista local.
     */
    public void applyEffectAsEnemy(CartaEfecto efecto) {
        if (efecto == null) return;
        withPerspectiveInverted(() -> {
            // Si el efecto usa getTropasPropias/getTropasEnemigas o setTropaSeleccionada,
            // al invertir la perspectiva actuará sobre las estructuras correctas.
            efecto.aplicarEfecto(this);
        });
    }

    @Override
    public String toString() {
        return "ContextoBatalla{" +
            "vidaPropia=" + (perspectivaInvertida ? vidaEnemiga : vidaPropia) +
            ", vidaEnemiga=" + (perspectivaInvertida ? vidaPropia : vidaEnemiga) +
            ", tropasPropias=" + getTropasPropias().size() +
            ", tropasEnemigas=" + getTropasEnemigas().size() +
            ", tropaSeleccionada=" + (tropaSeleccionada != null ? tropaSeleccionada.getClass().getSimpleName() : "null") +
            ", invocacionLibreEsteTurno=" + invocacionLibreEsteTurno +
            ", invocacionesIlimitadasEsteTurno=" + invocacionesIlimitadasEsteTurno +
            ", ataquesEnemigosAnuladosEsteTurno=" + ataquesEnemigosAnuladosEsteTurno +
            ", limpiarCampoSolicitado=" + limpiarCampoSolicitado +
            ", purgaPorNivelSolicitada=" + purgaPorNivelSolicitada +
            ", nivelesAPurgar=" + nivelesAPurgar +
            ", intercambioSolicitado=" + intercambioSolicitado +
            ", reversionesTurno=" + reversionesTurno.size() +
            ", perspectivaInvertida=" + perspectivaInvertida +
            '}';
    }
}
