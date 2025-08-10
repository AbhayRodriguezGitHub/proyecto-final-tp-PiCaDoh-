package mijuego.picadoh.cartas;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Gdx;

public abstract class CartaTropa {
    protected String nombre;
    protected int atk;
    protected int def;
    protected Texture imagen;
    protected boolean puedeAtacarDosVeces = false;

    // NUEVO ATRIBUTO:
    private int usosRestantes;

    public CartaTropa(String nombre, int atk, int def, String rutaImagen) {
        this.nombre = nombre;
        this.atk = atk;
        this.def = def;
        this.imagen = new Texture(Gdx.files.absolute(rutaImagen));
        this.usosRestantes = 2; // por defecto 2 usos para invocar en modo desarrollo
    }

    public String getNombre() {
        return nombre;
    }

    public int getAtk() {
        return atk;
    }

    public int getDef() {
        return def;
    }

    public Texture getImagen() {
        return imagen;
    }

    public boolean puedeAtacarDosVeces() {
        return puedeAtacarDosVeces;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public void setPuedeAtacarDosVeces(boolean puedeAtacarDosVeces) {
        this.puedeAtacarDosVeces = puedeAtacarDosVeces;
    }

    // NUEVOS MÉTODOS PARA MANEJAR USOS:
    public int getUsosRestantes() {
        return usosRestantes;
    }

    /**
     * Disminuye en 1 los usos restantes y devuelve true si aún quedan usos,
     * false si se consumieron todos (la carta debe desaparecer de la mano).
     */
    public boolean invocar() {
        if (usosRestantes > 0) {
            usosRestantes--;
        }
        return usosRestantes > 0;
    }

    public void resetUsos() {
        this.usosRestantes = 2;
    }

    public void dispose() {
        if (imagen != null) imagen.dispose();
    }
}
