package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class Monarquia extends CartaEfecto {

    public Monarquia() {
        super("MONARQUÍA", "lwjgl3/assets/efectos/MONARQUIA.png");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.solicitarPurgaPorNivel(1, 2, 3);
        System.out.println("[EFECTO] Monarquía activada -> destruir cartas de nivel 1, 2 y 3.");
    }
}
