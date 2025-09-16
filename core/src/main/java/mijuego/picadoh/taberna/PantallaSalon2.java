package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import mijuego.picadoh.Principal;

public class PantallaSalon2 implements Screen {

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

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

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON2.png"));

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


        TextButton btnSalon1 = new TextButton("", skin);
        btnSalon1.setBounds(260, 299, 360 - 260, 390 - 299); // w=100, h=91
        btnSalon1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON2]", "Volviendo a PantallaSalon1");
                juego.setScreen(new PantallaSalon1(juego));
            }
        });
        stage.addActor(btnSalon1);

        TextButton btnSalon3 = new TextButton("", skin);
        btnSalon3.setBounds(1532, 302, 1639 - 1532, 394 - 302); // w=107, h=92
        btnSalon3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON2]", "Ir a PantallaSalon3");
                juego.setScreen(new PantallaSalon3(juego));
            }
        });
        stage.addActor(btnSalon3);
    }

    @Override
    public void render(float delta) {
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        juego.batch.end();

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    @Override public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) { stage.clear(); stage.dispose(); stage = null; }
        if (skin != null) { skin.dispose(); skin = null; }
        if (fondo != null) { fondo.dispose(); fondo = null; }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (fondo != null) fondo.dispose();
    }
}
