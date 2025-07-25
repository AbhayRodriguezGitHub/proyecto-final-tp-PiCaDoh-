package mijuego.picadoh.cartas;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Gdx;

public abstract class CartaTropa {
    protected String nombre;
    protected int atk;
    protected int def;
    protected Texture imagen;
    protected boolean puedeAtacarDosVeces = false;

    public CartaTropa(String nombre, int atk, int def, String rutaImagen) {
        this.nombre = nombre;
        this.atk = atk;
        this.def = def;
        this.imagen = new Texture(Gdx.files.absolute(rutaImagen));
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

    public void dispose() {
        if (imagen != null) imagen.dispose();
    }
}
