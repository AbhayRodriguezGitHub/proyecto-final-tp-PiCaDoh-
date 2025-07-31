package mijuego.picadoh.batalla;

import mijuego.picadoh.cartas.CartaTropa;

public class Ranura {
    private final int x, y, ancho, alto;
    private CartaTropa carta;

    public Ranura(int x, int y, int ancho, int alto) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.carta = null;
    }

    public boolean contiene(int px, int py) {
        return px >= x && px <= x + ancho && py >= y && py <= y + alto;
    }

    public void setCarta(CartaTropa carta) {
        this.carta = carta;
    }

    public CartaTropa getCarta() {
        return carta;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
}
