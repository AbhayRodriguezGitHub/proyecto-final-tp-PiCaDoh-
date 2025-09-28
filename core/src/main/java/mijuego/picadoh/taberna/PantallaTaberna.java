package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
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
import mijuego.picadoh.PantallaMenu;

public class PantallaTaberna implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;


    private static Music[] pistas;
    private static int indicePistaActual = 0;
    private static Music musicaActual;

    private boolean mantenerMusicaAlSalir = false;

    public PantallaTaberna(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/taberna/TABERNA.png"));

        if (pistas == null) {
            pistas = new Music[10];
            for (int i = 0; i < pistas.length; i++) {
                final int idx = i;
                pistas[i] = Gdx.audio.newMusic(Gdx.files.absolute("lwjgl3/assets/taberna/TABERNA" + (i + 1) + ".mp3"));
                pistas[i].setOnCompletionListener(music -> reproducirPista((idx + 1) % pistas.length));
            }
        }
        if (musicaActual == null || !musicaActual.isPlaying()) {
            reproducirPista(indicePistaActual);
        }

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("default", estiloInvisible);

        TextButton btnAtras = new TextButton("", skin);
        btnAtras.setBounds(26, 940, 150 - 26, 1064 - 940);
        btnAtras.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                detenerMusicaTaberna();
                juego.setScreen(new PantallaMenu(juego));
            }
        });
        stage.addActor(btnAtras);

        TextButton btnRadio = new TextButton("", skin);
        btnRadio.setBounds(1510, 374, 1684 - 1510, 579 - 374);
        btnRadio.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                siguientePista();
                System.out.println("[TABERNA] Radio: pista ahora -> " + (indicePistaActual + 1));
            }
        });
        stage.addActor(btnRadio);

        TextButton btnSalon1 = new TextButton("", skin);
        btnSalon1.setBounds(1410, 117, 1548 - 1410, 255 - 117);
        btnSalon1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                mantenerMusicaAlSalir = true;
                juego.setScreen(new PantallaSalon1(juego));
            }
        });
        stage.addActor(btnSalon1);

        TextButton btnNON = new TextButton("", skin);
        btnNON.setBounds(379, 115, 514 - 379, 527 - 115);
        btnNON.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                mantenerMusicaAlSalir = true;
                juego.setScreen(new PantallaPresentacionCarta(
                    juego,
                    PantallaTaberna.this,
                    "lwjgl3/assets/menus/NON.png"
                ));
            }
        });
        stage.addActor(btnNON);

        TextButton btnL1 = new TextButton("", skin);
        btnL1.setBounds(890, 115, 1030 - 890, 255 - 115);
        btnL1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                mantenerMusicaAlSalir = true;
                juego.setScreen(new PantallaPresentacionCarta(
                    juego,
                    PantallaTaberna.this,
                    "lwjgl3/assets/libro/L1.png"
                ));
            }
        });
        stage.addActor(btnL1);
    }

    private void reproducirPista(int indice) {
        if (musicaActual != null && musicaActual.isPlaying()) musicaActual.stop();
        indicePistaActual = indice;
        musicaActual = pistas[indicePistaActual];
        if (musicaActual != null) {
            musicaActual.setVolume(juego.getVolumenMusica());
            musicaActual.play();
        }
    }

    private void siguientePista() {
        int siguiente = (indicePistaActual + 1) % pistas.length;
        reproducirPista(siguiente);
    }

    private void detenerMusicaTaberna() {
        if (musicaActual != null && musicaActual.isPlaying()) {
            musicaActual.stop();
        }
    }

    @Override
    public void render(float delta) {
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, VW, VH);
        juego.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        if (!mantenerMusicaAlSalir) detenerMusicaTaberna();
        mantenerMusicaAlSalir = false;

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
