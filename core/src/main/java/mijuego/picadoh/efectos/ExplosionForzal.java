package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class ExplosionForzal extends CartaEfecto {

    public ExplosionForzal() {
        super("EXPLOSION FORZAL", "lwjgl3/assets/efectos/EXPLOSIONFORZAL.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        CartaTropa tropa = contexto.getTropaSeleccionada();
        if (tropa != null) {
            tropa.setAtk(tropa.getAtk() + 10);
        }
    }
}
