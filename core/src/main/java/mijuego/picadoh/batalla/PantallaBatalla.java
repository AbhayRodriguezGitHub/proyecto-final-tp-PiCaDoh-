package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mijuego.picadoh.Principal;
import mijuego.picadoh.efectos.CartaEfecto;
import mijuego.picadoh.cartas.CartaTropa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PantallaBatalla implements Screen {

    private final Principal juego;
    private final SpriteBatch batch;
    private final Texture fondo;
    private final ContextoBatalla contexto;
    private final List<CartaEfecto> efectosDisponibles;

    // === Datos de la mano de tropas ===
    private final List<CartaTropa> manoTropas;
    private CartaTropa cartaSeleccionada;
    private int cartaHoverIndex = -1;

    // === Constantes de tamaño/posición de cartas en mano ===
    private final float ANCHO_CARTA = 100f;
    private final float ALTURA_CARTA = 150f;
    private final float Y_CARTA_MANO = 40f;

    public PantallaBatalla(Principal juego, ContextoBatalla contexto, List<CartaEfecto> efectosDisponibles) {
        this.juego = juego;
        this.batch = new SpriteBatch();
        this.fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VALLEMISTICO.png"));
        this.contexto = contexto;
        this.efectosDisponibles = efectosDisponibles;
        this.manoTropas = new ArrayList<>();

        // === Inicializar mano con 3 cartas al azar ===
        List<CartaTropa> disponibles = new ArrayList<>(contexto.getTropasPropias());
        Collections.shuffle(disponibles);
        for (int i = 0; i < 3 && i < disponibles.size(); i++) {
            manoTropas.add(disponibles.get(i));
        }

        // === Input para selección y hover ===
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = 40 + i * (ANCHO_CARTA + 10);
                    if (screenX >= x && screenX <= x + ANCHO_CARTA &&
                        yInvertida >= Y_CARTA_MANO && yInvertida <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaSeleccionada = manoTropas.get(i);
                        System.out.println("Seleccionaste carta: " + cartaSeleccionada.getNombre());
                        break;
                    }
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;
                cartaHoverIndex = -1;
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = 40 + i * (ANCHO_CARTA + 10);
                    if (screenX >= x && screenX <= x + ANCHO_CARTA &&
                        yInvertida >= Y_CARTA_MANO && yInvertida <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaHoverIndex = i;
                        break;
                    }
                }
                return false;
            }
        });

        System.out.println("PantallaBatalla iniciada:");
        System.out.println("Tropas en contexto: " + contexto.getTropasPropias().size());
        System.out.println("Efectos disponibles: " + efectosDisponibles.size());
    }

    @Override
    public void show() {
        juego.detenerMusica();
        juego.detenerMusicaSeleccion();
        juego.reproducirMusicaBatalla();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(fondo, 0, 0, 1920, 1080);

        // Dibujar cartas en la mano (tropas)
        for (int i = 0; i < manoTropas.size(); i++) {
            float x = 40 + i * (ANCHO_CARTA + 10);
            float y = Y_CARTA_MANO;

            if (i == cartaHoverIndex) {
                y += 20;  // levantar carta al pasar el cursor
            }

            Texture img = manoTropas.get(i).getImagen();
            if (img != null) {
                batch.draw(img, x, y, ANCHO_CARTA, ALTURA_CARTA);
            } else {
                System.err.println("Imagen de carta null para: " + manoTropas.get(i).getNombre());
            }
        }

        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fondo.dispose();
        // Puedes añadir manoTropas.forEach(c -> c.getImagen().dispose()); si cargas texturas nuevas dinámicamente.
    }

    public ContextoBatalla getContexto() {
        return contexto;
    }

    public List<CartaEfecto> getEfectosDisponibles() {
        return efectosDisponibles;
    }
}
