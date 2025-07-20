package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.*;

public class PantallaSeleccionTropa implements Screen {
    private final Principal juego;
    private Texture fondo;

    private final List<String> todasLasCartas = Arrays.asList("(1)GUARDIANCITO", "(2)BARBOT", "(3)MAFIOSAROSA");
    private final List<String> cartasElegidas = new ArrayList<>();

    private Texture carta1, carta2;
    private String carta1Nombre, carta2Nombre;

    public PantallaSeleccionTropa(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/ELECCIONTROPA.png"));
        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                // Click en CARTA 1 (izquierda)
                if (screenX >= 335 && screenX <= 893 && yInvertida >= 77 && yInvertida <= 736) {
                    System.out.println("Elegiste: " + carta1Nombre);
                    cartasElegidas.add(carta1Nombre);
                    avanzarSeleccion();
                }

                // Click en CARTA 2 (derecha)
                else if (screenX >= 1026 && screenX <= 1580 && yInvertida >= 77 && yInvertida <= 736) {
                    System.out.println("Elegiste: " + carta2Nombre);
                    cartasElegidas.add(carta2Nombre);
                    avanzarSeleccion();
                }

                return true;
            }
        });
    }

    private void generarNuevoParDeCartas() {
        List<String> copia = new ArrayList<>(todasLasCartas);
        Collections.shuffle(copia);
        carta1Nombre = copia.get(0);
        carta2Nombre = copia.get(1);

        carta1 = new Texture(Gdx.files.absolute("lwjgl3/assets/cartas/" + carta1Nombre + ".png"));
        carta2 = new Texture(Gdx.files.absolute("lwjgl3/assets/cartas/" + carta2Nombre + ".png"));
    }

    private void avanzarSeleccion() {
        if (cartasElegidas.size() >= 15) {
            System.out.println(">>> Selección completa! Cartas elegidas:");
            for (String nombre : cartasElegidas) {
                System.out.println(" - " + nombre);
            }
            juego.setScreen(new PantallaSeleccionEfecto(juego));
        } else {
            carta1.dispose();
            carta2.dispose();
            generarNuevoParDeCartas();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        if (carta1 != null && carta2 != null) {
            juego.batch.draw(carta1, 335, 77, 558, 659);  // X, Y, ancho, alto
            juego.batch.draw(carta2, 1026, 77, 554, 659); // X, Y, ancho, alto
        }
        juego.batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        fondo.dispose();
        if (carta1 != null) carta1.dispose();
        if (carta2 != null) carta2.dispose();
    }
}
