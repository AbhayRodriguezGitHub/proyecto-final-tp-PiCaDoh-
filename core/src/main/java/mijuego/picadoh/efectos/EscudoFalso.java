package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.cartas.CartaTropa;

public class EscudoFalso extends CartaEfecto {

    private static final int BONUS_DEF = 5;

    public EscudoFalso() {
        super("ESCUDO FALSO", "lwjgl3/assets/efectos/ESCUDOFALSO.png");
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
        System.out.println("[EFECTO] Escudo Falso → Todas tus tropas reciben +" + BONUS_DEF + " DEF este turno.");
    }

    @Override
    public boolean esInstantaneo() {
        return true; // se aplica inmediatamente
    }
}
