package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class MalDeAmores extends CartaEfecto {

    public MalDeAmores() {
        super("MAL DE AMORES", "lwjgl3/assets/efectos/MALDEAMORES.png");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.restarVidaPropia(15);
        contexto.restarVidaEnemiga(18);
        System.out.println("[EFECTO] Mal de Amores activado -> Jugador pierde 15 de vida, enemigo pierde 18 de vida.");
    }
}
