package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
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

        // Cursor personalizado
        Pixmap pixmap = new Pixmap(Gdx.files.absolute("lwjgl3/assets/ui/CURSOR.png"));
        int xHotspot = pixmap.getWidth() / 2;
        int yHotspot = pixmap.getHeight() / 6;
        Cursor cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
        Gdx.graphics.setCursor(cursor);
        pixmap.dispose();

        // Stage + botón invisible sobre la tuerquita
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin();
        BitmapFont font = new BitmapFont(); // Fuente default
        skin.add("default", new TextButton.TextButtonStyle(null, null, null, font));

        TextButton btnConfig = new TextButton("", skin);
        btnConfig.setBounds(20, 20, 80, 80); // Aproximado para cubrir la tuerquita

        btnConfig.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(">>> CONFIGURACIÓN clickeada!");
                juego.setScreen(new PantallaConfiguracion(juego));
            }
        });

        stage.addActor(btnConfig);
    }

    @Override
    public void render(float delta) {
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        juego.batch.end();

        stage.act();
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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
