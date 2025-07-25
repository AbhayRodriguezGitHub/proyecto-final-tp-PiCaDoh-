package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class Acelereitor extends CartaEfecto {

    public Acelereitor() {
        super("ACELEREITOR", "lwjgl3/assets/efectos/ACELEREITOR.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        CartaTropa tropa = contexto.getTropaSeleccionada();
        if (tropa != null) {
            tropa.setPuedeAtacarDosVeces(true);
        }
    }
}
