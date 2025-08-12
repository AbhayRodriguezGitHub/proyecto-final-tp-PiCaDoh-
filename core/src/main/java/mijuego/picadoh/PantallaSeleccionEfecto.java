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

    private final List<Class<? extends CartaEfecto>> clasesEfecto = Arrays.asList(
        ExplosionForzal.class,
        SenoraArmadura.class,
        MagiaBendita.class,
        EscudoReal.class,
        Acelereitor.class
    );

    private final List<CartaEfecto> cartasEfectoElegidas = new ArrayList<>();
    private final List<CartaTropa> cartasTropaSeleccionadas;

    private CartaEfecto carta1, carta2;
    private boolean pantallaFinalizada = false;

    // NUEVO: variables para transición tipo PantallaSeleccionTropa
    private boolean esperandoTransicion = false;
    private float tiempoDesdeClick = 0;
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

                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= 338 && screenX <= 918 && yInvertida >= 70 && yInvertida <= 787) {
                    cartaSeleccionada = carta1;
                    System.out.println("Elegiste efecto: " + carta1.getNombre());
                } else if (screenX >= 1029 && screenX <= 1601 && yInvertida >= 70 && yInvertida <= 787) {
                    cartaSeleccionada = carta2;
                    System.out.println("Elegiste efecto: " + carta2.getNombre());
                } else {
                    cartaSeleccionada = null;
                }

                if (cartaSeleccionada != null) {
                    esperandoTransicion = true;
                    tiempoDesdeClick = 0;
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
        if (cartaSeleccionada != null) {
            cartasEfectoElegidas.add(cartaSeleccionada);
        }

        // Liberar solo la carta no elegida
        if (cartaSeleccionada == carta1 && carta2 != null) carta2.dispose();
        if (cartaSeleccionada == carta2 && carta1 != null) carta1.dispose();

        carta1 = null;
        carta2 = null;
        cartaSeleccionada = null;

        if (cartasEfectoElegidas.size() >= 7) {
            if (pantallaFinalizada) return;
            pantallaFinalizada = true;

            System.out.println(">>> Selección de efectos completa!");
            for (CartaEfecto carta : cartasEfectoElegidas) {
                System.out.println(" - " + carta.getNombre());
            }

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
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);

        if (!esperandoTransicion && carta1 != null && carta2 != null) {
            juego.batch.draw(carta1.getImagen(), 338, 70, 580, 717);
            juego.batch.draw(carta2.getImagen(), 1029, 70, 572, 717);
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
        // NO liberar cartasEfectoElegidas aquí: las usa PantallaBatalla
        if (carta1 != null) carta1.dispose();
        if (carta2 != null) carta2.dispose();
    }
}
