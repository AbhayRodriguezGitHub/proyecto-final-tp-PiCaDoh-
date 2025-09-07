package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class Paracetamol extends CartaEfecto {

    public Paracetamol() {
        super("PARACETAMOL", "lwjgl3/assets/efectos/PARACETAMOL.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        int vidaActual = contexto.getVidaPropia();
        int nuevaVida = vidaActual + 5;
        contexto.setVidaPropia(nuevaVida);
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
}
