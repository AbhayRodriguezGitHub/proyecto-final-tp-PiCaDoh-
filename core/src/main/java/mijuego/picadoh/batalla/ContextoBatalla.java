package mijuego.picadoh.batalla;

import mijuego.picadoh.cartas.CartaTropa;

import java.util.ArrayList;
import java.util.List;

public class ContextoBatalla {
    private static final int MAX_TROPAS_EN_CAMPO = 5;
    private static final int VIDA_MAXIMA = 80;

    private List<CartaTropa> tropasPropias;
    private List<CartaTropa> tropasEnemigas;

    private int vidaPropia;
    private int vidaEnemiga;


    private CartaTropa tropaSeleccionada;

    private boolean limpiarCampoSolicitado = false;

    public boolean isLimpiarCampoSolicitado() {
        return limpiarCampoSolicitado;
    }

    public void setLimpiarCampoSolicitado(boolean limpiarCampoSolicitado) {
        this.limpiarCampoSolicitado = limpiarCampoSolicitado;
    }
    private boolean invocacionLibreEsteTurno = false;

    private boolean purgaPorNivelSolicitada = false;
    private final java.util.HashSet<Integer> nivelesAPurgar = new java.util.HashSet<>();

    public void solicitarPurgaPorNivel(int... niveles) {
        nivelesAPurgar.clear();
        for (int n : niveles) nivelesAPurgar.add(n);
        purgaPorNivelSolicitada = true;
    }

    public boolean isPurgaPorNivelSolicitada() {
        return purgaPorNivelSolicitada;
    }

    public java.util.Set<Integer> getNivelesAPurgar() {
        return nivelesAPurgar;
    }

    public void limpiarPurgaPorNivelSolicitud() {
        purgaPorNivelSolicitada = false;
        nivelesAPurgar.clear();
    }

    public ContextoBatalla(List<CartaTropa> propias, List<CartaTropa> enemigas) {
        this(propias, enemigas, VIDA_MAXIMA, VIDA_MAXIMA);
    }


    public ContextoBatalla(List<CartaTropa> propias, List<CartaTropa> enemigas, int vidaP, int vidaE) {
        this.tropasPropias = new ArrayList<>(propias);
        this.tropasEnemigas = new ArrayList<>(enemigas);
        this.vidaPropia = vidaP;
        this.vidaEnemiga = vidaE;
    }

    // Getters
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

    // Tropas
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


    public void setTropaSeleccionada(CartaTropa tropa) {
        this.tropaSeleccionada = tropa;
    }

    public CartaTropa getTropaSeleccionada() {
        return tropaSeleccionada;
    }

    public boolean isInvocacionLibreEsteTurno() {
        return invocacionLibreEsteTurno;
    }

    public void setInvocacionLibreEsteTurno(boolean invocacionLibreEsteTurno) {
        this.invocacionLibreEsteTurno = invocacionLibreEsteTurno;
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
            '}';
    }
}
