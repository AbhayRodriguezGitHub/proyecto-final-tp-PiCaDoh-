package mijuego.picadoh;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Principal extends Game {
    public SpriteBatch batch;

    // NUEVO: Etapa y etiqueta para mostrar coordenadas globales
    private Stage coordenadasStage;
    private Label coordenadasLabel;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setCursorPersonalizado();
        setupVisorDeCoordenadas(); // NUEVO
        setScreen(new PantallaMenu(this));
    }

    private void setCursorPersonalizado() {
        Pixmap pixmap = new Pixmap(Gdx.files.absolute("lwjgl3/assets/ui/CURSOR.png"));
        int xHotspot = pixmap.getWidth() / 2;
        int yHotspot = pixmap.getHeight() / 6;

        Cursor cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
        Gdx.graphics.setCursor(cursor);
        pixmap.dispose();
    }

    // NUEVO: Configura el visor de coordenadas global
    private void setupVisorDeCoordenadas() {
        coordenadasStage = new Stage(new ScreenViewport());

        Skin skin = new Skin();
        BitmapFont font = new BitmapFont(); // Fuente por defecto
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        coordenadasLabel = new Label("X: 0 | Y: 0", skin);
        coordenadasLabel.setPosition(10, Gdx.graphics.getHeight() - 20);
        coordenadasStage.addActor(coordenadasLabel);
    }

    @Override
    public void render() {
        // Primero renderizamos la lógica normal (pantallas, juego, etc.)
        super.render();

        // Luego actualizamos y dibujamos las coordenadas encima
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        coordenadasLabel.setText("X: " + mouseX + " | Y: " + mouseY);

        coordenadasStage.act();
        coordenadasStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (coordenadasStage != null)
            coordenadasStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        coordenadasStage.dispose(); // NUEVO
        super.dispose();
    }
}
