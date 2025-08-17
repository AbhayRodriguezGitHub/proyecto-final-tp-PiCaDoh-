package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;
import mijuego.picadoh.cartas.*;

import java.util.*;

public class PantallaSeleccionTropa implements Screen {
    private final Principal juego;
    private Texture fondo;


    private static final int L_X = 336;
    private static final int L_Y = 75;
    private static final int L_W = 568;
    private static final int L_H = 752 - 75;



    private static final int R_X = 1023;
    private static final int R_Y = 75;
    private static final int R_W = 1589 - 1021;
    private static final int R_H = 752 - 75;


    private final List<Class<? extends CartaTropa>> clasesDisponibles = RegistroCartas.tropasDisponibles();

    private final List<CartaTropa> cartasElegidas = new ArrayList<>();
    private CartaTropa carta1, carta2;
    private boolean esperandoTransicion = false;
    private float tiempoDesdeClick = 0f;
    private CartaTropa cartaSeleccionada = null;

    public PantallaSeleccionTropa(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/ELECCIONTROPA.png"));
        juego.reproducirMusicaSeleccion();

        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (esperandoTransicion) return true;

                int yInvertida = Gdx.graphics.getHeight() - screenY;

                // Hitbox izquierda
                if (screenX >= L_X && screenX <= L_X + L_W && yInvertida >= L_Y && yInvertida <= L_Y + L_H) {
                    cartaSeleccionada = carta1;
                }
                // Hitbox derecha
                else if (screenX >= R_X && screenX <= R_X + R_W && yInvertida >= R_Y && yInvertida <= R_Y + R_H) {
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

    private void avanzarSeleccion() {
        if (cartaSeleccionada != null) cartasElegidas.add(cartaSeleccionada);

        if (cartaSeleccionada == carta1 && carta2 != null) carta2.dispose();
        if (cartaSeleccionada == carta2 && carta1 != null) carta1.dispose();

        carta1 = null;
        carta2 = null;
        cartaSeleccionada = null;

        if (cartasElegidas.size() >= 15) {
            juego.setScreen(new PantallaSeleccionEfecto(juego, cartasElegidas));
            dispose();
        } else {
            generarNuevoParDeCartas();
        }
    }

    private void generarNuevoParDeCartas() {
        try {
            List<Class<? extends CartaTropa>> copia = new ArrayList<>(clasesDisponibles);
            Collections.shuffle(copia);
            Class<? extends CartaTropa> clase1 = copia.get(0);
            Class<? extends CartaTropa> clase2 = copia.get(1);

            carta1 = RegistroCartas.crear(clase1);
            carta2 = RegistroCartas.crear(clase2);
        } catch (Exception e) {
            e.printStackTrace();
            carta1 = null;
            carta2 = null;
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);

        if (!esperandoTransicion) {
            if (carta1 != null) juego.batch.draw(carta1.getImagen(), L_X, L_Y, L_W, L_H);
            if (carta2 != null) juego.batch.draw(carta2.getImagen(), R_X, R_Y, R_W, R_H);
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

    @Override public void resize(int width, int height) {}
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
