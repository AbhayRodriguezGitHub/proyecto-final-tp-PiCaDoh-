package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import mijuego.picadoh.Principal;

public class PantallaSalon1 implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;

    public PantallaSalon1(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        // Mantener música (no tocar)
        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON1.png"));

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;  estiloInvisible.down = null;  estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        TextButton btnVolverTaberna = new TextButton("", skin, "invisible");
        btnVolverTaberna.setBounds(256, 311, 356 - 256, 409 - 311); // w=100, h=98
        btnVolverTaberna.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaTaberna(juego));
            }
        });
        stage.addActor(btnVolverTaberna);

        TextButton btnIrSalon2 = new TextButton("", skin, "invisible");
        btnIrSalon2.setBounds(1539, 317, 1635 - 1539, 406 - 317); // w=96, h=89
        btnIrSalon2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon2(juego));
            }
        });
        stage.addActor(btnIrSalon2);

        TextButton btnS11 = new TextButton("", skin, "invisible");
        btnS11.setBounds(234f, 697f, 147f, 161f);
        btnS11.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(
                    juego,
                    PantallaSalon1.this,
                    "lwjgl3/assets/presentaciones/S11.png"
                ));
            }
        });
        stage.addActor(btnS11);
    }

    @Override
    public void render(float delta) {
        juego.batch.setProjectionMatrix(camara.combined);
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, VW, VH);
        juego.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        // Mantener música (no detener)
        Gdx.input.setInputProcessor(null);
        if (stage != null) { stage.clear(); stage.dispose(); stage = null; }
        if (skin  != null) { skin.dispose();  skin  = null; }
        if (fondo != null) { fondo.dispose(); fondo = null; }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin  != null) skin.dispose();
        if (fondo != null) fondo.dispose();
    }
}
