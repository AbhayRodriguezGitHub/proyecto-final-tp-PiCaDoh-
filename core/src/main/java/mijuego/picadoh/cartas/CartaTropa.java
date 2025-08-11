package mijuego.picadoh.cartas;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Gdx;

public abstract class CartaTropa {
    protected String nombre;
    protected int atk;
    protected int def;
    protected Texture imagen;
    protected boolean puedeAtacarDosVeces = false;

    // NUEVO ATRIBUTO: Nivel de la carta (1 a 5)
    protected int nivel;

    // NUEVO ATRIBUTO: Usos restantes para invocar
    private int usosRestantes;

    /**
     * Constructor para inicializar una carta de tropa
     * @param nombre Nombre de la carta
     * @param atk Ataque
     * @param def Defensa
     * @param rutaImagen Ruta absoluta a la imagen
     * @param nivel Nivel de la carta (1 a 5)
     */
    public CartaTropa(String nombre, int atk, int def, String rutaImagen, int nivel) {
        this.nombre = nombre;
        this.atk = atk;
        this.def = def;
        this.imagen = new Texture(Gdx.files.absolute(rutaImagen));
        this.nivel = nivel;
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

    // --- Métodos compatibles con el código existente ---
    public int getAtaque() {
        return getAtk();
    }

    public int getDefensa() {
        return getDef();
    }

    public void setDefensa(int def) {
        setDef(def);
    }

    // --- Nuevo método para obtener el nivel ---
    public int getNivel() {
        return nivel;
    }

    // --- Nuevo método para cambiar el nivel si es necesario ---
    public void setNivel(int nivel) {
        if (nivel >= 1 && nivel <= 5) {
            this.nivel = nivel;
        }
    }

    // --- Métodos para manejar usos ---
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
