package mijuego.picadoh.batalla;

import mijuego.picadoh.cartas.CartaTropa;

import java.util.ArrayList;
import java.util.List;

public class ContextoBatalla {
    private static final int MAX_TROPAS_EN_CAMPO = 5;

    private List<CartaTropa> tropasPropias;
    private List<CartaTropa> tropasEnemigas;

    private int vidaPropia;
    private int vidaEnemiga;

    // ✅ NUEVO: carta seleccionada para aplicar efectos
    private CartaTropa tropaSeleccionada;

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

    // Vida
    public void restarVidaEnemiga(int cantidad) {
        vidaEnemiga -= cantidad;
        if (vidaEnemiga < 0) vidaEnemiga = 0;
    }

    public void restarVidaPropia(int cantidad) {
        vidaPropia -= cantidad;
        if (vidaPropia < 0) vidaPropia = 0;
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

    // ✅ NUEVOS MÉTODOS
    public void setTropaSeleccionada(CartaTropa tropa) {
        this.tropaSeleccionada = tropa;
    }

    public CartaTropa getTropaSeleccionada() {
        return tropaSeleccionada;
    }

    // Debug/logs
    @Override
    public String toString() {
        return "ContextoBatalla{" +
            "vidaPropia=" + vidaPropia +
            ", vidaEnemiga=" + vidaEnemiga +
            ", tropasPropias=" + tropasPropias.size() +
            ", tropasEnemigas=" + tropasEnemigas.size() +
            ", tropaSeleccionada=" + (tropaSeleccionada != null ? tropaSeleccionada.getClass().getSimpleName() : "null") +
            '}';
    }
}
