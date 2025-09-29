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

public class PantallaDerrota implements Screen {

    private final Principal juego;
    private SpriteBatch batch;
    private Texture imagenDerrota;

    private final int botonX1 = 1010;
    private final int botonX2 = 1853;
    private final int botonY1 = 568;
    private final int botonY2 = 740;

    public PantallaDerrota(Principal juego) {
        this.juego = juego;
        batch = new SpriteBatch();
        imagenDerrota = new Texture(Gdx.files.internal("lwjgl3/assets/condicion/DERROTA.png"));
        juego.reproducirMusicaDerrota();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;

                if (screenX >= botonX1 && screenX <= botonX2 && yInvertida >= botonY1 && yInvertida <= botonY2) {
                    juego.setScreen(new PantallaMenu(juego));
                    return true;
                }

                if (screenX >= 1017 && screenX <= 1861 && yInvertida >= 309 && yInvertida <= 484) {
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
        batch.draw(imagenDerrota, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        imagenDerrota.dispose();
    }
}
