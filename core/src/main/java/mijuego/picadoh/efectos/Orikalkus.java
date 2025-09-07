package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class Orikalkus extends CartaEfecto {

    private static final int NUEVO_ATK = 16;
    private static final int NUEVA_DEF = 0;

    public Orikalkus() {
        super("ORIKALKUS", "lwjgl3/assets/efectos/ORIKALKUS.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        for (CartaTropa t : contexto.getTropasPropias()) {
            if (t == null) continue;
            t.setAtk(NUEVO_ATK);
            t.setDef(NUEVA_DEF);
        }

    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
}
