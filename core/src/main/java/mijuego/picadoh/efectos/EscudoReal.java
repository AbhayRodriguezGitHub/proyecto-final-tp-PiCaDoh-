package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class EscudoReal extends CartaEfecto {

    public EscudoReal() {
        super("ESCUDO REAL", "lwjgl3/assets/efectos/ESCUDOREAL.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        CartaTropa tropa = contexto.getTropaSeleccionada();
        if (tropa != null) {
            tropa.setDef(tropa.getDef() + 10);
        }
    }
}
