package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class Rebelion extends CartaEfecto {

    public Rebelion() {
        super("REBELIÓN", "lwjgl3/assets/efectos/REBELION.png");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {

        contexto.solicitarPurgaPorNivel(4, 5);
        System.out.println("[EFECTO] Rebelión activada -> destruir cartas de nivel 4 y 5.");
    }
}
