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

public class PantallaSalon3 implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;

    public PantallaSalon3(Principal juego) {
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

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON3.png"));

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        TextButton btnSalon2 = new TextButton("", skin, "invisible");
        btnSalon2.setBounds(57, 43, 154 - 57, 133 - 43);
        btnSalon2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon2(juego));
            }
        });
        stage.addActor(btnSalon2);

        TextButton btnSalon4 = new TextButton("", skin, "invisible");
        btnSalon4.setBounds(1740, 43, 1835 - 1740, 133 - 43);
        btnSalon4.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon4(juego));
            }
        });
        stage.addActor(btnSalon4);

        float[] xMin = {222f, 407f, 594f, 778f, 965f, 1150f, 1335f, 1522f};
        float[] xMax = {374f, 559f, 747f, 929f, 1116f, 1302f, 1489f, 1679f};

        TextButton b;

        float yMinRow1 = 619f;
        float heightRow1 = 787f - 619f;

        for (int i = 0; i < 8; i++) {
            final String ruta = "lwjgl3/assets/presentaciones/S3" + (i + 1) + ".png";
            b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], yMinRow1, xMax[i] - xMin[i], heightRow1);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon3.this, ruta));
                }
            });
            stage.addActor(b);
        }

        float yMinRow2 = 375f;
        float heightRow2 = 553f - 375f;

        for (int i = 0; i < 8; i++) {
            final int index = i + 9; // S39 .. S316
            final String ruta = "lwjgl3/assets/presentaciones/S3" + index + ".png";
            b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], yMinRow2, xMax[i] - xMin[i], heightRow2);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon3.this, ruta));
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
