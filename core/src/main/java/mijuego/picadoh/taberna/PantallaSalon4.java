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

public class PantallaSalon4 implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;

    public PantallaSalon4(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
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

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON4.png"));

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        TextButton btnSalon3 = new TextButton("", skin, "invisible");
        btnSalon3.setBounds(57, 43, 154 - 57, 133 - 43);
        btnSalon3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon3(juego));
            }
        });
        stage.addActor(btnSalon3);

        TextButton btnSalon5 = new TextButton("", skin, "invisible");
        btnSalon5.setBounds(1740, 43, 1835 - 1740, 133 - 43);
        btnSalon5.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon5(juego));
            }
        });
        stage.addActor(btnSalon5);

        float[] xMin = {238f, 455f, 671f, 890f, 1107f, 1329f, 1538f};
        float[] xMax = {386f, 603f, 820f, 1039f, 1255f, 1473f, 1685f};

        float bottomRow1 = 610f;
        float topRow1 = 778f;
        float heightRow1 = topRow1 - bottomRow1;

        for (int i = 0; i < 7; i++) {
            final String ruta = "lwjgl3/assets/presentaciones/S4" + (i + 1) + ".png";
            TextButton b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], bottomRow1, xMax[i] - xMin[i], heightRow1);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon4.this, ruta));
                }
            });
            stage.addActor(b);
        }

        float bottomRow2 = 360f;
        float topRow2 = 528f;
        float heightRow2 = topRow2 - bottomRow2;

        for (int i = 0; i < 7; i++) {
            final int index = i + 8; // S48 .. S414
            final String ruta = "lwjgl3/assets/presentaciones/S4" + index + ".png";
            TextButton b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], bottomRow2, xMax[i] - xMin[i], heightRow2);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon4.this, ruta));
                }
            });
            stage.addActor(b);
        }
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

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
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
