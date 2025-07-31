package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import mijuego.picadoh.Principal;
import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.efectos.CartaEfecto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PantallaBatalla implements Screen {

    private final Principal juego;
    private final SpriteBatch batch;
    private final Texture fondo;
    private final ContextoBatalla contexto;
    private final List<CartaEfecto> efectosDisponibles;

    private final List<CartaTropa> manoTropas;
    private CartaTropa cartaSeleccionada;
    private int cartaHoverIndex = -1;

    private float cartaDragX = 0;
    private float cartaDragY = 0;
    private boolean arrastrando = false;

    private final float ANCHO_CARTA = 100f;
    private final float ALTURA_CARTA = 150f;
    private final float Y_CARTA_MANO = 40f;

    private final Ranura[] ranuras;
    private final ShapeRenderer shapeRenderer;
    private Ranura ranuraHover = null;

    public PantallaBatalla(Principal juego, ContextoBatalla contexto, List<CartaEfecto> efectosDisponibles) {
        this.juego = juego;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VALLEMISTICO.png"));
        this.contexto = contexto;
        this.efectosDisponibles = efectosDisponibles;
        this.manoTropas = new ArrayList<>();

        List<CartaTropa> disponibles = new ArrayList<>(contexto.getTropasPropias());
        Collections.shuffle(disponibles);
        for (int i = 0; i < 3 && i < disponibles.size(); i++) {
            manoTropas.add(disponibles.get(i));
        }

        ranuras = new Ranura[] {
            new Ranura(36, 254, 267, 180),    // RANURA 1
            new Ranura(437, 254, 267, 180),   // RANURA 2
            new Ranura(833, 254, 267, 180),   // RANURA 3
            new Ranura(1229, 254, 267, 180),  // RANURA 4
            new Ranura(1615, 254, 270, 180)   // RANURA 5
        };

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = 40 + i * (ANCHO_CARTA + 10);
                    if (screenX >= x && screenX <= x + ANCHO_CARTA &&
                        yInvertida >= Y_CARTA_MANO && yInvertida <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaSeleccionada = manoTropas.get(i);
                        cartaDragX = screenX - ANCHO_CARTA / 2;
                        cartaDragY = yInvertida - ALTURA_CARTA / 2;
                        arrastrando = true;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (arrastrando && cartaSeleccionada != null) {
                    cartaDragX = screenX - ANCHO_CARTA / 2;
                    cartaDragY = Gdx.graphics.getHeight() - screenY - ALTURA_CARTA / 2;

                    int mouseY = Gdx.graphics.getHeight() - screenY;
                    ranuraHover = null;
                    for (Ranura ranura : ranuras) {
                        if (ranura.contiene(screenX, mouseY) && ranura.getCarta() == null) {
                            ranuraHover = ranura;
                            break;
                        }
                    }
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (arrastrando && cartaSeleccionada != null) {
                    int mouseY = Gdx.graphics.getHeight() - screenY;
                    for (Ranura ranura : ranuras) {
                        if (ranura.contiene(screenX, mouseY) && ranura.getCarta() == null) {
                            ranura.setCarta(cartaSeleccionada);
                            manoTropas.remove(cartaSeleccionada);
                            break;
                        }
                    }
                    cartaSeleccionada = null;
                    arrastrando = false;
                    ranuraHover = null;
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                int yInvertida = Gdx.graphics.getHeight() - screenY;
                cartaHoverIndex = -1;
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = 40 + i * (ANCHO_CARTA + 10);
                    if (screenX >= x && screenX <= x + ANCHO_CARTA &&
                        yInvertida >= Y_CARTA_MANO && yInvertida <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaHoverIndex = i;
                        break;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void show() {
        juego.detenerMusica();
        juego.detenerMusicaSeleccion();
        juego.reproducirMusicaBatalla();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(fondo, 0, 0, 1920, 1080);

        // Dibujar cartas en ranuras
        for (Ranura ranura : ranuras) {
            if (ranura.getCarta() != null) {
                batch.draw(ranura.getCarta().getImagen(), ranura.getX(), ranura.getY(), ranura.getAncho(), ranura.getAlto());
            }
        }

        // Dibujar cartas en mano
        for (int i = 0; i < manoTropas.size(); i++) {
            CartaTropa carta = manoTropas.get(i);
            if (carta == cartaSeleccionada) continue;

            float x = 40 + i * (ANCHO_CARTA + 10);
            float y = Y_CARTA_MANO;
            if (i == cartaHoverIndex) y += 20;

            batch.draw(carta.getImagen(), x, y, ANCHO_CARTA, ALTURA_CARTA);
        }

        // Dibujar carta arrastrada
        if (arrastrando && cartaSeleccionada != null) {
            batch.draw(cartaSeleccionada.getImagen(), cartaDragX, cartaDragY, ANCHO_CARTA, ALTURA_CARTA);
        }

        batch.end();

        // === Dibujar iluminación de ranura ===
        if (ranuraHover != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));  // celeste transparente
            shapeRenderer.rect(ranuraHover.getX(), ranuraHover.getY(), ranuraHover.getAncho(), ranuraHover.getAlto());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        fondo.dispose();
    }

    public ContextoBatalla getContexto() {
        return contexto;
    }

    public List<CartaEfecto> getEfectosDisponibles() {
        return efectosDisponibles;
    }
}
