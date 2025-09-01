package mijuego.picadoh.taberna;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
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
import mijuego.picadoh.PantallaMenu;

public class PantallaTaberna implements Screen {

    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Skin skin;

    private Music[] pistas;
    private int indicePistaActual = 0;

    public PantallaTaberna(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        juego.detenerMusicaActual();
        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/taberna/TABERNA.png"));


        pistas = new Music[10];
        for (int i = 0; i < pistas.length; i++) {
            pistas[i] = Gdx.audio.newMusic(Gdx.files.absolute("lwjgl3/assets/taberna/TABERNA" + (i + 1) + ".mp3"));
            final int siguiente = (i + 1) % pistas.length;
            pistas[i].setOnCompletionListener(music -> reproducirPista(siguiente));
        }

        reproducirPista(0);

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
            @Override
            public void clicked(InputEvent event, float x, float y) {
                detenerMusicaTaberna();
                System.out.println("[TABERNA] Volviendo al menú principal...");
                juego.setScreen(new PantallaMenu(juego));
            }
        });
        stage.addActor(btnAtras);

        TextButton btnRadio = new TextButton("", skin);
        btnRadio.setBounds(1510, 374, 1684 - 1510, 579 - 374);
        btnRadio.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                siguientePista();
                System.out.println("[TABERNA] Radio: pista ahora -> " + (indicePistaActual + 1));
            }
        });
        stage.addActor(btnRadio);
    }

    private void reproducirPista(int indice) {
        detenerMusicaTaberna();
        indicePistaActual = indice;
        if (pistas[indicePistaActual] != null) {
            pistas[indicePistaActual].setVolume(juego.getVolumenMusica());
            pistas[indicePistaActual].play();
        }
    }

    private void siguientePista() {
        int siguiente = (indicePistaActual + 1) % pistas.length;
        reproducirPista(siguiente);
    }

    private void detenerMusicaTaberna() {
        if (pistas != null) {
            for (Music m : pistas) {
                if (m != null && m.isPlaying()) m.stop();
            }
        }
    }

    @Override
    public void render(float delta) {
        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, 1920, 1080);
        juego.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        detenerMusicaTaberna();
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
        detenerMusicaTaberna();
        if (pistas != null) {
            for (Music m : pistas) {
                if (m != null) m.dispose();
            }
        }
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (fondo != null) fondo.dispose();
    }
}
