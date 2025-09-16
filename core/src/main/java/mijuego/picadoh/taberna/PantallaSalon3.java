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

    private static final float VW = 1920f;  // ancho virtual
    private static final float VH = 1080f;  // alto  virtual

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    // Cámara + Viewport (para que fondo y botones se reescalen juntos)
    private OrthographicCamera camara;
    private Viewport viewport;

    public PantallaSalon3(Principal juego) {
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

        // Cámara + Viewport (misma lógica que Taberna/Salón1/Salón2)
        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        // Stage usando el MISMO viewport
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        // Fondo
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON3.png"));

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

        // Botón VOLVER A SALÓN 2 — X: 57..154 / Y: 43..133 (coordenadas virtuales)
        TextButton btnSalon2 = new TextButton("", skin);
        btnSalon2.setBounds(57, 43, 154 - 57, 133 - 43); // w=97, h=90
        btnSalon2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON3]", "Volver a PantallaSalon2");
                juego.setScreen(new PantallaSalon2(juego));
            }
        });
        stage.addActor(btnSalon2);

        // Botón IR A SALÓN 4 — X: 1740..1835 / Y: 43..133
        TextButton btnSalon4 = new TextButton("", skin);
        btnSalon4.setBounds(1740, 43, 1835 - 1740, 133 - 43); // w=95, h=90
        btnSalon4.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON3]", "Ir a PantallaSalon4");
                juego.setScreen(new PantallaSalon4(juego));
            }
        });
        stage.addActor(btnSalon4);
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
