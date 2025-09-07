package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class Gangsterio extends CartaEfecto {

    public Gangsterio() {
        super("GANGSTERIO", "lwjgl3/assets/efectos/GANGSTERIO.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        for (CartaTropa t : contexto.getTropasPropias()) {
            if (t != null) {
                final int oldAtk = t.getAtk();
                final int oldDef = t.getDef();

                t.setAtk(oldAtk * 2);
                t.setDef(oldDef * 2);
                contexto.registrarReversionTurno(() -> {
                    t.setAtk(oldAtk);
                    t.setDef(oldDef);
                });
            }
        }
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
}
