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

public class PantallaMenu implements Screen {
    private final Principal juego;
    private Texture fondo;
    private Stage stage;

    public PantallaMenu(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/FONDOMENU1.png"));

        // ⚠️ Aplicar el cursor según la preferencia del usuario
        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        // 🎵 Reproducir música si no está sonando
        juego.reproducirMusica();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", new TextButton.TextButtonStyle(null, null, null, font));

        // Botón invisible sobre la tuerquita (configuración)
        TextButton btnConfig = new TextButton("", skin);
        btnConfig.setBounds(20, 20, 80, 80);
        btnConfig.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(">>> CONFIGURACIÓN clickeada!");
                juego.setScreen(new PantallaConfiguracion(juego));
            }
        });
        stage.addActor(btnConfig);

        // Botón invisible para SALIR del juego
        TextButton btnSalir = new TextButton("", skin);
        btnSalir.setBounds(400, 150, 500, 200); // Tamaño y posición corregidos
        btnSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(">>> SALIR clickeado! Cerrando juego...");
                Gdx.app.exit();
            }
        });
        stage.addActor(btnSalir);
    }

    @Override
    public void render(float delta) {
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        juego.batch.end();

        stage.act();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        juego.aplicarCursor();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        fondo.dispose();
        stage.dispose();
    }
}
