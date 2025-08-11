package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mijuego.picadoh.Principal;
import mijuego.picadoh.PantallaMenu;

public class PantallaVictoria implements Screen {

    private final Principal juego;
    private SpriteBatch batch;
    private Texture imagenVictoria;

    // Coordenadas botón invisible
    private final int botonX1 = 938;
    private final int botonX2 = 1718;
    private final int botonY1 = 542;
    private final int botonY2 = 716;

    public PantallaVictoria(Principal juego) {
        this.juego = juego;
        batch = new SpriteBatch();
        imagenVictoria = new Texture(Gdx.files.internal("lwjgl3/assets/condicion/VICTORIA.png"));
        juego.reproducirMusicaVictoria(); // Música en loop (configurada en Principal)

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Las coordenadas Y de input van invertidas (0 abajo, pantalla arriba)
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= botonX1 && screenX <= botonX2 && yInvertida >= botonY1 && yInvertida <= botonY2) {
                    // Clic dentro del botón invisible
                    juego.setScreen(new PantallaMenu(juego));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void show() {
        // No necesario
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(imagenVictoria, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // No necesario
    }

    @Override
    public void pause() {
        // No necesario
    }

    @Override
    public void resume() {
        // No necesario
    }

    @Override
    public void hide() {
        // No necesario
    }

    @Override
    public void dispose() {
        batch.dispose();
        imagenVictoria.dispose();
    }
}
