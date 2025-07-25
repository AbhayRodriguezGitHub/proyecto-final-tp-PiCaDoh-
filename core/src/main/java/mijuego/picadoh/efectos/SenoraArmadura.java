package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class SenoraArmadura extends CartaEfecto {

    public SenoraArmadura() {
        super("SEÑORA ARMADURA", "lwjgl3/assets/efectos/SEÑORAARMADURA.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        CartaTropa tropa = contexto.getTropaSeleccionada();
        if (tropa != null) {
            tropa.setAtk(tropa.getAtk() + 5);
            tropa.setDef(tropa.getDef() + 5);
        }
    }
}
