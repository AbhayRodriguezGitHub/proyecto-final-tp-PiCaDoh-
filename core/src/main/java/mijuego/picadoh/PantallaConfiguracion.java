package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class PantallaConfiguracion implements Screen {
    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private SelectBox<String> resolucionBox;
    private Image fondoImage;
    private Label ticLabel;
    private boolean modoVentana = false;

    public PantallaConfiguracion(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/MENUCONFIGURACION.png"));

        // Cursor personalizado
        juego.setCursorPersonalizado();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        fondoImage = new Image(fondo);
        fondoImage.setFillParent(true);
        stage.addActor(fondoImage);

        // ESCALA dinámica
        float baseWidth = 1920f;
        float baseHeight = 1080f;
        float scaleX = Gdx.graphics.getWidth() / baseWidth;
        float scaleY = Gdx.graphics.getHeight() / baseHeight;

        // SKIN y fuente
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        font.getData().setScale(scaleX);  // Escala el texto
        font.setUseIntegerPositions(false); // Mejora suavizado
        skin.add("default-font", font);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // TIC ✔ modo ventana
        ticLabel = new Label("✔", skin);
        ticLabel.setFontScale(2 * scaleX);
        ticLabel.setPosition(1070 * scaleX, 800 * scaleY);
        ticLabel.setVisible(false);
        stage.addActor(ticLabel);

        // BOTÓN MODO VENTANA
        ImageButton botonModoVentana = crearBotonInvisible(1045 * scaleX, 906 * scaleY, 80 * scaleX, 80 * scaleY);
        botonModoVentana.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                modoVentana = !modoVentana;
                ticLabel.setVisible(modoVentana);

                if (modoVentana) {
                    Gdx.graphics.setWindowedMode(1920, 1080);
                } else {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                }

                stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
                juego.setCursorPersonalizado();
            }
        });
        stage.addActor(botonModoVentana);

        // SELECTBOX RESOLUCIONES
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = Color.BLACK;

        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(Color.LIGHT_GRAY);
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        selectBoxStyle.background = new Image(bgTexture).getDrawable();
        selectBoxStyle.backgroundOpen = selectBoxStyle.background;
        selectBoxStyle.backgroundOver = selectBoxStyle.background;
        selectBoxStyle.scrollStyle = new ScrollPane.ScrollPaneStyle();
        selectBoxStyle.listStyle = new List.ListStyle(font, Color.BLACK, Color.DARK_GRAY, selectBoxStyle.background);

        skin.add("selectbox-default", selectBoxStyle);

        resolucionBox = new SelectBox<>(skin, "selectbox-default");
        resolucionBox.setItems("1920x1080", "800x600", "1024x768", "1280x720", "1366x768", "1920x1020");
        resolucionBox.setSelected("1920x1080");
        resolucionBox.setSize(200 * scaleX, 40 * scaleY);
        resolucionBox.setPosition(1365 * scaleX, 610 * scaleY);
        resolucionBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String seleccion = resolucionBox.getSelected();
                String[] partes = seleccion.split("x");
                int ancho = Integer.parseInt(partes[0]);
                int alto = Integer.parseInt(partes[1]);

                Gdx.graphics.setWindowedMode(ancho, alto);
                stage.getViewport().update(ancho, alto, true);
                juego.setCursorPersonalizado();
            }
        });
        stage.addActor(resolucionBox);

        // BOTÓN VOLVER
        ImageButton botonVolver = crearBotonInvisible(56 * scaleX, 70 * scaleY, 80 * scaleX, 80 * scaleY);
        botonVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaMenu(juego));
            }
        });
        stage.addActor(botonVolver);
    }

    private ImageButton crearBotonInvisible(float x, float y, float width, float height) {
        Pixmap pixmap = new Pixmap((int) width, (int) height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); // Transparente
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        Drawable drawable = new Image(texture).getDrawable();
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
