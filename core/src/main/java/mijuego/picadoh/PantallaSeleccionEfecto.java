package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.batalla.PantallaBatalla;
import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.efectos.*;

import java.util.*;

public class PantallaSeleccionEfecto implements Screen {
    private final Principal juego;
    private Texture fondo;

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private OrthographicCamera camara;
    private Viewport viewport;

    private static final int L_X = 336;
    private static final int L_Y = 75;
    private static final int L_W = 924 - 336;
    private static final int L_H = 793 - 75;

    private static final int R_W = L_W;
    private static final int R_H = L_H;
    private static final int R_X = 1609 - R_W;
    private static final int R_Y = 75;

    private final List<Class<? extends CartaEfecto>> clasesEfecto = Arrays.asList(
        Acelereitor.class,
        EscudoReal.class,
        ExplosionForzal.class,
        MagiaBendita.class,
        SenoraArmadura.class,
        Tyson.class,
        AnarquiaNivel.class,
        Bombardrilo.class,
        Monarquia.class,
        Rebelion.class,
        MalDeAmores.class,
        MagoDel8.class,
        Paracetamol.class,
        Intercambio.class,
        Avaricioso.class,
        AgenteDeTransito.class,
        EscudoFalso.class,
        EscudoPlatinado.class,
        Gangsterio.class,
        Orikalkus.class
    );

    private final List<CartaEfecto> cartasEfectoElegidas = new ArrayList<>();
    private final List<CartaTropa> cartasTropaSeleccionadas;

    private CartaEfecto carta1, carta2;
    private boolean pantallaFinalizada = false;

    private boolean esperandoTransicion = false;
    private float tiempoDesdeClick = 0f;
    private CartaEfecto cartaSeleccionada = null;

    public PantallaSeleccionEfecto(Principal juego, List<CartaTropa> cartasTropaSeleccionadas) {
        this.juego = juego;
        this.cartasTropaSeleccionadas = cartasTropaSeleccionadas;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/ELECCIONEFECTO.png"));

        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            private final Vector2 tmp = new Vector2();
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (esperandoTransicion || pantallaFinalizada) return true;
                tmp.set(screenX, screenY);
                viewport.unproject(tmp);
                if (tmp.x >= L_X && tmp.x <= L_X + L_W && tmp.y >= L_Y && tmp.y <= L_Y + L_H) {
                    cartaSeleccionada = carta1;
                } else if (tmp.x >= R_X && tmp.x <= R_X + R_W && tmp.y >= R_Y && tmp.y <= R_Y + R_H) {
                    cartaSeleccionada = carta2;
                } else {
                    cartaSeleccionada = null;
                }
                if (cartaSeleccionada != null) {
                    esperandoTransicion = true;
                    tiempoDesdeClick = 0f;
                }
                return true;
            }
        });
    }

    private void generarNuevoParDeCartas() {
        Random rand = new Random();
        try {
            Class<? extends CartaEfecto> clase1 = clasesEfecto.get(rand.nextInt(clasesEfecto.size()));
            Class<? extends CartaEfecto> clase2;
            do {
                clase2 = clasesEfecto.get(rand.nextInt(clasesEfecto.size()));
            } while (clase1.equals(clase2));
            carta1 = clase1.getDeclaredConstructor().newInstance();
            carta2 = clase2.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void avanzarSeleccion() {
        if (cartaSeleccionada != null) cartasEfectoElegidas.add(cartaSeleccionada);
        if (cartaSeleccionada == carta1 && carta2 != null) carta2.dispose();
        if (cartaSeleccionada == carta2 && carta1 != null) carta1.dispose();
        carta1 = null;
        carta2 = null;
        cartaSeleccionada = null;

        if (cartasEfectoElegidas.size() >= 7) {
            if (pantallaFinalizada) return;
            pantallaFinalizada = true;
            ContextoBatalla contexto = new ContextoBatalla(cartasTropaSeleccionadas, new ArrayList<>(), 80, 80);
            juego.setScreen(new PantallaBatalla(juego, contexto, cartasEfectoElegidas));
            dispose();
        } else {
            generarNuevoParDeCartas();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.setProjectionMatrix(camara.combined);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, VW, VH);
        if (!esperandoTransicion && carta1 != null && carta2 != null) {
            juego.batch.draw(carta1.getImagen(), L_X, L_Y, L_W, L_H);
            juego.batch.draw(carta2.getImagen(), R_X, R_Y, R_W, R_H);
        }
        juego.batch.end();

        if (esperandoTransicion) {
            tiempoDesdeClick += delta;
            if (tiempoDesdeClick >= 0.25f) {
                esperandoTransicion = false;
                avanzarSeleccion();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (fondo != null) fondo.dispose();
        if (carta1 != null) carta1.dispose();
        if (carta2 != null) carta2.dispose();
    }
}
