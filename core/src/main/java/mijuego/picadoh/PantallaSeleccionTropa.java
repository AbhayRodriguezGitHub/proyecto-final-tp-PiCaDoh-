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

    private final List<Class<? extends CartaTropa>> clasesDisponibles = Arrays.asList(
        Guardiancito.class, Barbot.class, MafiosaRosa.class
    );

    private final List<CartaTropa> cartasElegidas = new ArrayList<>();
    private CartaTropa carta1, carta2;
    private boolean esperandoTransicion = false;
    private float tiempoDesdeClick = 0;
    private CartaTropa cartaSeleccionada = null;

    public PantallaSeleccionTropa(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/ELECCIONTROPA.png"));
        juego.reproducirMusicaSeleccion(); // Música de selección

        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (esperandoTransicion) return true;

                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= 335 && screenX <= 893 && yInvertida >= 77 && yInvertida <= 736) {
                    cartaSeleccionada = carta1;
                    System.out.println("Hiciste clic en la izquierda: " + carta1.getNombre());
                } else if (screenX >= 1026 && screenX <= 1580 && yInvertida >= 77 && yInvertida <= 736) {
                    cartaSeleccionada = carta2;
                    System.out.println("Hiciste clic en la derecha: " + carta2.getNombre());
                } else {
                    cartaSeleccionada = null;
                }

                if (cartaSeleccionada != null) {
                    System.out.println("Elegiste: " + cartaSeleccionada.getNombre());
                    esperandoTransicion = true;
                    tiempoDesdeClick = 0;
                }

                return true;
            }
        });
    }

    private void avanzarSeleccion() {
        if (cartaSeleccionada != null) {
            cartasElegidas.add(cartaSeleccionada); // No hacer dispose() de esta carta
        }

        // Solo dispose() de la carta que NO fue elegida
        if (cartaSeleccionada == carta1 && carta2 != null) carta2.dispose();
        if (cartaSeleccionada == carta2 && carta1 != null) carta1.dispose();

        carta1 = null;
        carta2 = null;
        cartaSeleccionada = null;

        if (cartasElegidas.size() >= 15) {
            System.out.println(">>> Selección completa! Cartas elegidas:");
            for (CartaTropa carta : cartasElegidas) {
                System.out.println(" - " + carta.getNombre());
            }

            // Pasar cartas seleccionadas a la siguiente pantalla
            juego.setScreen(new PantallaSeleccionEfecto(juego, cartasElegidas));
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

            carta1 = clase1.getDeclaredConstructor().newInstance();
            carta2 = clase2.getDeclaredConstructor().newInstance();

            System.out.println("Nuevo par generado:");
            System.out.println(" - Izquierda: " + carta1.getNombre());
            System.out.println(" - Derecha:   " + carta2.getNombre());
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
            if (carta1 != null) {
                juego.batch.draw(carta1.getImagen(), 335, 77, 558, 659);
            }
            if (carta2 != null) {
                juego.batch.draw(carta2.getImagen(), 1026, 77, 554, 659);
            }
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
        fondo.dispose();

        // Solo dispose() de las cartas que quedaron visibles
        if (carta1 != null) carta1.dispose();
        if (carta2 != null) carta2.dispose();

    }
}
