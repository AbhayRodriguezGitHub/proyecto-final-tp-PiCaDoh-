package mijuego.picadoh;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mijuego.picadoh.batalla.ContextoBatalla;
import mijuego.picadoh.batalla.PantallaBatalla;
import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.cartas.RegistroCartas;
import mijuego.picadoh.efectos.CartaEfecto;
import mijuego.picadoh.efectos.RegistroEfectos;

import java.util.*;
import java.util.function.Consumer;

/**
 * Pantalla de selección de efectos.
 * En modo LAN: cuando el jugador finaliza las 7 elecciones, envía EFFECT_READY al servidor
 * y espera un mensaje START para crear la pantalla de batalla.
 */
public class PantallaSeleccionEfecto implements Screen {
    private final Principal juego;
    private Texture fondo;

    private static final float VW = 1920f;
    private static final float VH = 1080f;

    private OrthographicCamera camara;
    private Viewport viewport;

    private static final int L_X = 336;
    private static final int L_Y = 75;
    private static final int L_W = 924 - 336;
    private static final int L_H = 793 - 75;

    private static final int R_W = L_W;
    private static final int R_H = L_H;
    private static final int R_X = 1609 - R_W;
    private static final int R_Y = 75;

    private final List<Class<? extends CartaEfecto>> clasesEfecto = Arrays.asList(
        mijuego.picadoh.efectos.Acelereitor.class,
        mijuego.picadoh.efectos.EscudoReal.class,
        mijuego.picadoh.efectos.ExplosionForzal.class,
        mijuego.picadoh.efectos.MagiaBendita.class,
        mijuego.picadoh.efectos.SenoraArmadura.class,
        mijuego.picadoh.efectos.Tyson.class,
        mijuego.picadoh.efectos.AnarquiaNivel.class,
        mijuego.picadoh.efectos.Bombardrilo.class,
        mijuego.picadoh.efectos.Monarquia.class,
        mijuego.picadoh.efectos.Rebelion.class,
        mijuego.picadoh.efectos.MalDeAmores.class,
        mijuego.picadoh.efectos.MagoDel8.class,
        mijuego.picadoh.efectos.Paracetamol.class,
        mijuego.picadoh.efectos.Intercambio.class,
        mijuego.picadoh.efectos.Avaricioso.class,
        mijuego.picadoh.efectos.AgenteDeTransito.class,
        mijuego.picadoh.efectos.EscudoFalso.class,
        mijuego.picadoh.efectos.EscudoPlatinado.class,
        mijuego.picadoh.efectos.Gangsterio.class,
        mijuego.picadoh.efectos.Orikalkus.class
    );

    private final List<CartaEfecto> cartasEfectoElegidas = new ArrayList<>();
    private final List<CartaTropa> cartasTropaSeleccionadas;

    private CartaEfecto carta1, carta2;
    private boolean pantallaFinalizada = false;

    private boolean esperandoTransicion = false;
    private float tiempoDesdeClick = 0f;
    private CartaEfecto cartaSeleccionada = null;

    // Estado de espera por START
    private boolean esperandoStart = false;

    // Listener previo (si quisieras restaurarlo luego)
    private Consumer<com.google.gson.JsonObject> listenerPrevio = null;

    // Recursos de “espera rival”
    private Texture esperaImg = null;
    private final BitmapFont fontEsperando = new BitmapFont(); // fallback

    public PantallaSeleccionEfecto(Principal juego, List<CartaTropa> cartasTropaSeleccionadas) {
        this.juego = juego;
        this.cartasTropaSeleccionadas = cartasTropaSeleccionadas;
    }

    @Override
    public void show() {
        fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/menus/ELECCIONEFECTO.png"));

        // Imagen de “espera rival”
        try {
            esperaImg = new Texture(Gdx.files.absolute("lwjgl3/assets/lan/ESPERA1.png"));
        } catch (Exception e) {
            System.out.println("[PantallaSeleccionEfecto] No se pudo cargar ESPERA1.png: " + e.getMessage());
            esperaImg = null;
        }

        camara = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camara);
        viewport.apply(true);
        camara.position.set(VW / 2f, VH / 2f, 0f);
        camara.update();

        generarNuevoParDeCartas();

        Gdx.input.setInputProcessor(new InputAdapter() {
            private final Vector2 tmp = new Vector2();
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // mientras esperamos START, bloquear interacción
                if (esperandoTransicion || pantallaFinalizada || esperandoStart) return true;
                tmp.set(screenX, screenY);
                viewport.unproject(tmp);
                if (tmp.x >= L_X && tmp.x <= L_X + L_W && tmp.y >= L_Y && tmp.y <= L_Y + L_H) {
                    cartaSeleccionada = carta1;
                } else if (tmp.x >= R_X && tmp.x <= R_X + R_W && tmp.y >= R_Y && tmp.y <= R_Y + R_H) {
                    cartaSeleccionada = carta2;
                } else {
                    cartaSeleccionada = null;
                }
                if (cartaSeleccionada != null) {
                    esperandoTransicion = true;
                    tiempoDesdeClick = 0f;
                }
                return true;
            }
        });
    }

    private void generarNuevoParDeCartas() {
        Random rand = new Random();
        try {
            Class<? extends CartaEfecto> clase1 = clasesEfecto.get(rand.nextInt(clasesEfecto.size()));
            Class<? extends CartaEfecto> clase2;
            do {
                clase2 = clasesEfecto.get(rand.nextInt(clasesEfecto.size()));
            } while (clase1.equals(clase2));
            carta1 = clase1.getDeclaredConstructor().newInstance();
            carta2 = clase2.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            carta1 = null;
            carta2 = null;
        }
    }

    private void avanzarSeleccion() {
        if (cartaSeleccionada != null) cartasEfectoElegidas.add(cartaSeleccionada);
        if (cartaSeleccionada == carta1 && carta2 != null) carta2.dispose();
        if (cartaSeleccionada == carta2 && carta1 != null) carta1.dispose();
        carta1 = null;
        carta2 = null;
        cartaSeleccionada = null;

        if (cartasEfectoElegidas.size() >= 7) {
            if (pantallaFinalizada) return;
            pantallaFinalizada = true;

            if (juego.clienteLAN != null) {
                listenerPrevio = null; // si tu ClienteLAN tuviera getOnMessage() lo podrías guardar

                // escuchamos START
                juego.clienteLAN.setOnMessage(json -> {
                    try {
                        if (!json.has("type")) return;
                        String type = json.get("type").getAsString();
                        if ("START".equals(type)) {
                            List<String> playerTropas = jsonArrayToList(json.getAsJsonArray("playerTropas"));
                            List<String> playerEfectos = jsonArrayToList(json.getAsJsonArray("playerEfectos"));
                            List<String> enemyTropas = jsonArrayToList(json.getAsJsonArray("enemyTropas"));
                            int vidaP = json.has("vidaP") ? json.get("vidaP").getAsInt() : 80;
                            int vidaE = json.has("vidaE") ? json.get("vidaE").getAsInt() : 80;

                            Gdx.app.postRunnable(() -> {
                                try {
                                    List<CartaTropa> propias = convertToTropas(playerTropas);
                                    List<CartaEfecto> efectosPropios = convertToEfectos(playerEfectos);
                                    List<CartaTropa> enemigas = convertToTropas(enemyTropas);

                                    ContextoBatalla contexto = new ContextoBatalla(propias, enemigas, vidaP, vidaE);
                                    juego.setScreen(new PantallaBatalla(juego, contexto, efectosPropios));
                                } catch (Exception ex) {
                                    System.out.println("[CLIENTE-LAN] Error creando batalla desde START: " + ex.getMessage());
                                }
                            });
                        }
                    } catch (Exception ex) {
                        System.out.println("[CLIENTE-LAN] Error en listener de pantalla efecto: " + ex.getMessage());
                    }
                });

                // enviamos EFFECT_READY
                List<String> effectClassNames = new ArrayList<>();
                for (CartaEfecto e : cartasEfectoElegidas) if (e != null) effectClassNames.add(e.getClass().getName());
                System.out.println("[CLIENTE-LAN] Enviando EFFECT_READY...");
                try { juego.clienteLAN.sendEffectReady(effectClassNames); }
                catch (Exception ex) { System.out.println("[CLIENTE-LAN] Error al enviar EFFECT_READY: " + ex.getMessage()); }

                esperandoStart = true; // desde ahora mostramos la imagen de espera
            } else {
                // Modo offline
                ContextoBatalla contexto = new ContextoBatalla(cartasTropaSeleccionadas, new ArrayList<>(), 80, 80);
                juego.setScreen(new PantallaBatalla(juego, contexto, cartasEfectoElegidas));
                dispose();
            }
        } else {
            generarNuevoParDeCartas();
        }
    }

    private List<String> jsonArrayToList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement e : arr) out.add(e.getAsString());
        return out;
    }

    private List<CartaTropa> convertToTropas(List<String> names) {
        List<CartaTropa> out = new ArrayList<>();
        if (names == null) return out;
        for (String n : names) {
            CartaTropa t = tryCreateTropaByName(n);
            if (t != null) out.add(t);
        }
        return out;
    }

    private CartaTropa tryCreateTropaByName(String name) {
        try {
            Class<?> c = Class.forName(name);
            if (CartaTropa.class.isAssignableFrom(c)) {
                @SuppressWarnings("unchecked")
                Class<? extends CartaTropa> ct = (Class<? extends CartaTropa>) c;
                return RegistroCartas.crear(ct);
            }
        } catch (Throwable ignored) {}
        try {
            Optional<CartaTropa> opt = RegistroCartas.crearPorNombre(name);
            if (opt.isPresent()) return opt.get();
        } catch (Throwable ignored) {}
        System.out.println("[CLIENTE-LAN] No se pudo crear tropa: " + name);
        return null;
    }

    private List<CartaEfecto> convertToEfectos(List<String> names) {
        List<CartaEfecto> out = new ArrayList<>();
        if (names == null) return out;
        for (String n : names) {
            CartaEfecto e = tryCreateEfectoByName(n);
            if (e != null) out.add(e);
        }
        return out;
    }

    private CartaEfecto tryCreateEfectoByName(String name) {
        try {
            Class<?> c = Class.forName(name);
            if (CartaEfecto.class.isAssignableFrom(c)) {
                @SuppressWarnings("unchecked")
                Class<? extends CartaEfecto> ce = (Class<? extends CartaEfecto>) c;
                return RegistroEfectos.crear(ce);
            }
        } catch (Throwable ignored) {}
        try {
            Optional<CartaEfecto> opt = RegistroEfectos.crearPorNombre(name);
            if (opt.isPresent()) return opt.get();
        } catch (Throwable ignored) {}
        System.out.println("[CLIENTE-LAN] No se pudo crear efecto: " + name);
        return null;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        juego.batch.draw(fondo, 0, 0, VW, VH);

        if (!esperandoTransicion && carta1 != null && carta2 != null && !esperandoStart) {
            juego.batch.draw(carta1.getImagen(), L_X, L_Y, L_W, L_H);
            juego.batch.draw(carta2.getImagen(), R_X, R_Y, R_W, R_H);
        }

        // Mostrar imagen de espera mientras aguardamos el START del servidor
        if (esperandoStart) {
            if (esperaImg != null) {
                // cubrir pantalla completa
                juego.batch.draw(esperaImg, 0, 0, VW, VH);
            } else {
                // fallback a texto si no está la imagen
                String msg = "Esperando rival...";
                fontEsperando.getData().setScale(2f);
                float textX = VW / 2f - 200;
                float textY = VH / 2f;
                fontEsperando.draw(juego.batch, msg, textX, textY);
            }
        }

        juego.batch.end();

        if (esperandoTransicion) {
            tiempoDesdeClick += delta;
            if (tiempoDesdeClick >= 0.25f) {
                esperandoTransicion = false;
                avanzarSeleccion();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (fondo != null) fondo.dispose();
        if (esperaImg != null) esperaImg.dispose();
        if (carta1 != null) carta1.dispose();
        if (carta2 != null) carta2.dispose();
        fontEsperando.dispose();
        // (si guardaste listenerPrevio y querés restaurarlo, hacelo acá)
    }
}
