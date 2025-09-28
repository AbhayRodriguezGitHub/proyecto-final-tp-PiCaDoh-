package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import mijuego.picadoh.Principal;

public class PantallaLibroPagina implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private final Screen pantallaAnterior;
    private final int pagina; // 1..16

    private OrthographicCamera camara;
    private Viewport viewport;
    private Stage stage;
    private Skin skin;
    private Texture imagen;
    private InputMultiplexer multiplexer;

    public PantallaLibroPagina(Principal juego, Screen pantallaAnterior, int pagina) {
        this.juego = juego;
        this.pantallaAnterior = pantallaAnterior;
        this.pagina = Math.max(1, Math.min(16, pagina));
        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();
        imagen = new Texture(Gdx.files.absolute("lwjgl3/assets/libro/L" + this.pagina + ".png"));
    }

    @Override
    public void show() {
        stage = new Stage(viewport);
        skin = new Skin();
        skin.add("default-font", new com.badlogic.gdx.graphics.g2d.BitmapFont());
        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = skin.getFont("default-font");
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        TextButton btnIzq = new TextButton("", skin, "invisible");
        btnIzq.setBounds(36f, 34f, 136f - 36f, 132f - 34f);
        btnIzq.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (pagina <= 1) {
                    juego.setScreen(pantallaAnterior);
                } else {
                    juego.setScreen(new PantallaLibroPagina(juego, pantallaAnterior, pagina - 1));
                }
            }
        });
        stage.addActor(btnIzq);

        TextButton btnDer = new TextButton("", skin, "invisible");
        btnDer.setBounds(1759f, 34f, 1861f - 1759f, 132f - 34f);
        btnDer.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (pagina >= 16) {
                    juego.setScreen(pantallaAnterior);
                } else {
                    juego.setScreen(new PantallaLibroPagina(juego, pantallaAnterior, pagina + 1));
                }
            }
        });
        stage.addActor(btnDer);

        InputAdapter adapter = new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    juego.setScreen(pantallaAnterior);
                    return true;
                }
                return false;
            }
        };

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(adapter);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        juego.batch.setProjectionMatrix(camara.combined);
        juego.batch.begin();
        juego.batch.draw(imagen, 0, 0, VW, VH);
        juego.batch.end();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) { stage.clear(); stage.dispose(); stage = null; }
        if (skin != null) { skin.dispose(); skin = null; }
        if (imagen != null) { imagen.dispose(); imagen = null; }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (imagen != null) imagen.dispose();
    }
}
