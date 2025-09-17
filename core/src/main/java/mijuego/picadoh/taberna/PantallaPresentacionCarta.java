package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import mijuego.picadoh.Principal;

public class PantallaPresentacionCarta implements Screen {

    private static final float VW = 1920f, VH = 1080f;

    private final Principal juego;
    private final Screen pantallaAnterior;

    private final OrthographicCamera camara;
    private final Viewport viewport;
    private Texture imagen;

    public PantallaPresentacionCarta(Principal juego, Screen pantallaAnterior, String rutaAbsolutaImagen) {
        this.juego = juego;
        this.pantallaAnterior = pantallaAnterior;
        this.camara = new OrthographicCamera();
        this.viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        imagen = new Texture(Gdx.files.absolute(rutaAbsolutaImagen));

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    juego.setScreen(pantallaAnterior);
                    return true;
                }
                return false;
            }
        });
    }

    @Override public void show() {}

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            juego.setScreen(pantallaAnterior);
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        juego.batch.setProjectionMatrix(camara.combined);
        juego.batch.begin();
        juego.batch.draw(imagen, 0, 0, VW, VH);
        juego.batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (imagen != null) { imagen.dispose(); imagen = null; }
    }
}
