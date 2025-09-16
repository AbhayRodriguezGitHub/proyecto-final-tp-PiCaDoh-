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

    private static final float VW = 1920f;  // ancho virtual
    private static final float VH = 1080f;  // alto  virtual

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    // Cámara + Viewport (para que fondo y botones se reescalen juntos)
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

        // Cámara + Viewport (igual que Taberna y los otros Salones)
        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        // Stage usando el MISMO viewport
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        // Fondo
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON4.png"));

        // UI invisible
        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("default", estiloInvisible);

        // Botón VOLVER A SALÓN 3 — X: 57..154 / Y: 43..133 (coordenadas virtuales)
        TextButton btnSalon3 = new TextButton("", skin);
        btnSalon3.setBounds(57, 43, 154 - 57, 133 - 43); // w=97, h=90
        btnSalon3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON4]", "Volver a PantallaSalon3");
                juego.setScreen(new PantallaSalon3(juego));
            }
        });
        stage.addActor(btnSalon3);

        // Botón IR A SALÓN 5 — X: 1740..1835 / Y: 43..133
        TextButton btnSalon5 = new TextButton("", skin);
        btnSalon5.setBounds(1740, 43, 1835 - 1740, 133 - 43); // w=95, h=90
        btnSalon5.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON4]", "Ir a PantallaSalon5");
                juego.setScreen(new PantallaSalon5(juego));
            }
        });
        stage.addActor(btnSalon5);
    }

    @Override
    public void render(float delta) {
        // Proyectar el batch con la cámara del viewport (clave para el reescalado)
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, VW, VH); // dibujar en el mundo virtual
        juego.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // Reescalar todo (fondo y botones) al nuevo tamaño real
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
