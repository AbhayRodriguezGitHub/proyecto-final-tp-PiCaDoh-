package mijuego.picadoh.batalla;

import mijuego.picadoh.cartas.CartaTropa;

public class Ranura {
    private final int x, y, ancho, alto;
    private CartaTropa carta;
    private boolean esEnemigo;

    public Ranura(int x, int y, int ancho, int alto, boolean esEnemigo) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.carta = null;
        this.esEnemigo = esEnemigo;
    }

    public boolean contiene(int px, int py) {
        return px >= x && px <= x + ancho && py >= y && py <= y + alto;
    }

    /**
     * Asigna una carta a esta ranura.
     */
    public void setCarta(CartaTropa carta) {
        this.carta = carta;
    }

    public CartaTropa getCarta() {
        return carta;
    }

    /**
     * Invoca la carta de esta ranura.
     * Devuelve true si la carta sigue disponible, false si debe eliminarse (usos agotados).
     */
    public boolean invocar() {
        if (carta == null) return false;
        return carta.invocar();
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }

    public boolean esEnemigo() {
        return esEnemigo;
    }
}
