package mijuego.picadoh.efectos;

import mijuego.picadoh.batalla.ContextoBatalla;

public class Avaricioso extends CartaEfecto {

    public Avaricioso() {
        super("AVARICIOSO", "lwjgl3/assets/efectos/AVARICIOSO.png");
    }

    @Override
    public void aplicarEfecto(ContextoBatalla contexto) {
        contexto.setInvocacionesIlimitadasEsteTurno(true);
        contexto.registrarReversionTurno(() -> contexto.setInvocacionesIlimitadasEsteTurno(false));
        System.out.println("[EFECTO] Avaricioso → invocaciones de tropas ILIMITADAS este turno.");
    }

    @Override
    public boolean esInstantaneo() {
        return true;
    }
}
