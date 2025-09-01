package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import mijuego.picadoh.taberna.PantallaTaberna;

public class PantallaMenu implements Screen {
    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Skin skin;

    public PantallaMenu(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/FONDOMENU1.png"));

        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        juego.reproducirMusica();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("default", estiloInvisible);

        TextButton btnConfig = new TextButton("", skin);
        btnConfig.setBounds(20, 20, 80, 80);
        btnConfig.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaConfiguracion(juego));
            }
        });
        stage.addActor(btnConfig);

        TextButton btnBatalla = new TextButton("", skin);
        btnBatalla.setBounds(320, 750, 640, 180);
        btnBatalla.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.detenerMusica();
                juego.setScreen(new PantallaSeleccionTropa(juego));
            }
        });
        stage.addActor(btnBatalla);

        TextButton btnSalir = new TextButton("", skin);
        btnSalir.setBounds(320, 180, 640, 180);
        btnSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        stage.addActor(btnSalir);

        TextButton btnTaberna = new TextButton("", skin);
        btnTaberna.setBounds(325, 475, 958 - 325, 650 - 475);
        btnTaberna.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Corta música actual y cambia de pantalla
                juego.detenerMusicaActual();
                juego.setScreen(new PantallaTaberna(juego));
            }
        });
        stage.addActor(btnTaberna);
    }

    @Override
    public void render(float delta) {
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        juego.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        juego.aplicarCursor();
    }

    @Override public void pause() {}
    @Override public void resume() {}


    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) {
            stage.clear();
            stage.dispose();
            stage = null;
        }
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        if (fondo != null) {
            fondo.dispose();
            fondo = null;
        }
    }

    @Override
    public void dispose() {

        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (fondo != null) fondo.dispose();
    }
}
