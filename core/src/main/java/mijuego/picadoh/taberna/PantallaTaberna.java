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

    // ─────────────────────────────────────────────────────────
    // RADIO DE TABERNA (persistente entre instancias de pantalla)
    // ─────────────────────────────────────────────────────────
    private static Music[] pistas;              // se crean una sola vez
    private static int indicePistaActual = 0;   // índice actual
    private static Music musicaActual;          // pista actualmente sonando

    // Para NO cortar la música al ir a Salón
    private boolean mantenerMusicaAlSalir = false;

    public PantallaTaberna(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Apagar cualquier música global del Principal (menús, etc.)
        juego.detenerMusicaActual();

        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/taberna/TABERNA.png"));

        // ─────────────────────────────────────────────────────────
        // Inicializar la radio una sola vez (reutilizable)
        // ─────────────────────────────────────────────────────────
        if (pistas == null) {
            pistas = new Music[10];
            for (int i = 0; i < pistas.length; i++) {
                final int idx = i; // capturar índice correcto para el listener
                pistas[i] = Gdx.audio.newMusic(Gdx.files.absolute("lwjgl3/assets/taberna/TABERNA" + (i + 1) + ".mp3"));
                pistas[i].setOnCompletionListener(music -> reproducirPista((idx + 1) % pistas.length));
            }
        }

        // Si no hay música sonando aún, arrancar la actual; si ya suena, NO tocarla.
        if (musicaActual == null || !musicaActual.isPlaying()) {
            reproducirPista(indicePistaActual);
        }

        // ─────────────────────────────────────────────────────────
        // UI invisible
        // ─────────────────────────────────────────────────────────
        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("default", estiloInvisible);

        // Botón atrás
        TextButton btnAtras = new TextButton("", skin);
        btnAtras.setBounds(26, 940, 150 - 26, 1064 - 940);
        btnAtras.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                detenerMusicaTaberna(); // acá sí paramos al salir al Menú
                System.out.println("[TABERNA] Volviendo al menú principal...");
                juego.setScreen(new PantallaMenu(juego));
            }
        });
        stage.addActor(btnAtras);

        // Botón radio (siguiente pista)
        TextButton btnRadio = new TextButton("", skin);
        btnRadio.setBounds(1510, 374, 1684 - 1510, 579 - 374);
        btnRadio.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                siguientePista();
                System.out.println("[TABERNA] Radio: pista ahora -> " + (indicePistaActual + 1));
            }
        });
        stage.addActor(btnRadio);

        // Botón invisible para ir al SALÓN 1 (X: 1410..1548, Y: 117..255)
        TextButton btnSalon1 = new TextButton("", skin);
        btnSalon1.setBounds(1410, 117, 1548 - 1410, 255 - 117);
        btnSalon1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // No cortar la música: la dejamos sonando
                mantenerMusicaAlSalir = true;
                System.out.println("[TABERNA] Ir a PantallaSalon1 (manteniendo música)");
                juego.setScreen(new PantallaSalon1(juego));
            }
        });
        stage.addActor(btnSalon1);
    }

    // ─────────────────────────────────────────────────────────
    // Reproducir pista: detiene SOLO la actual y arranca la nueva
    // (sin tocar objetos Music de otras instancias ni recrearlos)
    // ─────────────────────────────────────────────────────────
    private void reproducirPista(int indice) {
        if (pistas == null || pistas.length == 0) return;

        // Detener solo la actual (si la hay)
        if (musicaActual != null && musicaActual.isPlaying()) {
            musicaActual.stop();
        }

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
        // Detener SOLO la que está sonando en la radio de Taberna
        if (musicaActual != null && musicaActual.isPlaying()) {
            musicaActual.stop();
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
        // Si vamos a Salón, dejamos que la música siga sonando.
        // Si NO (por ejemplo a Menú), ya la paramos en el botón atrás.
        if (!mantenerMusicaAlSalir) {
            detenerMusicaTaberna();
        }
        mantenerMusicaAlSalir = false;

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
        // Importante: NO dispose() de pistas/musicaActual aquí,
        // porque la radio es estática y debe persistir.
    }

    @Override
    public void dispose() {
        // No liberar pistas aquí para que la radio persista entre pantallas.
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (fondo != null) fondo.dispose();
    }
}
