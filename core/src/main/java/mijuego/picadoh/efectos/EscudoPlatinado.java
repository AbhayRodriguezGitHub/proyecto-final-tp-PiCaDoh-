package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class EscudoPlatinado extends CartaEfecto {

    private static final int BONUS_DEF = 25;

    public EscudoPlatinado() {
        super("ESCUDO PLATINADO", "lwjgl3/assets/efectos/ESCUDOPLATINADO.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        for (CartaTropa t : contexto.getTropasPropias()) {
            if (t != null) {
                final int oldDef = t.getDef();
                t.setDef(oldDef + BONUS_DEF);

                contexto.registrarReversionTurno(() -> t.setDef(oldDef));
            }
        }
        System.out.println("[EFECTO] Escudo Platinado → Todas tus tropas reciben +" + BONUS_DEF + " DEF este turno.");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
}
