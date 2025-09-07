package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.batalla.PantallaBatalla;
import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.efectos.*;

import java.util.*;

public class PantallaSeleccionEfecto implements Screen {
    private final Principal juego;
    private Texture fondo;

    private static final int L_X = 336;
    private static final int L_Y = 75;
    private static final int L_W = 924 - 336; // 588
    private static final int L_H = 793 - 75;  // 718

    private static final int R_W = L_W;       // 588
    private static final int R_H = L_H;       // 718
    private static final int R_X = 1609 - R_W; // 1021
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
        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (esperandoTransicion || pantallaFinalizada) return true;

                int yInv = Gdx.graphics.getHeight() - screenY;

                if (screenX >= L_X && screenX <= L_X + L_W && yInv >= L_Y && yInv <= L_Y + L_H) {
                    cartaSeleccionada = carta1;
                } else if (screenX >= R_X && screenX <= R_X + R_W && yInv >= R_Y && yInv <= R_Y + R_H) {
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

        // liberar la no elegida
        if (cartaSeleccionada == carta1 && carta2 != null) carta2.dispose();
        if (cartaSeleccionada == carta2 && carta1 != null) carta1.dispose();

        carta1 = null;
        carta2 = null;
        cartaSeleccionada = null;

        if (cartasEfectoElegidas.size() >= 7) {
            if (pantallaFinalizada) return;
            pantallaFinalizada = true;

            ContextoBatalla contexto =
                new ContextoBatalla(cartasTropaSeleccionadas, new ArrayList<>(), 80, 80);

            juego.setScreen(new PantallaBatalla(juego, contexto, cartasEfectoElegidas));
            dispose();
        } else {
            generarNuevoParDeCartas();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);

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
