package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class Intercambio extends CartaEfecto {

    public Intercambio() {
        super("INTERCAMBIO", "lwjgl3/assets/efectos/INTERCAMBIO.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        // Intercambia ATK y DEF de todas las tropas y registra la reversión
        for (CartaTropa t : contexto.getTropasPropias()) {
            if (t != null) {
                final int oldAtk = t.getAtk();
                final int oldDef = t.getDef();
                t.setAtk(oldDef);
                t.setDef(oldAtk);
                contexto.registrarReversionTurno(() -> {
                    t.setAtk(oldAtk);
                    t.setDef(oldDef);
                });
            }
        }
        for (CartaTropa t : contexto.getTropasEnemigas()) {
            if (t != null) {
                final int oldAtk = t.getAtk();
                final int oldDef = t.getDef();
                t.setAtk(oldDef);
                t.setDef(oldAtk);
                contexto.registrarReversionTurno(() -> {
                    t.setAtk(oldAtk);
                    t.setDef(oldDef);
                });
            }
        }
        System.out.println("[EFECTO] Intercambio → ATK/DEF invertidos temporalmente en todas las tropas.");
    }

    @Override
    public boolean esInstantaneo() {
        return true; // Apenas se invoca, se ejecuta
    }
}
