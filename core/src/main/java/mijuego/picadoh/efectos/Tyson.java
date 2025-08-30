package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class Tyson extends CartaEfecto {

    public Tyson() {
        super("TYSON", "lwjgl3/assets/efectos/TYSON.png");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.restarVidaEnemiga(5);
        System.out.println("[EFECTO] Tyson inflige 5 de daño directo al enemigo (instantáneo).");
    }
}
