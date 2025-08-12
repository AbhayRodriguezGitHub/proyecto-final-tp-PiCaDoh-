package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mijuego.picadoh.Principal;
import mijuego.picadoh.PantallaMenu;

public class PantallaEmpate implements Screen {

    private final Principal juego;
    private SpriteBatch batch;
    private Texture imagenEmpate;

    // Coordenadas botón invisible (rectángulo)
    // X: 85 a 831  |  Y: 715 a 872
    private static final int BOTON_X1 = 85;
    private static final int BOTON_X2 = 831;
    private static final int BOTON_Y1 = 715;
    private static final int BOTON_Y2 = 872;

    public PantallaEmpate(Principal juego) {
        this.juego = juego;
        batch = new SpriteBatch();
        imagenEmpate = new Texture(Gdx.files.internal("lwjgl3/assets/condicion/EMPATE.png"));

        // Reproduce EMPATE.mp3
        juego.reproducirMusicaEmpate();

        // Input para botón invisible
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Invertimos Y (en render 0 está abajo)
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= BOTON_X1 && screenX <= BOTON_X2 &&
                    yInvertida >= BOTON_Y1 && yInvertida <= BOTON_Y2) {
                    // Click dentro del rectángulo invisible -> volver al menú
                    juego.setScreen(new PantallaMenu(juego));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void show() {
        // No es necesario por ahora
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(imagenEmpate, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        imagenEmpate.dispose();
    }
}
