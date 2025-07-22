package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class PantallaMenu implements Screen {
    private final Principal juego;
    private Texture fondo;
    private Stage stage;
    private Skin skin;

    public PantallaMenu(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/FONDOMENU1.png"));

        // Aplica el cursor correcto
        if (juego.isCursorPersonalizadoUsado()) {
            juego.setCursorPersonalizado();
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        // Inicia la música si no está sonando
        juego.reproducirMusica();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Skin y fuente para botones invisibles
        skin = new Skin();
        BitmapFont font = new BitmapFont(); // Fuente por defecto
        skin.add("default", font);

        TextButton.TextButtonStyle estiloInvisible = new TextButton.TextButtonStyle();
        estiloInvisible.font = font;
        estiloInvisible.up = null;
        estiloInvisible.down = null;
        estiloInvisible.over = null;
        skin.add("default", estiloInvisible);

        // 🔧 Botón invisible de CONFIGURACIÓN (tuerquita arriba izquierda)
        TextButton btnConfig = new TextButton("", skin);
        btnConfig.setBounds(20, 20, 80, 80); // Coordenadas originales
        btnConfig.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(">>> CONFIGURACIÓN clickeada!");
                juego.setScreen(new PantallaConfiguracion(juego));
            }
        });
        stage.addActor(btnConfig);

        // ❌ Botón invisible para SALIR (parte media-inferior)
        TextButton btnSalir = new TextButton("", skin);
        btnSalir.setBounds(400, 150, 500, 200); // Coordenadas originales
        btnSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(">>> SALIR clickeado! Cerrando juego...");
                Gdx.app.exit();
            }
        });
        stage.addActor(btnSalir);

        // 🔥 Botón invisible para ir a BATALLA (selección de tropas)
        TextButton btnBatalla = new TextButton("", skin);
        btnBatalla.setPosition(Gdx.graphics.getWidth() * 0.166f, Gdx.graphics.getHeight() * 0.707f);
        btnBatalla.setSize(Gdx.graphics.getWidth() * 0.333f, Gdx.graphics.getHeight() * 0.159f);
        btnBatalla.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(">>> BATALLA clickeada! Iniciando selección de tropa...");
                juego.detenerMusica();                 // 🚫 Detenemos música del menú
           // ▶️ Reproducimos música de selección
                juego.setScreen(new PantallaSeleccionTropa(juego));
            }
        });
        stage.addActor(btnBatalla);
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
        juego.aplicarCursor();
        for (Actor actor : stage.getActors()) {
            if (actor instanceof TextButton) {
                TextButton btn = (TextButton) actor;
                if (btn.getListeners().size > 0 && btn.getListeners().first() instanceof ClickListener) {
                    // Reubicar botón de batalla (asumiendo que es el único con ese tamaño aproximado)
                    if (btn.getWidth() > 400 && btn.getHeight() > 100) {
                        btn.setPosition(width * 0.166f, height * 0.707f);
                        btn.setSize(width * 0.333f, height * 0.159f);
                    }
                }
            }
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        fondo.dispose();
        stage.dispose();
        skin.dispose();
    }
}
