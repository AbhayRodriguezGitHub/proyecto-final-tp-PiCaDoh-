package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import mijuego.picadoh.Principal;
import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.efectos.CartaEfecto;
import mijuego.picadoh.efectos.RegistroEfectos;
import mijuego.picadoh.cartas.RegistroCartas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * PantallaBatalla con sincronización LAN básica:
 * - Envía INVOKE cuando invocas (slot + class)
 * - Envía PLAY cuando presionás play
 * - Procesa REVEAL recibido desde servidor (se ejecuta en hilo principal)
 *
 * Requisitos:
 * - juego.clienteLAN debe estar conectado (connect()) antes de usar las funciones de red.
 */
public class PantallaBatalla implements Screen {

    private final Principal juego;
    private final SpriteBatch batch;
    private final Texture fondo;
    private final Texture imgSiguiente;
    private Texture vida2Img;
    private Texture vida3Img;
    private Texture vida4Img;

    // Cámara + Viewport para escalar correctamente todo
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Vector3 tmpUnproject = new Vector3();

    private static final float VIDA_IMG_X = 347f;
    private static final float VIDA_IMG_Y = 469f;
    private static final float VIDA_IMG_W = 1908f - 1670f;
    private static final float VIDA_IMG_H = 623f - 469f;

    private static final float VIDA_ENE_IMG_X = 1671f;
    private static final float VIDA_ENE_IMG_Y = 469f;
    private static final float VIDA_ENE_IMG_W = 1915f - 1675f;
    private static final float VIDA_ENE_IMG_H = 623f - 469f;

    private final ContextoBatalla contexto;
    private final List<CartaEfecto> efectosDisponibles;

    private final List<CartaTropa> manoTropas;
    private final List<CartaEfecto> manoEfectos;

    private final List<CartaTropa> mazoTropasRestantes;
    private final List<CartaEfecto> mazoEfectosRestantes;

    private CartaTropa cartaSeleccionada;
    private int cartaSeleccionadaIndex = -1; // índice en la mano de la carta seleccionada
    private int cartaHoverIndex = -1;

    private float cartaDragX = 0;
    private float cartaDragY = 0;
    private boolean arrastrando = false;

    private CartaEfecto cartaEfectoSeleccionada = null;
    private int efectoHoverIndex = -1;
    private float efectoDragX = 0f;
    private float efectoDragY = 0f;
    private boolean arrastrandoEfecto = false;
    private int indiceEfectoTomado = -1;
    private boolean hoverRanuraEfectoJugador = false;

    private CartaEfecto efectoEnRanuraJugador = null;
    private boolean efectoAplicadoEsteTurno = false;

    private static class SnapshotStats {
        final CartaTropa tropa;
        final int deltaAtk;
        final int deltaDef;
        SnapshotStats(CartaTropa t, int deltaAtk, int deltaDef) {
            this.tropa = t;
            this.deltaAtk = deltaAtk;
            this.deltaDef = deltaDef;
        }
        void revertir() {
            if (tropa == null) return;
            tropa.setAtk(Math.max(0, tropa.getAtk() - deltaAtk));
            tropa.setDef(Math.max(0, tropa.getDef() - deltaDef));
        }
    }
    private final List<SnapshotStats> snapshotsEfectoTurno = new ArrayList<>();

    private final float ANCHO_CARTA = 100f;
    private final float ALTURA_CARTA = 150f;

    private static final int MAX_CARTAS_MANO = 7;

    private final float Y_CARTA_MANO = 40f;
    private final float TROPAS_X_INICIO = 0f;
    private final float ESPACIO_CARTAS = 10f;

    private final float Y_CARTA_MANO_DERECHA = 40f;
    private final float EFECTOS_BORDE_DER = 1920f;

    private final List<Ranura> ranuras;
    private final ShapeRenderer shapeRenderer;
    private Ranura ranuraHover = null;

    private final int VIDA_BARRA_ANCHO = 153;
    private final int VIDA_BARRA_ALTO = 15;
    private final int VIDA_Y = 496;
    private final int VIDA_X_JUGADOR = 421;
    private final int VIDA_X_ENEMIGO = 1746;

    private final BitmapFont fuenteVida;
    private final GlyphLayout layout;

    private int ranuraActual = -1;
    private float tiempoHighlight = 0f;
    private final float DURACION_HIGHLIGHT = 0.5f;
    private boolean batallaEnCurso = false;

    private boolean mostrarBotonSiguiente = false;

    private final int BOTON_PLAY_X = 870;
    private final int BOTON_PLAY_Y = 430;
    private final int BOTON_PLAY_ANCHO = 205;
    private final int BOTON_PLAY_ALTO = 220;

    private static final int MAX_TURNO = 22;
    private int turnoActual = 1;

    private final List<List<Integer>> nivelesPorTurno = new ArrayList<>();

    private final BitmapFont fuenteTurno;
    private final GlyphLayout layoutTurno;

    private final BitmapFont fuenteNiveles;
    private final GlyphLayout layoutNiveles = new GlyphLayout();

    private final int TURNO_X = 727;
    private final int TURNO_Y = 515;
    private final int TURNO_ANCHO = 68;
    private final int TURNO_ALTO = 65;

    private final float AREA_NIVELES_X = 1105f;
    private final float AREA_NIVELES_WIDTH = 157f;
    private final float AREA_NIVELES_Y = 498f;
    private final float AREA_NIVELES_HEIGHT = 73f;

    private boolean partidaTerminada = false;

    private static final int EFECTO_JUG_X1 = 812;
    private static final int EFECTO_JUG_X2 = 1108;
    private static final int EFECTO_JUG_Y1 = 8;
    private static final int EFECTO_JUG_Y2 = 208;

    private static final int EFECTO_ENE_X1 = 812;
    private static final int EFECTO_ENE_X2 = 1108;
    private static final int EFECTO_ENE_Y1 = 872;
    private static final int EFECTO_ENE_Y2 = 1072;

    private final float EFECTO_FULL_W = (EFECTO_JUG_X2 - EFECTO_JUG_X1);
    private final float EFECTO_FULL_H = (EFECTO_JUG_Y2 - EFECTO_JUG_Y1);

    private static final int MAX_INVOC_TROPAS_TURNO = 2;
    private int invocacionesTropaEsteTurno = 0;

    // Estado LAN
    private boolean esperandoReveal = false; // True después de enviar PLAY hasta recibir REVEAL

    private final Gson gson = new Gson();

    public PantallaBatalla(Principal juego, ContextoBatalla contexto, List<CartaEfecto> efectosDisponibles) {
        this.juego = juego;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();

        // Mundo virtual fijo 1920x1080; lo demás se ajusta con el viewport.
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(1920f, 1080f, camera);
        camera.position.set(960f, 540f, 0f);
        camera.update();

        this.fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VALLEMISTICO.png"));
        this.imgSiguiente = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/SIGUIENTE.png"));
        vida2Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA2.png"));
        vida3Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA3.png"));
        vida4Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA4.png"));
        this.contexto = contexto;
        this.efectosDisponibles = efectosDisponibles;

        this.manoTropas = new ArrayList<>();
        this.manoEfectos = new ArrayList<>();

        List<CartaTropa> disponibles = new ArrayList<>(contexto.getTropasPropias());
        Collections.shuffle(disponibles);

        for (int i = 0; i < 3 && !disponibles.isEmpty() && manoTropas.size() < MAX_CARTAS_MANO; i++) {
            manoTropas.add(disponibles.remove(0));
        }
        this.mazoTropasRestantes = new ArrayList<>(disponibles);

        this.mazoEfectosRestantes = new ArrayList<>(efectosDisponibles);
        Collections.shuffle(this.mazoEfectosRestantes);

        this.fuenteVida = new BitmapFont();
        this.layout = new GlyphLayout();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("lwjgl3/assets/fonts/arial.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 48;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.minFilter = Texture.TextureFilter.Linear;
        this.fuenteTurno = generator.generateFont(parameter);
        this.fuenteTurno.setColor(Color.BLACK);
        generator.dispose();

        this.layoutTurno = new GlyphLayout();

        FreeTypeFontGenerator genNiv = new FreeTypeFontGenerator(Gdx.files.internal("lwjgl3/assets/fonts/arial.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter pNiv = new FreeTypeFontGenerator.FreeTypeFontParameter();
        pNiv.size = 28;
        pNiv.magFilter = Texture.TextureFilter.Linear;
        pNiv.minFilter = Texture.TextureFilter.Linear;
        this.fuenteNiveles = genNiv.generateFont(pNiv);
        this.fuenteNiveles.setColor(Color.WHITE);
        genNiv.dispose();

        ranuras = new ArrayList<>();
        // Las primeras 5 son del jugador (esEnemigo = false)
        ranuras.add(new Ranura(36, 254, 267, 180, false));
        ranuras.add(new Ranura(437, 254, 267, 180, false));
        ranuras.add(new Ranura(833, 254, 267, 180, false));
        ranuras.add(new Ranura(1229, 254, 267, 180, false));
        ranuras.add(new Ranura(1615, 254, 270, 180, false));

        // Las siguientes 5 son del enemigo (esEnemigo = true)
        ranuras.add(new Ranura(22, 645, 283, 183, true));
        ranuras.add(new Ranura(412, 645, 286, 183, true));
        ranuras.add(new Ranura(813, 645, 286, 183, true));
        ranuras.add(new Ranura(1213, 645, 282, 183, true));
        ranuras.add(new Ranura(1615, 645, 283, 183, true));

        nivelesPorTurno.add(List.of(1));
        nivelesPorTurno.add(List.of(1, 2));
        nivelesPorTurno.add(List.of(1, 2));
        nivelesPorTurno.add(List.of(1, 2, 3));
        nivelesPorTurno.add(List.of(1, 2, 3));
        nivelesPorTurno.add(List.of(2, 3, 4));
        nivelesPorTurno.add(List.of(2, 3, 4));
        nivelesPorTurno.add(List.of(2, 4));
        nivelesPorTurno.add(List.of(3, 4));
        nivelesPorTurno.add(List.of(4, 5));
        nivelesPorTurno.add(List.of(4, 5));
        nivelesPorTurno.add(List.of(5));
        nivelesPorTurno.add(List.of(1, 5));
        nivelesPorTurno.add(List.of(1, 2, 5));
        nivelesPorTurno.add(List.of(1, 2, 3));
        nivelesPorTurno.add(List.of(1, 2, 3, 4));
        nivelesPorTurno.add(List.of(1, 3));
        nivelesPorTurno.add(List.of(2, 4));
        nivelesPorTurno.add(List.of(3, 5));
        nivelesPorTurno.add(List.of(1, 2, 3, 4, 5));
        nivelesPorTurno.add(List.of(1, 2, 3, 4, 5));
        nivelesPorTurno.add(List.of(1, 2, 3, 4, 5));

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (partidaTerminada) return false;
                if (keycode == com.badlogic.gdx.Input.Keys.F1) {
                    contexto.setVidaPropia(contexto.getVidaMaxima());
                    contexto.setVidaEnemiga(contexto.getVidaMaxima());
                    System.out.println("[DEBUG] Vida restaurada a ambos jugadores.");
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.F2) {
                    contexto.restarVidaEnemiga(10);
                    System.out.println("[DEBUG] Daño de 10 puntos al enemigo.");
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.F3) {
                    contexto.restarVidaPropia(10);
                    System.out.println("[DEBUG] Daño de 10 puntos al jugador.");
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.SPACE) {
                    iniciarBatallaConSiguiente();
                    return true;
                }
                return false;
            }

            private void unproj(int screenX, int screenY) {
                tmpUnproject.set(screenX, screenY, 0f);
                viewport.unproject(tmpUnproject); // → coords del mundo 1920×1080
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (batallaEnCurso || partidaTerminada || esperandoReveal) return false;

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                if (wx >= BOTON_PLAY_X && wx <= BOTON_PLAY_X + BOTON_PLAY_ANCHO &&
                    wy >= BOTON_PLAY_Y && wy <= BOTON_PLAY_Y + BOTON_PLAY_ALTO) {
                    // cuando se presiona play, mandamos PLAY al servidor y bloqueamos
                    enviarPlayAlServidor();
                    System.out.println("[INPUT] Botón PLAY presionado → play/esperando");
                    return true;
                }

                for (int i = 0; i < manoEfectos.size(); i++) {
                    float x = (EFECTOS_BORDE_DER - ANCHO_CARTA) - (ANCHO_CARTA + ESPACIO_CARTAS) * i;
                    float y = Y_CARTA_MANO_DERECHA;
                    if (wx >= x && wx <= x + ANCHO_CARTA &&
                        wy >= y && wy <= y + ALTURA_CARTA + 30) {
                        cartaEfectoSeleccionada = manoEfectos.get(i);
                        indiceEfectoTomado = i;
                        manoEfectos.remove(i);
                        efectoDragX = wx - ANCHO_CARTA / 2f;
                        efectoDragY = wy - ALTURA_CARTA / 2f;
                        arrastrandoEfecto = true;
                        return true;
                    }
                }

                // Selección de tropa: guardamos índice para poder eliminar al invocar
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
                    if (wx >= x && wx <= x + ANCHO_CARTA &&
                        wy >= Y_CARTA_MANO && wy <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaSeleccionada = manoTropas.get(i);
                        cartaSeleccionadaIndex = i; // <-- guardamos índice
                        cartaDragX = wx - ANCHO_CARTA / 2f;
                        cartaDragY = wy - ALTURA_CARTA / 2f;
                        arrastrando = true;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (batallaEnCurso || partidaTerminada || esperandoReveal) return false;

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                if (arrastrando && cartaSeleccionada != null) {
                    cartaDragX = wx - ANCHO_CARTA / 2f;
                    cartaDragY = wy - ALTURA_CARTA / 2f;

                    ranuraHover = null;
                    // Buscar ranura válida SOLO entre las ranuras del JUGADOR (primeras 5)
                    for (int idx = 0; idx < 5; idx++) {
                        Ranura r = ranuras.get(idx);
                        if (r.contiene((int) wx, (int) wy) && (r.getCarta() == null) && !r.esEnemigo()) {
                            ranuraHover = r;
                            break;
                        }
                    }
                }

                if (arrastrandoEfecto && cartaEfectoSeleccionada != null) {
                    efectoDragX = wx - ANCHO_CARTA / 2f;
                    efectoDragY = wy - ALTURA_CARTA / 2f;

                    hoverRanuraEfectoJugador =
                        wx >= EFECTO_JUG_X1 && wx <= EFECTO_JUG_X2 &&
                            wy >= EFECTO_JUG_Y1 && wy <= EFECTO_JUG_Y2;
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (batallaEnCurso || partidaTerminada || esperandoReveal) return false;

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                if (arrastrando && cartaSeleccionada != null) {
                    boolean invocado = false;
                    int ranuraIndexInvocada = -1;
                    // Intentamos invocar SOLO en las ranuras del JUGADOR (índices 0..4)
                    for (int idx = 0; idx < 5; idx++) {
                        Ranura ranura = ranuras.get(idx);
                        if (ranura.contiene((int) wx, (int) wy) && ranura.getCarta() == null && !ranura.esEnemigo()) {

                            if (!contexto.isInvocacionesIlimitadasEsteTurno()
                                && invocacionesTropaEsteTurno >= MAX_INVOC_TROPAS_TURNO) {
                                System.out.println("[INVOCACIÓN BLOQUEADA] Ya invocaste " + MAX_INVOC_TROPAS_TURNO + " tropas este turno.");
                                break;
                            }

                            if (puedeInvocarPorNivel(cartaSeleccionada)) {
                                // Asigna la carta a la ranura del jugador
                                ranura.setCarta(cartaSeleccionada);

                                // --- Eliminar la carta de la mano (USO ÚNICO) ---
                                if (cartaSeleccionadaIndex >= 0 && cartaSeleccionadaIndex < manoTropas.size()) {
                                    // si el objeto coincide, eliminar por índice
                                    if (manoTropas.get(cartaSeleccionadaIndex) == cartaSeleccionada) {
                                        manoTropas.remove(cartaSeleccionadaIndex);
                                    } else {
                                        // fallback: eliminar la instancia encontrada
                                        manoTropas.remove(cartaSeleccionada);
                                    }
                                } else {
                                    // fallback: eliminar por objeto si existe
                                    manoTropas.remove(cartaSeleccionada);
                                }

                                invocacionesTropaEsteTurno++;
                                System.out.println("[INVOCACIÓN] Tropas invocadas este turno: " + invocacionesTropaEsteTurno + "/" + MAX_INVOC_TROPAS_TURNO);
                                invocado = true;
                                ranuraIndexInvocada = idx;
                            } else {
                                System.out.println("[INVOCACIÓN BLOQUEADA] No puedes invocar carta de nivel "
                                    + cartaSeleccionada.getNivel() + " en el turno " + turnoActual);
                            }
                            break;
                        }
                    }

                    // Si invocamos correctamente, mandamos INVOKE al servidor (slot 1..5)
                    if (invocado && ranuraIndexInvocada >= 0) {
                        String clase = cartaSeleccionada.getClass().getName();
                        enviarInvokeAlServidor(ranuraIndexInvocada + 1, clase);
                    }

                    // reset selección siempre
                    cartaSeleccionada = null;
                    cartaSeleccionadaIndex = -1;
                    arrastrando = false;
                    ranuraHover = null;
                }

                if (arrastrandoEfecto && cartaEfectoSeleccionada != null) {
                    boolean invocado = false;
                    if (wx >= EFECTO_JUG_X1 && wx <= EFECTO_JUG_X2 &&
                        wy >= EFECTO_JUG_Y1 && wy <= EFECTO_JUG_Y2 &&
                        efectoEnRanuraJugador == null) {
                        efectoEnRanuraJugador = cartaEfectoSeleccionada;
                        System.out.println("[EFECTO] Colocado efecto en ranura: " + efectoEnRanuraJugador.getNombre());
                        invocado = true;

                        if (efectoEnRanuraJugador.esInstantaneo()) {
                            try {
                                contexto.setTropaSeleccionada(null);
                                efectoEnRanuraJugador.aplicarEfecto(contexto);

                                if (contexto.isPurgaPorNivelSolicitada()) {
                                    for (int i = 0; i < ranuras.size(); i++) {
                                        if (ranuras.get(i).getCarta() != null) {
                                            int nivel = ranuras.get(i).getCarta().getNivel();
                                            if (contexto.getNivelesAPurgar().contains(nivel)) {
                                                ranuras.get(i).setCarta(null);
                                            }
                                        }
                                    }
                                    contexto.limpiarPurgaPorNivelSolicitud();
                                    System.out.println("[EFECTO] Purga por nivel aplicada.");
                                }

                                if (contexto.isLimpiarCampoSolicitado()) {
                                    for (int i = 0; i < ranuras.size(); i++) {
                                        ranuras.get(i).setCarta(null);
                                    }
                                    contexto.setLimpiarCampoSolicitado(false);
                                    System.out.println("[EFECTO] Limpieza total del campo aplicada.");
                                }

                                efectoAplicadoEsteTurno = true;
                                System.out.println("[EFECTO] " + efectoEnRanuraJugador.getNombre() + " aplicado instantáneamente.");
                            } catch (Exception ex) {
                                System.out.println("[EFECTO] Error en aplicación instantánea: " + ex.getMessage());
                            }
                        }
                    }

                    if (!invocado) {
                        int idx = (indiceEfectoTomado >= 0 && indiceEfectoTomado <= manoEfectos.size())
                            ? indiceEfectoTomado : manoEfectos.size();
                        manoEfectos.add(idx, cartaEfectoSeleccionada);
                    }

                    cartaEfectoSeleccionada = null;
                    arrastrandoEfecto = false;
                    indiceEfectoTomado = -1;
                    hoverRanuraEfectoJugador = false;
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (batallaEnCurso || partidaTerminada || esperandoReveal) {
                    cartaHoverIndex = -1;
                    efectoHoverIndex = -1;
                    return false;
                }

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                cartaHoverIndex = -1;
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
                    if (wx >= x && wx <= x + ANCHO_CARTA &&
                        wy >= Y_CARTA_MANO && wy <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaHoverIndex = i;
                        break;
                    }
                }

                efectoHoverIndex = -1;
                for (int i = 0; i < manoEfectos.size(); i++) {
                    float x = (EFECTOS_BORDE_DER - ANCHO_CARTA) - (ANCHO_CARTA + ESPACIO_CARTAS) * i;
                    float y = Y_CARTA_MANO_DERECHA;
                    if (wx >= x && wx <= x + ANCHO_CARTA &&
                        wy >= y && wy <= y + ALTURA_CARTA + 30) {
                        efectoHoverIndex = i;
                        break;
                    }
                }
                return false;
            }
        });

        // Registrar listener LAN (si hay cliente)
        registerClientListenerIfNeeded();
    }

    private void registerClientListenerIfNeeded() {
        if (juego == null || juego.clienteLAN == null) return;

        // Este listener correrá en el thread del cliente; si necesita crear Texturas o instancias gráficas
        // lo pasamos al hilo principal con Gdx.app.postRunnable(...)
        juego.clienteLAN.setOnMessage(json -> {
            try {
                if (json == null || !json.has("type")) return;
                String type = json.get("type").getAsString();
                if ("MATCHED".equals(type)) {
                    System.out.println("[LAN-RECV] " + json.toString());
                } else if ("REVEAL".equals(type)) {
                    System.out.println("[LAN-RECV] REVEAL: " + json.toString());
                    // procesar reveal en hilo principal para evitar crear Textures fuera del contexto GL
                    Gdx.app.postRunnable(() -> {
                        applyReveal(json);
                        // liberamos el bloqueo de espera (siempre que llegue REVEAL)
                        esperandoReveal = false;
                    });
                } else if ("OPPONENT_DISCONNECTED".equals(type)) {
                    System.out.println("[LAN-RECV] Rival desconectado.");
                    // Podrías mostrar mensaje en pantalla o regresar al menú
                } else {
                    // otros tipos (pong, etc.)
                    System.out.println("[LAN-RECV] " + json.toString());
                }
            } catch (Exception ex) {
                System.out.println("[LAN-RECV] Error procesando mensaje: " + ex.getMessage());
            }
        });
    }

    // Enviar INVOKE al servidor
    private void enviarInvokeAlServidor(int slot, String className) {
        if (juego == null || juego.clienteLAN == null) return;
        try {
            JsonObject o = new JsonObject();
            o.addProperty("type", "INVOKE");
            o.addProperty("slot", slot); // 1..5
            o.addProperty("class", className);
            juego.clienteLAN.sendJson(o);
            System.out.println("[LAN-SENT] INVOKE -> slot=" + slot + " class=" + className);
        } catch (Exception ex) {
            System.out.println("[LAN-SENT] Error enviando INVOKE: " + ex.getMessage());
        }
    }

    // Enviar PLAY al servidor (y marcar esperandoReveal para bloquear inputs hasta recibir REVEAL)
    private void enviarPlayAlServidor() {
        if (juego == null || juego.clienteLAN == null) {
            // fallback local: ejecutar batalla directamente
            iniciarBatallaConSiguiente();
            return;
        }
        try {
            JsonObject o = new JsonObject();
            o.addProperty("type", "PLAY");
            juego.clienteLAN.sendJson(o);
            esperandoReveal = true;
            // opcional: mostrar indicador "Esperando rival..." (podés dibujarlo en render según esperandoReveal)
            System.out.println("[CLIENTE-LAN] PLAY enviado. Esperando REVEAL del servidor...");
        } catch (Exception ex) {
            System.out.println("[CLIENTE-LAN] Error enviando PLAY: " + ex.getMessage());
            // fallback local
            iniciarBatallaConSiguiente();
        }
    }

    // Aplica los invokes recibidos en el REVEAL (ya estamos en hilo principal cuando se llama)
    private void applyReveal(JsonObject msg) {
        try {
            // playerInvokes: lista de objetos { slot: int, class: string }
            // enemyInvokes: lista de objetos { slot: int, class: string }
            List<JsonObject> playerInvokes = new ArrayList<>();
            List<JsonObject> enemyInvokes = new ArrayList<>();

            if (msg.has("playerInvokes")) {
                JsonArray arr = msg.getAsJsonArray("playerInvokes");
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) playerInvokes.add(e.getAsJsonObject());
                }
            }
            if (msg.has("enemyInvokes")) {
                JsonArray arr = msg.getAsJsonArray("enemyInvokes");
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) enemyInvokes.add(e.getAsJsonObject());
                }
            }

            // Crear y colocar las tropas reveladas:
            // playerInvokes -> ranuras 0..4 (slot 1 -> index 0)
            // enemyInvokes -> ranuras 5..9 (slot 1 -> index 5)
            for (JsonObject p : playerInvokes) {
                int slot = p.has("slot") ? p.get("slot").getAsInt() : -1;
                String cls = p.has("class") ? p.get("class").getAsString() : null;
                if (slot >= 1 && slot <= 5 && cls != null) {
                    CartaTropa t = createTropaInstanceByName(cls);
                    if (t != null) {
                        ranuras.get(slot - 1).setCarta(t);
                        System.out.println("[LAN-REVEAL] Player slot " + slot + " -> " + cls);
                    }
                }
            }
            for (JsonObject p : enemyInvokes) {
                int slot = p.has("slot") ? p.get("slot").getAsInt() : -1;
                String cls = p.has("class") ? p.get("class").getAsString() : null;
                if (slot >= 1 && slot <= 5 && cls != null) {
                    CartaTropa t = createTropaInstanceByName(cls);
                    if (t != null) {
                        ranuras.get(5 + (slot - 1)).setCarta(t);
                        System.out.println("[LAN-REVEAL] Enemy slot " + slot + " -> " + cls);
                    }
                }
            }

            // Al recibir REVEAL, ejecutamos la fase de combate localmente (mismo comportamiento que cuando se presiona PLAY)
            iniciarBatallaConSiguiente();
        } catch (Exception ex) {
            System.out.println("[LAN-REVEAL] Error applying reveal: " + ex.getMessage());
            ex.printStackTrace();
            // fallback: iniciar batalla local
            iniciarBatallaConSiguiente();
        }
    }

    // Intenta crear una tropa por nombre de clase o por nombre simple/display name
    private CartaTropa createTropaInstanceByName(String name) {
        if (name == null) return null;
        // 1) intentar Class.forName
        try {
            Class<?> c = Class.forName(name);
            if (CartaTropa.class.isAssignableFrom(c)) {
                @SuppressWarnings("unchecked")
                Class<? extends CartaTropa> ct = (Class<? extends CartaTropa>) c;
                return RegistroCartas.crear(ct);
            }
        } catch (Throwable ignored) {}

        // 2) intentar registro por nombre simple o por display name
        try {
            Optional<CartaTropa> opt = RegistroCartas.crearPorNombre(name);
            if (opt.isPresent()) return opt.get();
        } catch (Throwable ignored) {}

        System.out.println("[LAN] No se pudo crear tropa: " + name);
        return null;
    }

    private void iniciarBatallaConSiguiente() {
        if (!batallaEnCurso) {
            ejecutarBatalla();
            mostrarBotonSiguiente = true;
        }
    }

    private void ejecutarBatalla() {
        System.out.println("[COMBATE] Iniciando animación de batalla...");

        if (efectoEnRanuraJugador != null && !efectoAplicadoEsteTurno) {
            try {
                snapshotsEfectoTurno.clear();
                for (int i = 0; i < 5; i++) {
                    CartaTropa t = ranuras.get(i).getCarta();
                    if (t == null) continue;

                    int atkAntes = t.getAtk();
                    int defAntes = t.getDef();

                    contexto.setTropaSeleccionada(t);
                    efectoEnRanuraJugador.aplicarEfecto(contexto);

                    int deltaAtk = t.getAtk() - atkAntes;
                    int deltaDef = t.getDef() - defAntes;

                    snapshotsEfectoTurno.add(new SnapshotStats(t, deltaAtk, deltaDef));
                }
                contexto.setTropaSeleccionada(null);

                System.out.println("[EFECTO] Aplicado a " + snapshotsEfectoTurno.size() +
                    " tropas: " + efectoEnRanuraJugador.getNombre());
            } catch (Exception e) {
                System.out.println("[EFECTO] Error aplicando efecto: " + e.getMessage());
            }
            efectoAplicadoEsteTurno = true;
        }

        ranuraActual = 0;
        tiempoHighlight = 0f;
        batallaEnCurso = true;
    }

    private void procesarAtaqueRanura(int i) {
        Ranura ranuraJugador = ranuras.get(i);
        Ranura ranuraEnemigo = ranuras.get(i + 5);

        if (ranuraJugador.getCarta() != null) {
            CartaTropa cartaJugador = ranuraJugador.getCarta();

            if (ranuraEnemigo.getCarta() != null) {
                CartaTropa cartaEnemigo = ranuraEnemigo.getCarta();

                int dañoJugador = cartaJugador.getAtaque();
                int nuevaDefEnemigo = cartaEnemigo.getDefensa() - dañoJugador;

                int dañoEnemigo = contexto.isAtaquesEnemigosAnuladosEsteTurno() ? 0 : cartaEnemigo.getAtaque();
                int nuevaDefJugador = cartaJugador.getDefensa() - dañoEnemigo;

                if (nuevaDefEnemigo <= 0) {
                    int dañoRestante = -nuevaDefEnemigo;
                    contexto.restarVidaEnemiga(dañoRestante);
                    ranuraEnemigo.setCarta(null);
                    System.out.println("[COMBATE] Carta enemiga destruida en ranura " + (i + 1));
                } else {
                    cartaEnemigo.setDefensa(nuevaDefEnemigo);
                }

                if (nuevaDefJugador <= 0) {
                    int dañoRestante = -nuevaDefJugador;
                    if (dañoRestante > 0) contexto.restarVidaPropia(dañoRestante);
                    ranuraJugador.setCarta(null);
                    System.out.println("[COMBATE] Carta del jugador destruida en ranura " + (i + 1));
                } else {
                    cartaJugador.setDefensa(nuevaDefJugador);
                }

            } else {
                contexto.restarVidaEnemiga(cartaJugador.getAtaque());
                System.out.println("[COMBATE] Ataque directo al enemigo por " + cartaJugador.getAtaque() + " desde ranura " + (i + 1));
            }
        }

        if (ranuraEnemigo.getCarta() != null && ranuraJugador.getCarta() == null) {
            int dano = contexto.isAtaquesEnemigosAnuladosEsteTurno() ? 0 : ranuraEnemigo.getCarta().getAtaque();
            if (dano > 0) {
                contexto.restarVidaPropia(dano);
                System.out.println("[COMBATE] Ataque directo al jugador por " + dano + " desde ranura enemiga " + (i + 1));
            } else {
                System.out.println("[COMBATE] Ataque enemigo anulado en ranura " + (i + 1));
            }
        }

        verificarCondicionYTransicion();
    }

    @Override
    public void show() {
        juego.detenerMusicaSeleccion();
        juego.reproducirMusicaBatalla();
    }

    @Override
    public void render(float delta) {
        if (partidaTerminada) return;

        // Aplicar viewport y matrices de proyección antes de dibujar
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int vidaUsuario = contexto.getVidaPropia();
        int vidaEnemigo = contexto.getVidaEnemiga();

        batch.begin();
        batch.draw(fondo, 0, 0, 1920, 1080);

        for (Ranura ranura : ranuras) {
            if (ranura.getCarta() != null) {
                batch.draw(ranura.getCarta().getImagen(), ranura.getX(), ranura.getY(), ranura.getAncho(), ranura.getAlto());
            }
        }

        for (int i = 0; i < manoTropas.size(); i++) {
            CartaTropa carta = manoTropas.get(i);
            if (carta == cartaSeleccionada) continue;
            float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
            float y = Y_CARTA_MANO;
            if (i == cartaHoverIndex) y += 20;
            batch.draw(carta.getImagen(), x, y, ANCHO_CARTA, ALTURA_CARTA);
        }

        if (arrastrando && cartaSeleccionada != null) {
            batch.draw(cartaSeleccionada.getImagen(), cartaDragX, cartaDragY, ANCHO_CARTA, ALTURA_CARTA);
        }

        dibujarManoEfectos();

        if (efectoEnRanuraJugador != null) {
            batch.draw(efectoEnRanuraJugador.getImagen(), EFECTO_JUG_X1, EFECTO_JUG_Y1, EFECTO_FULL_W, EFECTO_FULL_H);
        }

        if (arrastrandoEfecto && cartaEfectoSeleccionada != null) {
            batch.draw(cartaEfectoSeleccionada.getImagen(), efectoDragX, efectoDragY, ANCHO_CARTA, ALTURA_CARTA);
        }

        if (mostrarBotonSiguiente) {
            batch.draw(imgSiguiente, BOTON_PLAY_X, BOTON_PLAY_Y, BOTON_PLAY_ANCHO, BOTON_PLAY_ALTO);
        }

        String textoTurno = String.valueOf(turnoActual);
        layoutTurno.setText(fuenteTurno, textoTurno);
        float textX = TURNO_X + (TURNO_ANCHO - layoutTurno.width) / 2f;
        float textY = TURNO_Y + (TURNO_ALTO + layoutTurno.height) / 2f;
        fuenteTurno.setColor(Color.BLACK);
        fuenteTurno.draw(batch, layoutTurno, textX, textY);

        dibujarNivelesDisponibles();

        if (vida2Img != null && vida3Img != null && vida4Img != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);

            if (vidaUsuario <= 50 && vidaUsuario > 20) {
                batch.draw(vida2Img, VIDA_IMG_X, VIDA_IMG_Y, VIDA_IMG_W, VIDA_IMG_H);
            } else if (vidaUsuario <= 20 && vidaUsuario > 0) {
                batch.draw(vida3Img, VIDA_IMG_X, VIDA_IMG_Y, VIDA_IMG_W, VIDA_IMG_H);
            } else if (vidaUsuario <= 0) {
                batch.draw(vida4Img, VIDA_IMG_X, VIDA_IMG_Y, VIDA_IMG_W, VIDA_IMG_H);
            }

            if (vidaEnemigo <= 50 && vidaEnemigo > 20) {
                batch.draw(vida2Img, VIDA_ENE_IMG_X, VIDA_ENE_IMG_Y, VIDA_ENE_IMG_W, VIDA_ENE_IMG_H);
            } else if (vidaEnemigo <= 20 && vidaEnemigo > 0) {
                batch.draw(vida3Img, VIDA_ENE_IMG_X, VIDA_ENE_IMG_Y, VIDA_ENE_IMG_W, VIDA_ENE_IMG_H);
            } else if (vidaEnemigo <= 0) {
                batch.draw(vida4Img, VIDA_ENE_IMG_X, VIDA_ENE_IMG_Y, VIDA_ENE_IMG_W, VIDA_ENE_IMG_H);
            }

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        batch.end();

        dibujarBarraVida();

        if (ranuraHover != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));
            shapeRenderer.rect(ranuraHover.getX(), ranuraHover.getY(), ranuraHover.getAncho(), ranuraHover.getAlto());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (arrastrandoEfecto && hoverRanuraEfectoJugador) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));
            shapeRenderer.rect(EFECTO_JUG_X1, EFECTO_JUG_Y1, EFECTO_FULL_W, EFECTO_FULL_H);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (esperandoReveal) {
            // dibujar texto "Esperando rival..." simple
            batch.begin();
            BitmapFont f = new BitmapFont();
            GlyphLayout g = new GlyphLayout();
            g.setText(f, "Esperando rival...");
            f.setColor(Color.WHITE);
            f.draw(batch, g, (1920 - g.width) / 2f, 540f);
            batch.end();
        }

        if (batallaEnCurso) {
            tiempoHighlight += delta;
            if (tiempoHighlight >= DURACION_HIGHLIGHT) {
                procesarAtaqueRanura(ranuraActual);
                ranuraActual++;
                tiempoHighlight = 0f;

                if (ranuraActual >= 5) {
                    batallaEnCurso = false;
                    mostrarBotonSiguiente = false;
                    ranuraActual = -1;

                    if (!snapshotsEfectoTurno.isEmpty()) {
                        for (SnapshotStats s : snapshotsEfectoTurno) s.revertir();
                        snapshotsEfectoTurno.clear();
                    }

                    contexto.revertirEfectosTurno();

                    if (!contexto.isInvocacionesIlimitadasEsteTurno()
                        && invocacionesTropaEsteTurno >= MAX_INVOC_TROPAS_TURNO) {
                        System.out.println("[INVOCACIÓN BLOQUEADA] Límite de invocaciones alcanzado.");
                    }

                    if (efectoEnRanuraJugador != null) {
                        System.out.println("[EFECTO] Finaliza duración del efecto: " + efectoEnRanuraJugador.getNombre());
                        efectoEnRanuraJugador = null;
                        efectoAplicadoEsteTurno = false;
                    }

                    if (turnoActual < MAX_TURNO) {
                        pasarSiguienteTurno();
                    } else {
                        evaluarFinalPorVidaTrasUltimoTurno();
                    }
                }
            }

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(1f, 1f, 0f, 0.3f));
            switch (ranuraActual) {
                case 0: shapeRenderer.rect(0, 230, 338, 630); break;
                case 1: shapeRenderer.rect(370, 230, 385, 630); break;
                case 2: shapeRenderer.rect(777, 230, 379, 630); break;
                case 3: shapeRenderer.rect(1175, 230, 376, 630); break;
                case 4: shapeRenderer.rect(1562, 230, 357, 630); break;
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void dibujarManoEfectos() {
        for (int i = 0; i < manoEfectos.size(); i++) {
            float x = (EFECTOS_BORDE_DER - ANCHO_CARTA) - (ANCHO_CARTA + ESPACIO_CARTAS) * i;
            float y = Y_CARTA_MANO_DERECHA;
            if (i == efectoHoverIndex) y += 20;
            batch.draw(manoEfectos.get(i).getImagen(), x, y, ANCHO_CARTA, ALTURA_CARTA);
        }
    }

    private void pasarSiguienteTurno() {
        if (turnoActual < MAX_TURNO) {
            turnoActual++;
            invocacionesTropaEsteTurno = 0;
            System.out.println("[TURNO] Avanzando al turno " + turnoActual);

            otorgarCartasPorTurno();
            verificarCondicionYTransicion();
        } else {
            System.out.println("[TURNO] Ya estamos en el turno final (" + turnoActual + "), no se avanza ni se roban cartas.");
            verificarCondicionYTransicion();
        }
    }

    private void otorgarCartasPorTurno() {
        if (!mazoTropasRestantes.isEmpty() && manoTropas.size() < MAX_CARTAS_MANO) {
            manoTropas.add(mazoTropasRestantes.remove(0));
        }
        if (turnoActual % 3 == 0 && !mazoEfectosRestantes.isEmpty() && manoEfectos.size() < MAX_CARTAS_MANO) {
            manoEfectos.add(mazoEfectosRestantes.remove(0));
        }

        System.out.println("[ROBO] Turno " + turnoActual + " -> Tropa? " +
            manoTropas.size() + "/" + MAX_CARTAS_MANO + " | Efectos " +
            manoEfectos.size() + "/" + MAX_CARTAS_MANO +
            " | Tropas restantes: " + mazoTropasRestantes.size() +
            " | Efectos restantes: " + mazoEfectosRestantes.size());
    }

    private boolean puedeInvocarPorNivel(CartaTropa carta) {
        if (contexto.isInvocacionLibreEsteTurno()) {
            return true;
        }
        if (turnoActual < 1 || turnoActual > nivelesPorTurno.size()) {
            return false;
        }
        List<Integer> nivelesPermitidos = nivelesPorTurno.get(turnoActual - 1);
        return nivelesPermitidos.contains(carta.getNivel());
    }

    private void verificarCondicionYTransicion() {
        if (partidaTerminada) return;

        int vidaPropia = contexto.getVidaPropia();
        int vidaEnemiga = contexto.getVidaEnemiga();

        if (vidaEnemiga <= 0 && vidaPropia > 0) {
            partidaTerminada = true;
            System.out.println("[PARTIDA] VICTORIA detectada - cambio a PantallaVictoria");
            juego.reproducirMusicaVictoria();
            juego.setScreen(new PantallaVictoria(juego));
            return;
        }

        if (vidaPropia <= 0 && vidaEnemiga > 0) {
            partidaTerminada = true;
            System.out.println("[PARTIDA] DERROTA detectada - cambio a PantallaDerrota");
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
            return;
        }

        if (vidaEnemiga <= 0 && vidaPropia <= 0) {
            partidaTerminada = true;
            System.out.println("[PARTIDA] Ambos a 0 simultáneamente: aplicando regla -> DERROTA");
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
        }
    }

    private void dibujarNivelesDisponibles() {
        if (turnoActual < 1 || turnoActual > nivelesPorTurno.size()) return;

        List<Integer> nivelesPermitidos = nivelesPorTurno.get(turnoActual - 1);
        if (nivelesPermitidos == null || nivelesPermitidos.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivelesPermitidos.size(); i++) {
            sb.append(nivelesPermitidos.get(i));
            if (i < nivelesPermitidos.size() - 1) sb.append(", ");
        }
        String texto = sb.toString();

        layoutNiveles.setText(fuenteNiveles, texto);

        float textX = AREA_NIVELES_X + (AREA_NIVELES_WIDTH - layoutNiveles.width) / 2f;
        float textY = AREA_NIVELES_Y + (AREA_NIVELES_HEIGHT + layoutNiveles.height) / 2f;

        fuenteNiveles.setColor(Color.WHITE);
        fuenteNiveles.draw(batch, layoutNiveles, textX, textY);
    }

    private void dibujarBarraVida() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(VIDA_X_JUGADOR, VIDA_Y, VIDA_BARRA_ANCHO, VIDA_BARRA_ALTO);
        shapeRenderer.setColor(Color.WHITE);
        float anchoPerdidoJugador = VIDA_BARRA_ANCHO * (1 - (float) contexto.getVidaPropia() / contexto.getVidaMaxima());
        shapeRenderer.rect(VIDA_X_JUGADOR, VIDA_Y, anchoPerdidoJugador, VIDA_BARRA_ALTO);

        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(VIDA_X_ENEMIGO, VIDA_Y, VIDA_BARRA_ANCHO, VIDA_BARRA_ALTO);
        shapeRenderer.setColor(Color.WHITE);
        float anchoPerdidoEnemigo = VIDA_BARRA_ANCHO * (1 - (float) contexto.getVidaEnemiga() / contexto.getVidaMaxima());
        shapeRenderer.rect(VIDA_X_ENEMIGO, VIDA_Y, anchoPerdidoEnemigo, VIDA_BARRA_ALTO);

        shapeRenderer.end();

        batch.begin();
        dibujarTextoVida(contexto.getVidaPropia(), VIDA_X_JUGADOR, VIDA_Y);
        dibujarTextoVida(contexto.getVidaEnemiga(), VIDA_X_ENEMIGO, VIDA_Y);
        batch.end();
    }

    private void dibujarTextoVida(int vida, int xBarra, int yBarra) {
        String texto = String.valueOf(vida);

        if (vida <= 20) {
            fuenteVida.setColor(new Color(1f, 0f, 0f, 1f));
        } else if (vida <= 50) {
            fuenteVida.setColor(new Color(0.8f, 0.66f, 0f, 1f));
        } else {
            fuenteVida.setColor(new Color(0f, 0.4f, 0f, 1f));
        }

        layout.setText(fuenteVida, texto);
        float textX = xBarra + (VIDA_BARRA_ANCHO - layout.width) / 2f;
        float textY = yBarra + (VIDA_BARRA_ALTO + layout.height) / 2f;
        fuenteVida.draw(batch, layout, textX, textY);
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, true); // mantiene centrado el mundo 1920×1080
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        fondo.dispose();
        imgSiguiente.dispose();
        fuenteVida.dispose();
        fuenteTurno.dispose();
        fuenteNiveles.dispose();
        if (vida2Img != null) vida2Img.dispose();
        if (vida3Img != null) vida3Img.dispose();
        if (vida4Img != null) vida4Img.dispose();

        for (CartaTropa c : manoTropas) if (c != null) c.dispose();
        for (CartaTropa c : mazoTropasRestantes) if (c != null) c.dispose();
        for (CartaEfecto e : manoEfectos) if (e != null) e.dispose();
        for (CartaEfecto e : mazoEfectosRestantes) if (e != null) e.dispose();
    }

    public ContextoBatalla getContexto() {
        return contexto;
    }

    public List<CartaEfecto> getEfectosDisponibles() {
        return efectosDisponibles;
    }

    private void evaluarFinalPorVidaTrasUltimoTurno() {
        if (partidaTerminada) return;

        int vidaPropia = contexto.getVidaPropia();
        int vidaEnemiga = contexto.getVidaEnemiga();

        if (vidaPropia <= 0 || vidaEnemiga <= 0) {
            verificarCondicionYTransicion();
            return;
        }

        if (vidaPropia > vidaEnemiga) {
            partidaTerminada = true;
            System.out.println("[PARTIDA] Fin de turno 22: gana el JUGADOR por tener más vida (" + vidaPropia + " > " + vidaEnemiga + ")");
            juego.reproducirMusicaVictoria();
            juego.setScreen(new PantallaVictoria(juego));
        } else if (vidaPropia < vidaEnemiga) {
            partidaTerminada = true;
            System.out.println("[PARTIDA] Fin de turno 22: gana el ENEMIGO por tener más vida (" + vidaEnemiga + " > " + vidaPropia + ")");
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
        } else {
            partidaTerminada = true;
            System.out.println("[PARTIDA] Fin de turno 22: empate perfecto de vida -> PANTALLA EMPATE");
            juego.reproducirMusicaEmpate();
            juego.setScreen(new PantallaEmpate(juego));
        }
    }
}
