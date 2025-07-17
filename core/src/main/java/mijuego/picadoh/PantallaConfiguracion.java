package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.Graphics.DisplayMode;

public class PantallaConfiguracion implements Screen {
    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Image fondoImage;
    private Label ticLabel;
    private boolean modoVentana = false;

    private final float VIRTUAL_WIDTH = 1920f;
    private final float VIRTUAL_HEIGHT = 1080f;
    private Viewport viewport;

    public PantallaConfiguracion(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/MENUCONFIGURACION.png"));

        // 🎵 Reproducir música si aún no está sonando
        juego.reproducirMusica();

        viewport = new ScalingViewport(Scaling.stretch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        fondoImage = new Image(fondo);
        fondoImage.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(fondoImage);

        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        ticLabel = new Label("X", skin);
        ticLabel.setFontScale(8f);
        ticLabel.setPosition(1032, 910);
        ticLabel.setVisible(false);
        stage.addActor(ticLabel);

        ImageButton botonModoVentana = crearBotonInvisible(1045, 906, 150, 150);
        botonModoVentana.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                modoVentana = !modoVentana;
                ticLabel.setVisible(modoVentana);

                if (modoVentana) {
                    Gdx.graphics.setWindowedMode(1024, 768);
                } else {
                    for (DisplayMode mode : Gdx.graphics.getDisplayModes()) {
                        if (mode.width == 1920 && mode.height == 1080) {
                            Gdx.graphics.setFullscreenMode(mode);
                            break;
                        }
                    }
                }

                // Forzar actualización del viewport
                resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                juego.aplicarCursor(); // Reaplica el cursor
            }
        });
        stage.addActor(botonModoVentana);

        ImageButton botonVolver = crearBotonInvisible(56, 70, 80, 80);
        botonVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaMenu(juego));
            }
        });
        stage.addActor(botonVolver);

        ImageButton botonCursorSistema = crearBotonInvisible(1138, 237, 193, 182);
        botonCursorSistema.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setCursorPersonalizadoUsado(false);
            }
        });
        stage.addActor(botonCursorSistema);

        ImageButton botonCursorRubberhose = crearBotonInvisible(1557, 259, 193, 182);
        botonCursorRubberhose.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setCursorPersonalizadoUsado(true);
            }
        });
        stage.addActor(botonCursorRubberhose);
    }

    private ImageButton crearBotonInvisible(float x, float y, float width, float height) {
        Pixmap pixmap = new Pixmap((int) width, (int) height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); // Color transparente
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        Drawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton boton = new ImageButton(drawable);
        boton.setBounds(x, y, width, height);
        return boton;
    }

    @Override
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        juego.aplicarCursor(); // <-- Esto reaplica el cursor escalado
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
