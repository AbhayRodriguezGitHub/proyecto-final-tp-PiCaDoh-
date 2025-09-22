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

public class PantallaSalon2 implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;

    public PantallaSalon2(Principal juego) {
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

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON2.png"));

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        TextButton btnSalon1 = new TextButton("", skin, "invisible");
        btnSalon1.setBounds(260, 299, 360 - 260, 390 - 299);
        btnSalon1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon1(juego));
            }
        });
        stage.addActor(btnSalon1);

        TextButton btnSalon3 = new TextButton("", skin, "invisible");
        btnSalon3.setBounds(1532, 302, 1639 - 1532, 394 - 302);
        btnSalon3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaSalon3(juego));
            }
        });
        stage.addActor(btnSalon3);

        float[] xMin = {234f, 446f, 661f, 872f, 1085f, 1298f, 1511f};
        float[] xMax = {381f, 596f, 809f, 1024f, 1235f, 1446f, 1657f};

        TextButton b;

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[0], 697f, xMax[0] - xMin[0], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S21.png"));
            }
        });
        stage.addActor(b);

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[1], 697f, xMax[1] - xMin[1], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S22.png"));
            }
        });
        stage.addActor(b);

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[2], 697f, xMax[2] - xMin[2], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S23.png"));
            }
        });
        stage.addActor(b);

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[3], 697f, xMax[3] - xMin[3], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S24.png"));
            }
        });
        stage.addActor(b);

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[4], 697f, xMax[4] - xMin[4], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S25.png"));
            }
        });
        stage.addActor(b);

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[5], 697f, xMax[5] - xMin[5], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S26.png"));
            }
        });
        stage.addActor(b);

        b = new TextButton("", skin, "invisible");
        b.setBounds(xMin[6], 697f, xMax[6] - xMin[6], 858f - 697f);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, "lwjgl3/assets/presentaciones/S27.png"));
            }
        });
        stage.addActor(b);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            b = new TextButton("", skin, "invisible");
            b.setBounds(xMin[i], 486f, xMax[i] - xMin[i], 648f - 486f);
            final String ruta;
            switch (i) {
                case 0: ruta = "lwjgl3/assets/presentaciones/S28.png"; break;
                case 1: ruta = "lwjgl3/assets/presentaciones/S29.png"; break;
                case 2: ruta = "lwjgl3/assets/presentaciones/S210.png"; break;
                case 3: ruta = "lwjgl3/assets/presentaciones/S211.png"; break;
                case 4: ruta = "lwjgl3/assets/presentaciones/S212.png"; break;
                case 5: ruta = "lwjgl3/assets/presentaciones/S213.png"; break;
                default: ruta = "lwjgl3/assets/presentaciones/S214.png"; break;
            }
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, ruta));
                }
            });
            stage.addActor(b);
        }

        float[] xMinLast = {446f, 661f, 872f, 1085f, 1298f};
        float[] xMaxLast = {596f, 809f, 1024f, 1235f, 1446f};
        String[] rutasLast = {
            "lwjgl3/assets/presentaciones/S215.png",
            "lwjgl3/assets/presentaciones/S216.png",
            "lwjgl3/assets/presentaciones/S217.png",
            "lwjgl3/assets/presentaciones/S218.png",
            "lwjgl3/assets/presentaciones/S219.png"
        };

        for (int i = 0; i < xMinLast.length; i++) {
            final String ruta = rutasLast[i];
            b = new TextButton("", skin, "invisible");
            b.setBounds(xMinLast[i], 268f, xMaxLast[i] - xMinLast[i], 448f - 268f);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaPresentacionCarta(juego, PantallaSalon2.this, ruta));
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
