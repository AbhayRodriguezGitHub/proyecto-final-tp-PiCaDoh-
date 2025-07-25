package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class MagiaBendita extends CartaEfecto {

    public MagiaBendita() {
        super("MAGIA BENDITA", "lwjgl3/assets/efectos/MAGIABENDITA.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        CartaTropa tropa = contexto.getTropaSeleccionada();
        if (tropa != null) {
            tropa.setAtk(tropa.getAtk() * 2);
        }
    }
}
