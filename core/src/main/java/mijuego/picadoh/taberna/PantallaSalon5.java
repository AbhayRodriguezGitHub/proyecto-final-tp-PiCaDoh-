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

public class PantallaSalon5 implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;

    public PantallaSalon5(Principal juego) {
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

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON5.png"));

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        TextButton btnSalon4 = new TextButton("", skin, "invisible");
        btnSalon4.setBounds(57, 43, 154 - 57, 133 - 43);
        btnSalon4.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon4(juego));
            }
        });
        stage.addActor(btnSalon4);

        TextButton btnVolverTaberna = new TextButton("", skin, "invisible");
        btnVolverTaberna.setBounds(1740, 43, 1835 - 1740, 133 - 43);
        btnVolverTaberna.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaTaberna(juego));
            }
        });
        stage.addActor(btnVolverTaberna);

        float[] xMin = {387f, 714f, 1043f, 1369f};
        float[] xMax = {535f, 864f, 1193f, 1522f};

        float bottomRow1 = 672f;
        float topRow1 = 853f;
        float heightRow1 = topRow1 - bottomRow1;

        for (int i = 0; i < 4; i++) {
            final String ruta = "lwjgl3/assets/presentaciones/S5" + (i + 1) + ".png";
            TextButton b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], bottomRow1, xMax[i] - xMin[i], heightRow1);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon5.this, ruta));
                }
            });
            stage.addActor(b);
        }

        float bottomRow2 = 448f;
        float topRow2 = 631f;
        float heightRow2 = topRow2 - bottomRow2;

        for (int i = 0; i < 4; i++) {
            final int index = i + 5;
            final String ruta = "lwjgl3/assets/presentaciones/S5" + index + ".png";
            TextButton b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], bottomRow2, xMax[i] - xMin[i], heightRow2);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon5.this, ruta));
                }
            });
            stage.addActor(b);
        }

        float bottomRow3 = 218f;
        float topRow3 = 399f;
        float heightRow3 = topRow3 - bottomRow3;

        for (int i = 0; i < 4; i++) {
            final int index = i + 9;
            final String ruta = "lwjgl3/assets/presentaciones/S5" + index + ".png";
            TextButton b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], bottomRow3, xMax[i] - xMin[i], heightRow3);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon5.this, ruta));
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
