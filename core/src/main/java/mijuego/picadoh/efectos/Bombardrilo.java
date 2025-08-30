package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class Bombardrilo extends CartaEfecto {

    public Bombardrilo() {
        super("BOMBARDRILO", "lwjgl3/assets/efectos/BOMBARDRILO.png");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.setLimpiarCampoSolicitado(true);
        System.out.println("[EFECTO] Bombardrilo activado -> destruir TODAS las tropas del campo.");
    }
}
