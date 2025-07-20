package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.*;

public class PantallaSeleccionEfecto implements Screen {
    private final Principal juego;
    private Texture fondo;

    private final List<String> todasLasCartasEfecto = Arrays.asList(
        "ACELEREITOR", "SEÑORAARMADURA", "MAGIABENDITA", "EXPLOSIONFORZAL", "ESCUDOREAL"
    );
    private final List<String> cartasEfectoElegidas = new ArrayList<>();

    private Texture carta1, carta2;
    private String carta1Nombre, carta2Nombre;

    public PantallaSeleccionEfecto(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/ELECCIONEFECTO.png"));
        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                // CARTA 1: desde X:338 hasta 918, Y:787 hasta 70
                if (screenX >= 338 && screenX <= 918 && yInvertida >= 70 && yInvertida <= 787) {
                    System.out.println("Elegiste efecto: " + carta1Nombre);
                    cartasEfectoElegidas.add(carta1Nombre);
                    avanzarSeleccion();
                }

                // CARTA 2: desde X:1029 hasta 1601, Y:787 hasta 70
                else if (screenX >= 1029 && screenX <= 1601 && yInvertida >= 70 && yInvertida <= 787) {
                    System.out.println("Elegiste efecto: " + carta2Nombre);
                    cartasEfectoElegidas.add(carta2Nombre);
                    avanzarSeleccion();
                }

                return true;
            }
        });
    }

    private void generarNuevoParDeCartas() {
        Random random = new Random();

        // Elegir primera carta al azar
        carta1Nombre = todasLasCartasEfecto.get(random.nextInt(todasLasCartasEfecto.size()));

        // Elegir segunda carta distinta
        do {
            carta2Nombre = todasLasCartasEfecto.get(random.nextInt(todasLasCartasEfecto.size()));
        } while (carta2Nombre.equals(carta1Nombre));

        // Cargar texturas
        carta1 = new Texture(Gdx.files.absolute("lwjgl3/assets/efectos/" + carta1Nombre + ".png"));
        carta2 = new Texture(Gdx.files.absolute("lwjgl3/assets/efectos/" + carta2Nombre + ".png"));
    }


    private void avanzarSeleccion() {
        if (cartasEfectoElegidas.size() >= 7) {
            System.out.println(">>> Selección de efectos completa!");
            for (String nombre : cartasEfectoElegidas) {
                System.out.println(" - " + nombre);
            }

            // En el futuro: juego.setScreen(new PantallaBatalla(juego));
        } else {
            if (carta1 != null) carta1.dispose();
            if (carta2 != null) carta2.dispose();
            generarNuevoParDeCartas();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        if (carta1 != null && carta2 != null) {
            juego.batch.draw(carta1, 338, 70, 580, 717);  // Tamaño para carta 1
            juego.batch.draw(carta2, 1029, 70, 572, 717); // Tamaño para carta 2
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
