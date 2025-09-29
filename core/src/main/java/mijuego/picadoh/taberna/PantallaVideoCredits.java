package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mijuego.picadoh.Principal;

import java.io.File;
import java.io.IOException;

public class PantallaVideoCredits implements Screen {

    private static final float VW = 1920f, VH = 1080f;

    private final Principal juego;
    private final Screen pantallaOrigen;
    private final String rutaArchivo;
    private final boolean reanudarMusicaAlSalir;

    private final OrthographicCamera camara;
    private final Viewport viewport;

    private Process proceso = null;
    private volatile boolean procesoTerminado = false;
    private SpriteBatch batch;
    private Texture fondoNegro;

    public PantallaVideoCredits(Principal juego, Screen pantallaOrigen, String rutaArchivo, boolean reanudarMusicaAlSalir) {
        this.juego = juego;
        this.pantallaOrigen = pantallaOrigen;
        this.rutaArchivo = rutaArchivo;
        this.reanudarMusicaAlSalir = reanudarMusicaAlSalir;

        this.camara = new OrthographicCamera();
        this.viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW/2f, VH/2f, 0f);
        camara.update();

        batch = new SpriteBatch();
        try {
            fondoNegro = new Texture(Gdx.files.absolute("lwjgl3/assets/taberna/NEGRO.png"));
        } catch (Exception e) {
            fondoNegro = null;
        }
    }

    @Override
    public void show() {
        if (reanudarMusicaAlSalir) {
            PantallaTaberna.pauseMusicaActual();
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    terminarYVolver();
                    return true;
                }
                return false;
            }
        });

        Thread t = new Thread(() -> {
            try {
                String absolutePath = new File(rutaArchivo).getAbsolutePath();
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("win")) {
                    pb = new ProcessBuilder("cmd", "/c", "start", "/wait", "\"\"", absolutePath);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", "-W", absolutePath);
                } else {
                    pb = new ProcessBuilder("xdg-open", absolutePath);
                }
                pb.redirectErrorStream(true);
                proceso = pb.start();

                try {
                    proceso.waitFor();
                } catch (InterruptedException ignored) {}
            } catch (IOException e) {
                Gdx.app.postRunnable(this::terminarYVolver);
                return;
            } finally {
                procesoTerminado = true;
                Gdx.app.postRunnable(this::terminarYVolver);
            }
        }, "VideoCredits-Launcher");
        t.setDaemon(true);
        t.start();
    }

    private void terminarYVolver() {
        if (proceso != null && proceso.isAlive()) {
            try { proceso.destroy(); } catch (Exception ignored) {}
            proceso = null;
        }

        if (reanudarMusicaAlSalir) {
            PantallaTaberna.resumeMusicaActual();
        }

        Gdx.input.setInputProcessor(null);
        juego.setScreen(pantallaOrigen);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camara.update();
        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        if (fondoNegro != null) batch.draw(fondoNegro, 0, 0, VW, VH);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            terminarYVolver();
        }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (fondoNegro != null) fondoNegro.dispose();
    }
}
