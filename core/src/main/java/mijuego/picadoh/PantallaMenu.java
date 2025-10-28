package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.InputAdapter;
import com.google.gson.JsonObject;
import mijuego.picadoh.taberna.PantallaTaberna;
import mijuego.red.ClienteLAN;

/**
 * Pantalla principal del menú de Pi-Ca-Doh!
 * Maneja opciones de configuración, taberna, salida y conexión LAN.
 */
public class PantallaMenu implements Screen {

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Skin skin;

    private OrthographicCamera camara;
    private Viewport viewport;

    private boolean waitingForMatch = false;
    private Texture waitTexture = null;
    private BitmapFont fontWait = null;
    private com.badlogic.gdx.InputProcessor previousInputProcessor = null;

    public PantallaMenu(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        // Fondo del menú
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/FONDOMENU1.png"));

        // Cursor
        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        // Música
        juego.reproducirMusica();

        // Cámara y viewport
        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        // Stage y UI
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("invisible", estiloInvisible);

        // Botón Configuración
        TextButton btnConfig = new TextButton("", skin, "invisible");
        btnConfig.setBounds(20, 20, 80, 80);
        btnConfig.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaConfiguracion(juego));
            }
        });
        stage.addActor(btnConfig);

        // Botón BATALLA
        TextButton btnBatalla = new TextButton("", skin, "invisible");
        btnBatalla.setBounds(320, 750, 640, 180);
        btnBatalla.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                iniciarEsperaMatch();
            }
        });
        stage.addActor(btnBatalla);

        // Botón Taberna
        TextButton btnTaberna = new TextButton("", skin, "invisible");
        btnTaberna.setBounds(325, 475, 958 - 325, 650 - 475);
        btnTaberna.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.detenerMusicaActual();
                juego.setScreen(new PantallaTaberna(juego));
            }
        });
        stage.addActor(btnTaberna);

        // Botón SALIR
        TextButton btnSalir = new TextButton("", skin, "invisible");
        btnSalir.setBounds(320, 180, 640, 180);
        btnSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        stage.addActor(btnSalir);

        // Precargar textura y fuente de espera
        try {
            waitTexture = new Texture(Gdx.files.absolute("lwjgl3/assets/lan/ESPERA.png"));
        } catch (Exception e) {
            System.out.println("[PantallaMenu] No se pudo cargar ESPERA.png: " + e.getMessage());
            waitTexture = null;
        }
        fontWait = new BitmapFont();
    }

    /** Inicia el proceso de conexión y espera de emparejamiento LAN */
    private void iniciarEsperaMatch() {
        if (waitingForMatch) return;
        waitingForMatch = true;

        previousInputProcessor = Gdx.input.getInputProcessor();

        // Bloquear input mientras esperamos (ESC para cancelar)
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    cancelarEsperaMatch();
                    return true;
                }
                return true; // consumir todo
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return true; // consumir clics
            }
        });

        try {
            // Crear cliente LAN (si no existe) con referencia al juego para que pueda cambiar pantallas
            if (juego.clienteLAN == null) {
                juego.clienteLAN = new ClienteLAN(juego, "127.0.0.1", 5000);
            }

            // Conectar solo si hace falta
            if (!juego.clienteLAN.isConnected()) {
                boolean ok = juego.clienteLAN.connect();
                if (!ok) {
                    System.out.println("[PantallaMenu] No se pudo conectar al servidor LAN.");
                    cancelarEsperaMatch();
                    return;
                }
            }

            // Listener adicional por si querés reaccionar aquí
            juego.clienteLAN.setOnMessage(json -> {
                try {
                    if (json == null || !json.has("type")) return;
                    String type = json.get("type").getAsString();
                    if ("MATCHED".equalsIgnoreCase(type)) {
                        System.out.println("[PantallaMenu] MATCHED recibido.");
                        Gdx.app.postRunnable(() -> {
                            waitingForMatch = false;
                            restaurarInputProcessor();
                            // Tu PantallaSeleccionTropa actual recibe solo (Principal)
                            juego.setScreen(new PantallaSeleccionTropa(juego));
                        });
                    }
                } catch (Exception e) {
                    System.out.println("[PantallaMenu] Error manejando mensaje: " + e.getMessage());
                }
            });

            // Enviar JOIN_QUEUE
            juego.clienteLAN.joinQueue();
            System.out.println("[PantallaMenu] JOIN_QUEUE enviado. Esperando rival...");

        } catch (Throwable t) {
            System.out.println("[PantallaMenu] Error al conectar cliente LAN: " + t.getMessage());
            cancelarEsperaMatch();
        }
    }

    /** Cancela la espera y restablece los controles */
    private void cancelarEsperaMatch() {
        waitingForMatch = false;
        restaurarInputProcessor();

        try {
            if (juego.clienteLAN != null) {
                JsonObject o = new JsonObject();
                o.addProperty("type", "LEAVE_QUEUE"); // el servidor puede ignorarlo si aún no lo implementaste
                juego.clienteLAN.sendJson(o);
            }
        } catch (Exception e) {
            System.out.println("[PantallaMenu] Error enviando LEAVE_QUEUE: " + e.getMessage());
        }
    }

    private void restaurarInputProcessor() {
        if (previousInputProcessor != null) {
            Gdx.input.setInputProcessor(previousInputProcessor);
            previousInputProcessor = null;
        } else if (stage != null) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void render(float delta) {
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, VW, VH);

        // Modo espera
        if (waitingForMatch) {
            if (waitTexture != null) {
                float tx = (VW - waitTexture.getWidth()) / 2f;
                float ty = (VH - waitTexture.getHeight()) / 2f;
                juego.batch.draw(waitTexture, tx, ty);
            } else if (fontWait != null) {
                fontWait.draw(juego.batch, "ESPERANDO RIVAL... (ESC para cancelar)", VW / 2f - 200, VH / 2f);
            }
        }

        juego.batch.end();

        if (!waitingForMatch) {
            stage.act(delta);
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        juego.aplicarCursor();
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        // Restaurar input si salimos en medio de la espera
        restaurarInputProcessor();

        if (stage != null) { stage.clear(); stage.dispose(); stage = null; }
        if (skin  != null) { skin.dispose();  skin  = null; }
        if (fondo != null) { fondo.dispose(); fondo = null; }
        if (waitTexture != null) { waitTexture.dispose(); waitTexture = null; }
        if (fontWait != null) { fontWait.dispose(); fontWait = null; }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin  != null) skin.dispose();
        if (fondo != null) fondo.dispose();
        if (waitTexture != null) waitTexture.dispose();
        if (fontWait != null) fontWait.dispose();
    }
}
