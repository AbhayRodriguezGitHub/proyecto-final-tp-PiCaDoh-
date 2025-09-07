package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mijuego.picadoh.Principal;
import mijuego.picadoh.PantallaMenu;

public class PantallaDerrota implements Screen {

    private final Principal juego;
    private SpriteBatch batch;
    private Texture imagenDerrota;

    // Coordenadas botón invisible
    private final int botonX1 = 1010;
    private final int botonX2 = 1853;
    private final int botonY1 = 568;
    private final int botonY2 = 740;

    public PantallaDerrota(Principal juego) {
        this.juego = juego;
        batch = new SpriteBatch();
        imagenDerrota = new Texture(Gdx.files.internal("lwjgl3/assets/condicion/DERROTA.png"));
        juego.reproducirMusicaDerrota(); // Música en loop

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Invertimos Y para que coincida con el sistema gráfico
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= botonX1 && screenX <= botonX2 && yInvertida >= botonY1 && yInvertida <= botonY2) {
                    // Click dentro del botón invisible
                    juego.setScreen(new PantallaMenu(juego));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void show() {
        // No se necesita por ahora
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(imagenDerrota, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // No se necesita por ahora
    }

    @Override
    public void pause() {
        // No se necesita por ahora
    }

    @Override
    public void resume() {
        // No se necesita por ahora
    }

    @Override
    public void hide() {
        // No se necesita por ahora
    }

    @Override
    public void dispose() {
        batch.dispose();
        imagenDerrota.dispose();
    }
}
