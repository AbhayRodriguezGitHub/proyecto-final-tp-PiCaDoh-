package mijuego.picadoh.batalla;

import mijuego.picadoh.cartas.CartaTropa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ContextoBatalla {
    private static final int MAX_TROPAS_EN_CAMPO = 5;
    private static final int VIDA_MAXIMA = 80;

    private final List<CartaTropa> tropasPropias;
    private final List<CartaTropa> tropasEnemigas;

    private int vidaPropia;
    private int vidaEnemiga;

    private CartaTropa tropaSeleccionada;

    private boolean limpiarCampoSolicitado = false;     // Bombardrilo
    private boolean invocacionLibreEsteTurno = false;   // Anarquía de nivel

    private boolean purgaPorNivelSolicitada = false;    // Monarquía/Rebelión
    private final Set<Integer> nivelesAPurgar = new HashSet<>();


    private boolean intercambioSolicitado = false;


    private final List<Runnable> reversionesTurno = new ArrayList<>();

    // ---- Constructores
    public ContextoBatalla(List<CartaTropa> propias, List<CartaTropa> enemigas) {
        this(propias, enemigas, VIDA_MAXIMA, VIDA_MAXIMA);
    }

    public ContextoBatalla(List<CartaTropa> propias, List<CartaTropa> enemigas, int vidaP, int vidaE) {
        this.tropasPropias = new ArrayList<>(propias);
        this.tropasEnemigas = new ArrayList<>(enemigas);
        this.vidaPropia = vidaP;
        this.vidaEnemiga = vidaE;
    }

    // ---- Getters básicos
    public List<CartaTropa> getTropasPropias() {
        return tropasPropias;
    }

    public List<CartaTropa> getTropasEnemigas() {
        return tropasEnemigas;
    }

    public int getVidaPropia() {
        return vidaPropia;
    }

    public int getVidaEnemiga() {
        return vidaEnemiga;
    }

    public int getVidaMaxima() {
        return VIDA_MAXIMA;
    }

    public CartaTropa getTropaSeleccionada() {
        return tropaSeleccionada;
    }

    // ---- Vida
    public void restarVidaEnemiga(int cantidad) {
        vidaEnemiga -= cantidad;
        if (vidaEnemiga < 0) vidaEnemiga = 0;
    }

    public void restarVidaPropia(int cantidad) {
        vidaPropia -= cantidad;
        if (vidaPropia < 0) vidaPropia = 0;
    }

    public void setVidaPropia(int vida) {
        this.vidaPropia = Math.min(vida, VIDA_MAXIMA);
        if (this.vidaPropia < 0) this.vidaPropia = 0;
    }

    public void setVidaEnemiga(int vida) {
        this.vidaEnemiga = Math.min(vida, VIDA_MAXIMA);
        if (this.vidaEnemiga < 0) this.vidaEnemiga = 0;
    }

    // ---- Tropas
    public boolean agregarTropaPropia(CartaTropa carta) {
        if (tropasPropias.size() < MAX_TROPAS_EN_CAMPO) {
            tropasPropias.add(carta);
            return true;
        }
        return false;
    }

    public boolean agregarTropaEnemiga(CartaTropa carta) {
        if (tropasEnemigas.size() < MAX_TROPAS_EN_CAMPO) {
            tropasEnemigas.add(carta);
            return true;
        }
        return false;
    }

    public boolean estaCampoLlenoPropio() {
        return tropasPropias.size() >= MAX_TROPAS_EN_CAMPO;
    }

    public boolean estaCampoLlenoEnemigo() {
        return tropasEnemigas.size() >= MAX_TROPAS_EN_CAMPO;
    }

    // ---- Selección de tropa para efectos por-objetivo
    public void setTropaSeleccionada(CartaTropa tropa) {
        this.tropaSeleccionada = tropa;
    }

    // ---- Invocación libre (Anarquía de Nivel)
    public boolean isInvocacionLibreEsteTurno() {
        return invocacionLibreEsteTurno;
    }

    public void setInvocacionLibreEsteTurno(boolean invocacionLibreEsteTurno) {
        this.invocacionLibreEsteTurno = invocacionLibreEsteTurno;
    }

    // ---- Limpieza de campo (Bombardrilo)
    public boolean isLimpiarCampoSolicitado() {
        return limpiarCampoSolicitado;
    }

    public void setLimpiarCampoSolicitado(boolean limpiarCampoSolicitado) {
        this.limpiarCampoSolicitado = limpiarCampoSolicitado;
    }

    // ---- Purga por nivel (Monarquía/Rebelión)
    public void solicitarPurgaPorNivel(int... niveles) {
        nivelesAPurgar.clear();
        for (int n : niveles) nivelesAPurgar.add(n);
        purgaPorNivelSolicitada = true;
    }

    public boolean isPurgaPorNivelSolicitada() {
        return purgaPorNivelSolicitada;
    }

    public Set<Integer> getNivelesAPurgar() {
        return nivelesAPurgar;
    }

    public void limpiarPurgaPorNivelSolicitud() {
        purgaPorNivelSolicitada = false;
        nivelesAPurgar.clear();
    }

    // ---- Intercambio (swap ATK/DEF global 1 turno) - bandera opcional
    public void solicitarIntercambio() {
        intercambioSolicitado = true;
    }

    public boolean isIntercambioSolicitado() {
        return intercambioSolicitado;
    }

    public void limpiarIntercambioSolicitud() {
        intercambioSolicitado = false;
    }


    public void registrarReversionTurno(Runnable reversion) {
        if (reversion != null) {
            reversionesTurno.add(reversion);
        }
    }


    public void revertirEfectosTurno() {
        // Ejecutar en orden inverso por seguridad (LIFO)
        for (int i = reversionesTurno.size() - 1; i >= 0; i--) {
            try {
                reversionesTurno.get(i).run();
            } catch (Exception ex) {
                System.out.println("[ContextoBatalla] Error revirtiendo efecto temporal: " + ex.getMessage());
            }
        }
        reversionesTurno.clear();


        invocacionLibreEsteTurno = false;
        intercambioSolicitado = false;


        limpiarCampoSolicitado = false;
        purgaPorNivelSolicitada = false;
        nivelesAPurgar.clear();
    }

    @Override
    public String toString() {
        return "ContextoBatalla{" +
            "vidaPropia=" + vidaPropia +
            ", vidaEnemiga=" + vidaEnemiga +
            ", tropasPropias=" + tropasPropias.size() +
            ", tropasEnemigas=" + tropasEnemigas.size() +
            ", tropaSeleccionada=" + (tropaSeleccionada != null ? tropaSeleccionada.getClass().getSimpleName() : "null") +
            ", invocacionLibreEsteTurno=" + invocacionLibreEsteTurno +
            ", limpiarCampoSolicitado=" + limpiarCampoSolicitado +
            ", purgaPorNivelSolicitada=" + purgaPorNivelSolicitada +
            ", nivelesAPurgar=" + nivelesAPurgar +
            ", intercambioSolicitado=" + intercambioSolicitado +
            ", reversionesTurno=" + reversionesTurno.size() +
            '}';
    }
}
