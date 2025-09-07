package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class MagoDel8 extends CartaEfecto {

    public MagoDel8() {
        super("MAGO DEL 8", "lwjgl3/assets/efectos/MAGODEL8.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        for (CartaTropa tropa : contexto.getTropasPropias()) {
            if (tropa != null) {
                tropa.setAtk(8);
                tropa.setDef(8);
            }
        }
        System.out.println("[EFECTO] Mago del 8 → todas tus tropas ahora tienen ATK=8 y DEF=8.");
    }
}
