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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class PantallaConfiguracion implements Screen {
    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Image fondoImage;
    private Label ticLabel;
    private final float VIRTUAL_WIDTH = 1920f;
    private final float VIRTUAL_HEIGHT = 1080f;
    private Viewport viewport;

    public PantallaConfiguracion(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/MENUCONFIGURACION.png"));
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
        ticLabel.setVisible(juego.isModoVentana());
        stage.addActor(ticLabel);
        ImageButton botonModoVentana = crearBotonInvisible(1045, 906, 150, 150);
        botonModoVentana.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean nuevoModo = !juego.isModoVentana();
                juego.setModoVentana(nuevoModo);
                ticLabel.setVisible(nuevoModo);
                if (nuevoModo) {
                    Gdx.graphics.setWindowedMode(1024, 768);
                } else {
                    for (DisplayMode mode : Gdx.graphics.getDisplayModes()) {
                        if (mode.width == 1920 && mode.height == 1080) {
                            Gdx.graphics.setFullscreenMode(mode);
                            break;
                        }
                    }
                }
                resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                juego.aplicarCursor();
            }
        });
        stage.addActor(botonModoVentana);
        ImageButton botonVolver = crearBotonInvisible(56, 70, 80, 80);
        botonVolver.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaMenu(juego));
            }
        });
        stage.addActor(botonVolver);
        ImageButton botonCursorSistema = crearBotonInvisible(1138, 237, 193, 182);
        botonCursorSistema.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setCursorPersonalizadoUsado(false);
            }
        });
        stage.addActor(botonCursorSistema);
        ImageButton botonCursorRubberhose = crearBotonInvisible(1557, 259, 193, 182);
        botonCursorRubberhose.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setCursorPersonalizadoUsado(true);
            }
        });
        stage.addActor(botonCursorRubberhose);
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        Pixmap fondoBarra = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
        fondoBarra.setColor(Color.BLACK);
        fondoBarra.fill();
        sliderStyle.background = new TextureRegionDrawable(new TextureRegion(new Texture(fondoBarra)));
        fondoBarra.dispose();
        Pixmap knob = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        knob.setColor(Color.BLACK);
        knob.fillCircle(15, 15, 15);
        sliderStyle.knob = new TextureRegionDrawable(new TextureRegion(new Texture(knob)));
        knob.dispose();
        Slider slider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        slider.setValue(juego.getVolumenMusica());
        slider.setBounds(1252, 630, 471, 30);
        slider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                juego.setVolumenMusica(slider.getValue());
            }
        });
        stage.addActor(slider);
        ImageButton botonManual = crearBotonInvisible(85, 845, 856 - 85, 986 - 845);
        botonManual.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaManual(juego, PantallaConfiguracion.this));
            }
        });
        stage.addActor(botonManual);
    }

    private ImageButton crearBotonInvisible(float x, float y, float width, float height) {
        Pixmap pixmap = new Pixmap((int) width, (int) height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
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
        juego.aplicarCursor();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        fondo.dispose();
        stage.dispose();
    }
}
