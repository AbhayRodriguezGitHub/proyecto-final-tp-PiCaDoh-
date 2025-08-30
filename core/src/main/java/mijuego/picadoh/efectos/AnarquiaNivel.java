package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class AnarquiaNivel extends CartaEfecto {

    public AnarquiaNivel() {
        super("ANARQUIA DE NIVEL", "lwjgl3/assets/efectos/ANARQUIA.png");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.setInvocacionLibreEsteTurno(true);
        System.out.println("[EFECTO] Anarquía de Nivel activada -> puedes invocar cualquier carta este turno.");
    }
}
