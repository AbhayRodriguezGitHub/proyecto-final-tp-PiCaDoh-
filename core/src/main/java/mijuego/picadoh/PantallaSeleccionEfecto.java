package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

import mijuego.picadoh.efectos.*;

import java.util.*;

public class PantallaSeleccionEfecto implements Screen {
    private final Principal juego;
    private Texture fondo;

    private final List<Class<? extends CartaEfecto>> clasesEfecto = Arrays.asList(
        ExplosionForzal.class,
        SenoraArmadura.class,
        MagiaBendita.class,
        EscudoReal.class,
        Acelereitor.class
    );

    private final List<CartaEfecto> cartasEfectoElegidas = new ArrayList<>();

    private CartaEfecto carta1, carta2;

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

                if (screenX >= 338 && screenX <= 918 && yInvertida >= 70 && yInvertida <= 787) {
                    System.out.println("Elegiste efecto: " + carta1.getNombre());
                    cartasEfectoElegidas.add(carta1);
                    avanzarSeleccion();
                } else if (screenX >= 1029 && screenX <= 1601 && yInvertida >= 70 && yInvertida <= 787) {
                    System.out.println("Elegiste efecto: " + carta2.getNombre());
                    cartasEfectoElegidas.add(carta2);
                    avanzarSeleccion();
                }

                return true;
            }
        });
    }

    private void generarNuevoParDeCartas() {
        // Repetimos hasta tener dos efectos distintos
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
        if (cartasEfectoElegidas.size() >= 7) {
            System.out.println(">>> Selección de efectos completa!");
            for (CartaEfecto carta : cartasEfectoElegidas) {
                System.out.println(" - " + carta.getNombre());
            }

            // En el futuro: juego.setScreen(new PantallaBatalla(juego, cartasElegidas, cartasEfectoElegidas));
        } else {
            // Importante: no necesitamos hacer dispose de cartaX.getImagen() aquí porque no se reutilizan.
            generarNuevoParDeCartas();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);

        if (carta1 != null && carta2 != null) {
            juego.batch.draw(carta1.getImagen(), 338, 70, 580, 717);
            juego.batch.draw(carta2.getImagen(), 1029, 70, 572, 717);
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
        for (CartaEfecto carta : cartasEfectoElegidas) {
            carta.dispose();
        }
        if (carta1 != null) carta1.dispose();
        if (carta2 != null) carta2.dispose();
    }
}
