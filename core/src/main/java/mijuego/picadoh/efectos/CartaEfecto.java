package mijuego.picadoh.efectos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Gdx;
import mijuego.picadoh.batalla.ContextoBatalla;

public abstract class CartaEfecto {
    private final String nombre;
    private final Texture imagen;

    // Ahora se pasa la ruta completa como parámetro
    public CartaEfecto(String nombre, String rutaImagen) {
        this.nombre = nombre;
        this.imagen = new Texture(Gdx.files.absolute(rutaImagen));
    }

    public String getNombre() {
        return nombre;
    }

    public Texture getImagen() {
        return imagen;
    }

    public abstract void aplicarEfecto(ContextoBatalla contexto);

    public void dispose() {
        if (imagen != null) {
            imagen.dispose();
        }
    }

    @Override
    public String toString() {
        return nombre;
    }
}
