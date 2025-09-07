package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class AgenteDeTransito extends CartaEfecto {

    public AgenteDeTransito() {
        super("AGENTE DE TRANSITO", "lwjgl3/assets/efectos/AGENTEDETRANSITO.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.setAtaquesEnemigosAnuladosEsteTurno(true);
        contexto.registrarReversionTurno(() ->
            contexto.setAtaquesEnemigosAnuladosEsteTurno(false)
        );
        System.out.println("[EFECTO] Agente de Tránsito → todos los ataques del enemigo quedan en 0 este turno.");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
}
