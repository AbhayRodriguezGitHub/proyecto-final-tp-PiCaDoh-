package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mijuego.picadoh.Principal;
import mijuego.picadoh.PantallaMenu;
import mijuego.picadoh.taberna.PantallaTaberna;

public class PantallaEmpate implements Screen {

    private final Principal juego;
    private SpriteBatch batch;
    private Texture imagenEmpate;

    private static final int BOTON_X1 = 85;
    private static final int BOTON_X2 = 831;
    private static final int BOTON_Y1 = 715;
    private static final int BOTON_Y2 = 872;

    private static final int BOTON_TABERNA_X1 = 1058;
    private static final int BOTON_TABERNA_X2 = 1814;
    private static final int BOTON_TABERNA_Y1 = 716;
    private static final int BOTON_TABERNA_Y2 = 873;

    public PantallaEmpate(Principal juego) {
        this.juego = juego;
        batch = new SpriteBatch();
        imagenEmpate = new Texture(Gdx.files.internal("lwjgl3/assets/condicion/EMPATE.png"));

        juego.reproducirMusicaEmpate();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= BOTON_X1 && screenX <= BOTON_X2 &&
                    yInvertida >= BOTON_Y1 && yInvertida <= BOTON_Y2) {
                    juego.setScreen(new PantallaMenu(juego));
                    return true;
                }

                if (screenX >= BOTON_TABERNA_X1 && screenX <= BOTON_TABERNA_X2 &&
                    yInvertida >= BOTON_TABERNA_Y1 && yInvertida <= BOTON_TABERNA_Y2) {
                    juego.detenerMusicaActual();
                    juego.setScreen(new PantallaTaberna(juego));
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public void show() {}

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
