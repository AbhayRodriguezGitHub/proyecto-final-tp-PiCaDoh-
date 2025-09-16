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

public class PantallaSalon1 implements Screen {

    private final Principal juego;
    private Texture fondo;

    private Stage stage;
    private Skin skin;

    public PantallaSalon1(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {

        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }


        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/salon/SALON1.png"));


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

        TextButton btnVolverTaberna = new TextButton("", skin);
        btnVolverTaberna.setBounds(256, 311, 356 - 256, 409 - 311); // w=100, h=98
        btnVolverTaberna.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Al volver a Taberna, no tocamos la música desde aquí.
                Gdx.app.log("[SALON1]", "Volviendo a PantallaTaberna");
                Gdx.app.postRunnable(() -> {
                    juego.setScreen(new PantallaTaberna(juego));
                });
            }
        });
        stage.addActor(btnVolverTaberna);

        TextButton btnIrSalon2 = new TextButton("", skin);
        btnIrSalon2.setBounds(1539, 317, 1635 - 1539, 406 - 317); // w=96, h=89
        btnIrSalon2.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[SALON1]", "Ir a PantallaSalon2 (manteniendo música)");
                Gdx.app.postRunnable(() -> juego.setScreen(new PantallaSalon2(juego)));
            }
        });
        stage.addActor(btnIrSalon2);
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
    @Override public void hide() {
        // No detener música
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
        if (stage != null) { stage.dispose(); stage = null; }
        if (skin  != null) { skin.dispose();  skin  = null; }
        if (fondo != null) { fondo.dispose(); fondo = null; }
    }
}
