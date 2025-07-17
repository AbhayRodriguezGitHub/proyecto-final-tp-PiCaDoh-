package mijuego.picadoh;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Principal extends Game {
    public SpriteBatch batch;

    private Stage coordenadasStage;
    private Label coordenadasLabel;

    private boolean cursorPersonalizadoUsado = true;

    // 🎵 Música principal global
    private Music musicaMenu;

    // 🔊 Volumen global (0.0f a 1.0f)
    private float volumenMusica = 1f;

    private boolean modoVentana = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        aplicarCursor();
        setupVisorDeCoordenadas();
        cargarMusica();
        reproducirMusica();
        setScreen(new PantallaMenu(this));
    }

    // Cargar música una sola vez
    private void cargarMusica() {
        musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/menus/musica_menu.mp3"));
        musicaMenu.setLooping(true);
        musicaMenu.setVolume(volumenMusica); // Aplica el volumen actual
    }

    public void reproducirMusica() {
        if (musicaMenu != null && !musicaMenu.isPlaying()) {
            musicaMenu.play();
        }
    }

    public void pausarMusica() {
        if (musicaMenu != null && musicaMenu.isPlaying()) {
            musicaMenu.pause();
        }
    }

    public void detenerMusica() {
        if (musicaMenu != null) {
            musicaMenu.stop();
        }
    }

    public float getVolumenMusica() {
        return volumenMusica;
    }

    public void setVolumenMusica(float volumen) {
        this.volumenMusica = MathUtils.clamp(volumen, 0f, 1f);
        if (musicaMenu != null) {
            musicaMenu.setVolume(this.volumenMusica);
        }
    }

    public void aplicarCursor() {
        if (cursorPersonalizadoUsado) {
            setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }
    }

    public void setCursorPersonalizado() {
        Pixmap original = new Pixmap(Gdx.files.absolute("lwjgl3/assets/ui/CURSOR.png"));

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        float refWidth = 1920f;
        float refHeight = 1080f;

        float scale = (screenWidth / refWidth + screenHeight / refHeight) / 2f;

        int newWidth = nextPowerOfTwo((int)(original.getWidth() * scale));
        int newHeight = nextPowerOfTwo((int)(original.getHeight() * scale));

        Pixmap scaled = new Pixmap(newWidth, newHeight, Pixmap.Format.RGBA8888);
        scaled.drawPixmap(original,
            0, 0, original.getWidth(), original.getHeight(),
            0, 0, newWidth, newHeight
        );

        int xHotspot = newWidth / 2;
        int yHotspot = newHeight / 6;

        Cursor cursor = Gdx.graphics.newCursor(scaled, xHotspot, yHotspot);
        Gdx.graphics.setCursor(cursor);

        original.dispose();
        scaled.dispose();
    }

    public boolean isCursorPersonalizadoUsado() {
        return cursorPersonalizadoUsado;
    }

    public void setCursorPersonalizadoUsado(boolean usado) {
        this.cursorPersonalizadoUsado = usado;
        aplicarCursor();
    }

    private int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        int power = 1;
        while (power < n) power <<= 1;
        return power;
    }

    public boolean isModoVentana() {
        return modoVentana;
    }

    public void setModoVentana(boolean modoVentana) {
        this.modoVentana = modoVentana;
    }

    private void setupVisorDeCoordenadas() {
        coordenadasStage = new Stage(new ScreenViewport());

        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        coordenadasLabel = new Label("X: 0 | Y: 0", skin);
        coordenadasLabel.setPosition(10, Gdx.graphics.getHeight() - 20);
        coordenadasStage.addActor(coordenadasLabel);
    }

    @Override
    public void render() {
        super.render();

        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        coordenadasLabel.setText("X: " + mouseX + " | Y: " + mouseY);

        coordenadasStage.act();
        coordenadasStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (coordenadasStage != null) {
            coordenadasStage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        coordenadasStage.dispose();
        if (musicaMenu != null) {
            musicaMenu.dispose();
        }
        super.dispose();
    }
}
