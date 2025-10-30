package mijuego.picadoh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class PantallaManual implements Screen {

    private static final float VW = 1920f, VH = 1080f;
    private final Principal juego;
    private final Screen pantallaOrigen;
    private OrthographicCamera camara;
    private Viewport viewport;
    private int paginaActual = 1;
    private Texture imagen;

    // Música del manual
    private Music musicaManual;

    public PantallaManual(Principal juego, Screen pantallaOrigen) {
        this.juego = juego;
        this.pantallaOrigen = pantallaOrigen;

        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        cargarPagina(paginaActual);
    }

    private void cargarPagina(int idx) {
        if (imagen != null) {
            imagen.dispose();
            imagen = null;
        }
        String ruta = "lwjgl3/assets/manual/M" + idx + ".png";
        try {
            imagen = new Texture(Gdx.files.absolute(ruta));
        } catch (Exception e) {
            imagen = null;
        }
    }

    @Override
    public void show() {
        // Preparar y reproducir música del manual
        if (musicaManual == null) {
            musicaManual = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/manual/MUSICAMANUAL.mp3"));
            musicaManual.setLooping(true);
        }
        musicaManual.setVolume(juego.getVolumenMusica());

        // Detener lo que esté sonando y reproducir el manual
        juego.detenerMusicaActual();
        musicaManual.play();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    // Al salir del manual, detener música del manual y volver a la del menú
                    if (musicaManual != null && musicaManual.isPlaying()) {
                        musicaManual.stop();
                    }
                    juego.reproducirMusica(); // vuelve a la música del menú principal
                    Gdx.input.setInputProcessor(null);
                    juego.setScreen(pantallaOrigen);
                    return true;
                }
                if (keycode == Input.Keys.RIGHT) {
                    if (paginaActual < 17) {
                        paginaActual++;
                        cargarPagina(paginaActual);
                    }
                    return true;
                }
                if (keycode == Input.Keys.LEFT) {
                    if (paginaActual > 1) {
                        paginaActual--;
                        cargarPagina(paginaActual);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camara.update();
        juego.batch.setProjectionMatrix(camara.combined);
        juego.batch.begin();
        if (imagen != null) juego.batch.draw(imagen, 0, 0, VW, VH);
        juego.batch.end();
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override public void pause() {}

    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        // Por seguridad, si se oculta la pantalla, parar la música del manual
        if (musicaManual != null && musicaManual.isPlaying()) {
            musicaManual.stop();
        }
        // No llamamos aquí a reproducirMusica() para evitar doble reproducción
        // (ya se hace al presionar ESC y cambiar de pantalla).
    }

    @Override
    public void dispose() {
        if (imagen != null) imagen.dispose();
        if (musicaManual != null) {
            musicaManual.dispose();
            musicaManual = null;
        }
    }
}
