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

import mijuego.picadoh.Principal;
import mijuego.picadoh.cartas.CartaTropa;
import mijuego.picadoh.cartas.RegistroCartas;
import mijuego.picadoh.efectos.CartaEfecto;
import mijuego.picadoh.efectos.RegistroEfectos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla de batalla con sincronización LAN y reintentos de PLAY para evitar deadlocks.
 * - INVOKE / INVOKE_EFFECT se envían al soltar cartas/efectos.
 * - Al presionar PLAY se envía PLAY y se inicia reintento hasta recibir REVEAL.
 * - Al recibir REVEAL se aplica lo revelado y se ejecuta la fase de combate.
 * - Si no llega REVEAL tras N reintentos, se resuelve localmente (fallback offline).
 *
 * Cambios clave:
 * - Efectos sincronizados para ambos clientes.
 * - Duración de 1 turno con reversión garantizada.
 * - Limpieza visual del efecto en ambos lados al finalizar el combate.
 */
public class PantallaBatalla implements Screen {

    private final Principal juego;
    private final SpriteBatch batch;
    private final Texture fondo;
    private final Texture imgSiguiente;
    private Texture vida2Img;
    private Texture vida3Img;
    private Texture vida4Img;

    // Overlay “esperando rival” sobre el botón PLAY
    private Texture esperaPlayImg;
    private static final float ESPERA2_X = 880f;
    private static final float ESPERA2_Y = 471f;
    private static final float ESPERA2_W = 1067f - 880f; // 187
    private static final float ESPERA2_H = 613f  - 471f; // 141

    // Cámara + Viewport
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Vector3 tmpUnproject = new Vector3();

    // Iconos de vida parpadeo
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
    private int cartaSeleccionadaIndex = -1;
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

    // --- LAN estricto: sin fallback offline si hay cliente LAN ---
    private final boolean lanModoEstricto;
    private boolean revealRecibidoEsteTurno = false; // sólo para depurar/diagnosticar

    private boolean inputBloqueado = false;

    // Efecto propio y enemigo mostrados en la UI del turno actual
    private CartaEfecto efectoEnRanuraJugador = null;
    private CartaEfecto efectoEnRanuraEnemigo = null;

    // Flags de aplicación por turno
    private boolean efectoJugadorAplicadoEsteTurno = false;
    private boolean efectoEnemigoAplicadoEsteTurno = false;

    // Snapshots para revertir buff/debuff aplicados por efecto de turno (propio y enemigo)
    private static class SnapshotStats {
        final CartaTropa tropa;
        final int deltaAtk;
        final int deltaDef;
        SnapshotStats(CartaTropa t, int deltaAtk, int deltaDef) {
            this.tropa = t; this.deltaAtk = deltaAtk; this.deltaDef = deltaDef;
        }
        void revertir() {
            if (tropa == null) return;
            try { tropa.setAtaque(Math.max(0, tropa.getAtaque() - deltaAtk)); } catch (Throwable ignored) {}
            try { tropa.setDefensa(Math.max(0, tropa.getDefensa() - deltaDef)); } catch (Throwable ignored) {}
        }
    }
    private final List<SnapshotStats> snapshotsEfectoTurnoJugador = new ArrayList<>();
    private final List<SnapshotStats> snapshotsEfectoTurnoEnemigo = new ArrayList<>();

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

    // Barras de vida
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

    // Botón PLAY/SIGUIENTE
    private final int BOTON_PLAY_X = 870;
    private final int BOTON_PLAY_Y = 430;
    private final int BOTON_PLAY_ANCHO = 205;
    private final int BOTON_PLAY_ALTO = 220;

    // Turnos y niveles permitidos por turno
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

    // Ranura de efectos (rectángulos en tablero)
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

    // --- Estado LAN / reintentos ---
    private boolean esperandoReveal = false;      // esperando REVEAL del servidor
    private boolean clientListenerRegistered = false;
    private boolean playEnviadoEsteTurno = false; // evita múltiples envíos por clics repetidos
    private float tiempoDesdeEspera = 0f;         // tiempo acumulado desde que se empezó a esperar
    private float tiempoHastaReintento = 0f;      // contador para siguiente reintento
    private int reintentosRealizados = 0;
    private static final int MAX_REINTENTOS_PLAY = 12; // ~12 reintentos
    private static final float INTERVALO_REINTENTO = 1.0f; // cada 1 segundo

    public PantallaBatalla(Principal juego, ContextoBatalla contexto, List<CartaEfecto> efectosDisponibles) {
        this.juego = juego;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();

        // Mundo virtual 1920x1080
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(1920f, 1080f, camera);
        camera.position.set(960f, 540f, 0f);
        camera.update();

        this.fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VALLEMISTICO.png"));
        this.imgSiguiente = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/SIGUIENTE.png"));
        vida2Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA2.png"));
        vida3Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA3.png"));
        vida4Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA4.png"));
        this.esperaPlayImg = new Texture(Gdx.files.absolute("lwjgl3/assets/lan/ESPERA2.png"));

        this.contexto = contexto;
        this.efectosDisponibles = efectosDisponibles;

        this.lanModoEstricto = (juego.clienteLAN != null);

        this.manoTropas = new ArrayList<>();
        this.manoEfectos = new ArrayList<>();

        // Mano inicial: 3 tropas aleatorias
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

        // Fuente para turno (grande)
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("lwjgl3/assets/fonts/arial.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 48;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.minFilter = Texture.TextureFilter.Linear;
        this.fuenteTurno = generator.generateFont(parameter);
        this.fuenteTurno.setColor(Color.BLACK);
        generator.dispose();

        this.layoutTurno = new GlyphLayout();

        // Fuente para niveles permitidos (mediana)
        FreeTypeFontGenerator genNiv = new FreeTypeFontGenerator(Gdx.files.internal("lwjgl3/assets/fonts/arial.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter pNiv = new FreeTypeFontGenerator.FreeTypeFontParameter();
        pNiv.size = 28;
        pNiv.magFilter = Texture.TextureFilter.Linear;
        pNiv.minFilter = Texture.TextureFilter.Linear;
        this.fuenteNiveles = genNiv.generateFont(pNiv);
        this.fuenteNiveles.setColor(Color.WHITE);
        genNiv.dispose();

        // Ranuras jugador (0..4) y enemigo (5..9)
        ranuras = new ArrayList<>();
        // Jugador
        ranuras.add(new Ranura(36, 254, 267, 180, false));
        ranuras.add(new Ranura(437, 254, 267, 180, false));
        ranuras.add(new Ranura(833, 254, 267, 180, false));
        ranuras.add(new Ranura(1229, 254, 267, 180, false));
        ranuras.add(new Ranura(1615, 254, 270, 180, false));
        // Enemigo
        ranuras.add(new Ranura(22, 645, 283, 183, true));
        ranuras.add(new Ranura(412, 645, 286, 183, true));
        ranuras.add(new Ranura(813, 645, 286, 183, true));
        ranuras.add(new Ranura(1213, 645, 282, 183, true));
        ranuras.add(new Ranura(1615, 645, 283, 183, true));

        // Niveles permitidos por turno (1..22)
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

        // Input
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (inputBloqueado) return false;

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
                    onPlayButtonPressed();
                    return true;
                }
                return false;
            }

            private void unproj(int screenX, int screenY) {
                tmpUnproject.set(screenX, screenY, 0f);
                viewport.unproject(tmpUnproject);
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (inputBloqueado) return false;
                if (batallaEnCurso || partidaTerminada) return false;

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                if (wx >= BOTON_PLAY_X && wx <= BOTON_PLAY_X + BOTON_PLAY_ANCHO &&
                    wy >= BOTON_PLAY_Y && wy <= BOTON_PLAY_Y + BOTON_PLAY_ALTO) {
                    onPlayButtonPressed();
                    System.out.println("[INPUT] Botón PLAY presionado.");
                    return true;
                }

                // Efectos (mano derecha)
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

                // Tropas (mano inferior)
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
                    if (wx >= x && wx <= x + ANCHO_CARTA &&
                        wy >= Y_CARTA_MANO && wy <= Y_CARTA_MANO + ALTURA_CARTA + 30) {
                        cartaSeleccionada = manoTropas.get(i);
                        cartaSeleccionadaIndex = i;
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
                if (inputBloqueado) return false;

                if (batallaEnCurso || partidaTerminada) return false;

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                if (arrastrando && cartaSeleccionada != null) {
                    cartaDragX = wx - ANCHO_CARTA / 2f;
                    cartaDragY = wy - ALTURA_CARTA / 2f;

                    ranuraHover = null;
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
                    hoverRanuraEfectoJugador = (wx >= EFECTO_JUG_X1 && wx <= EFECTO_JUG_X2 &&
                        wy >= EFECTO_JUG_Y1 && wy <= EFECTO_JUG_Y2);
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (inputBloqueado) return false;

                if (batallaEnCurso || partidaTerminada) return false;

                unproj(screenX, screenY);
                float wx = tmpUnproject.x;
                float wy = tmpUnproject.y;

                if (arrastrando && cartaSeleccionada != null) {
                    // Invocar tropa en ranuras del jugador
                    for (int idx = 0; idx < 5; idx++) {
                        Ranura ranura = ranuras.get(idx);
                        if (ranura.contiene((int) wx, (int) wy) && ranura.getCarta() == null && !ranura.esEnemigo()) {

                            if (!contexto.isInvocacionesIlimitadasEsteTurno() &&
                                invocacionesTropaEsteTurno >= MAX_INVOC_TROPAS_TURNO) {
                                System.out.println("[INVOCACIÓN] Límite por turno alcanzado.");
                                break;
                            }

                            if (puedeInvocarPorNivel(cartaSeleccionada)) {
                                ranura.setCarta(cartaSeleccionada);

                                if (cartaSeleccionadaIndex >= 0 && cartaSeleccionadaIndex < manoTropas.size()
                                    && manoTropas.get(cartaSeleccionadaIndex) == cartaSeleccionada) {
                                    manoTropas.remove(cartaSeleccionadaIndex);
                                } else {
                                    manoTropas.remove(cartaSeleccionada);
                                }

                                invocacionesTropaEsteTurno++;

                                // Enviar INVOKE
                                try {
                                    if (juego.clienteLAN != null) {
                                        String className = cartaSeleccionada.getClass().getName();
                                        juego.clienteLAN.sendInvoke(idx, className);
                                        System.out.println("[LAN-SENT] INVOKE slot=" + idx + " class=" + className);
                                    }
                                } catch (Exception ex) {
                                    System.out.println("[LAN] Error al enviar INVOKE: " + ex.getMessage());
                                }
                            } else {
                                System.out.println("[INVOCACIÓN] Nivel no permitido para este turno.");
                            }
                            break;
                        }
                    }

                    cartaSeleccionada = null;
                    cartaSeleccionadaIndex = -1;
                    arrastrando = false;
                    ranuraHover = null;
                }

                if (arrastrandoEfecto && cartaEfectoSeleccionada != null) {
                    boolean colocado = false;
                    if (wx >= EFECTO_JUG_X1 && wx <= EFECTO_JUG_X2 &&
                        wy >= EFECTO_JUG_Y1 && wy <= EFECTO_JUG_Y2 &&
                        efectoEnRanuraJugador == null) {

                        efectoEnRanuraJugador = cartaEfectoSeleccionada;
                        colocado = true;

                        // Enviar INVOKE_EFFECT
                        try {
                            if (juego.clienteLAN != null) {
                                String className = efectoEnRanuraJugador.getClass().getName();
                                juego.clienteLAN.sendInvokeEffect(className);
                                System.out.println("[LAN-SENT] INVOKE_EFFECT class=" + className);
                            }
                        } catch (Exception ex) {
                            System.out.println("[LAN] Error al enviar INVOKE_EFFECT: " + ex.getMessage());
                        }

                        // Efectos instantáneos: se aplican ya mismo (al lado local)
                        if (efectoEnRanuraJugador.esInstantaneo()) {
                            try {
                                aplicarEfectoLado(true /*jugador*/, true /*instantaneo*/);
                                efectoJugadorAplicadoEsteTurno = true; // para que no se reaplique
                            } catch (Exception ex) {
                                System.out.println("[EFECTO] Error en aplicación instantánea: " + ex.getMessage());
                            }
                        }
                    }

                    if (!colocado) {
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
                if (inputBloqueado) return false;
                if (batallaEnCurso || partidaTerminada) {
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
    }

    // ---------------------- LÓGICA PLAY/REVEAL ----------------------

    private void onPlayButtonPressed() {
        if (partidaTerminada || batallaEnCurso) return;

        if (esperandoReveal) { // anti doble clic
            System.out.println("[PLAY] Ya esperando REVEAL. Ignorado.");
            return;
        }

        boolean sent = enviarPlay();
        if (!sent) {
            // Sólo permitimos fallback si NO hay LAN
            if (juego.clienteLAN == null) {
                iniciarBatallaConSiguiente();
            } else {
                System.out.println("[PLAY] Cliente LAN presente pero no se pudo enviar PLAY. Se seguirá esperando (sin fallback).");
            }
        }
    }

    private boolean enviarPlay() {
        try {
            if (juego.clienteLAN != null) {
                // IMPORTANTE: enviar vidas actuales
                juego.clienteLAN.sendPlay(contexto.getVidaPropia(), contexto.getVidaEnemiga());
                esperandoReveal = true;
                inputBloqueado = true;
                playEnviadoEsteTurno = true;
                reintentosRealizados = 0;
                tiempoDesdeEspera = 0f;
                tiempoHastaReintento = INTERVALO_REINTENTO;
                mostrarBotonSiguiente = false;
                revealRecibidoEsteTurno = false;
                System.out.println("[CLIENTE-LAN] PLAY enviado. Esperando REVEAL...");
                return true;
            }
        } catch (Exception ex) {
            System.out.println("[CLIENTE-LAN] Error al enviar PLAY: " + ex.getMessage());
        }
        return false;
    }

    private void iniciarBatallaConSiguiente() {
        if (!batallaEnCurso) {
            // Limpieza de flags LAN antes de iniciar animación/combate
            esperandoReveal = false;
            inputBloqueado = false;
            playEnviadoEsteTurno = false;
            reintentosRealizados = 0;
            tiempoDesdeEspera = 0f;
            tiempoHastaReintento = 0f;

            ejecutarBatalla();
            mostrarBotonSiguiente = true;
        }
    }

    // Aplica el efecto del lado indicado (true: jugador / false: enemigo).
    // Si instantaneo=true se saltea la toma de snapshots (ya que el efecto podría limpiar tablero etc.)
    private void aplicarEfectoLado(boolean jugador, boolean instantaneo) {
        CartaEfecto efecto = jugador ? efectoEnRanuraJugador : efectoEnRanuraEnemigo;
        if (efecto == null) return;

        List<SnapshotStats> snapshots = jugador ? snapshotsEfectoTurnoJugador : snapshotsEfectoTurnoEnemigo;
        snapshots.clear();

        int start = jugador ? 0 : 5;
        int end   = jugador ? 5 : 10;

        for (int i = start; i < end; i++) {
            CartaTropa t = ranuras.get(i).getCarta();
            if (t == null) continue;

            int atkAntes = t.getAtaque();
            int defAntes = t.getDefensa();

            // Aplicamos con la perspectiva apropiada para que el efecto impacte en el lado correcto
            if (jugador) {
                contexto.applyEffectAsPlayer(efecto);
            } else {
                contexto.applyEffectAsEnemy(efecto);
            }

            // Si el efecto dependía de una selección, informamos qué tropa se está afectando
            contexto.setTropaSeleccionada(t);
            try {
                // Muchos efectos usan getTropaSeleccionada(); garantizamos que esté seteado.
                efecto.aplicarEfecto(contexto);
            } catch (Exception ex) {
                System.out.println("[EFECTO] Error aplicando efecto en " + (jugador ? "jugador" : "enemigo") + ": " + ex.getMessage());
            } finally {
                contexto.setTropaSeleccionada(null);
            }

            if (!instantaneo) {
                int deltaAtk = t.getAtaque() - atkAntes;
                int deltaDef = t.getDefensa() - defAntes;
                snapshots.add(new SnapshotStats(t, deltaAtk, deltaDef));
            }
        }

        // Limpiezas globales solicitadas por efectos (purga / limpiar campo)
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
        }
        if (contexto.isLimpiarCampoSolicitado()) {
            for (int i = 0; i < ranuras.size(); i++) ranuras.get(i).setCarta(null);
            contexto.setLimpiarCampoSolicitado(false);
        }
    }

    private void ejecutarBatalla() {
        // 1) Aplicar efectos de ambos lados si aún no se aplicaron este turno.
        if (efectoEnRanuraJugador != null && !efectoJugadorAplicadoEsteTurno) {
            try {
                aplicarEfectoLado(true, false);
                efectoJugadorAplicadoEsteTurno = true;
            } catch (Exception e) {
                System.out.println("[EFECTO] Error aplicando efecto del jugador: " + e.getMessage());
            }
        }
        if (efectoEnRanuraEnemigo != null && !efectoEnemigoAplicadoEsteTurno) {
            try {
                aplicarEfectoLado(false, false);
                efectoEnemigoAplicadoEsteTurno = true;
            } catch (Exception e) {
                System.out.println("[EFECTO] Error aplicando efecto del enemigo: " + e.getMessage());
            }
        }

        // 2) Iniciar animación/combate por columnas
        ranuraActual = 0;
        tiempoHighlight = 0f;
        batallaEnCurso = true;
        System.out.println("[COMBATE] Animación iniciada.");
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
                } else {
                    cartaEnemigo.setDefensa(nuevaDefEnemigo);
                }

                if (nuevaDefJugador <= 0) {
                    int dañoRestante = -nuevaDefJugador;
                    if (dañoRestante > 0) contexto.restarVidaPropia(dañoRestante);
                    ranuraJugador.setCarta(null);
                } else {
                    cartaJugador.setDefensa(nuevaDefJugador);
                }

            } else {
                contexto.restarVidaEnemiga(cartaJugador.getAtaque());
            }
        }

        if (ranuraEnemigo.getCarta() != null && ranuraJugador.getCarta() == null) {
            int dano = contexto.isAtaquesEnemigosAnuladosEsteTurno() ? 0 : ranuraEnemigo.getCarta().getAtaque();
            if (dano > 0) contexto.restarVidaPropia(dano);
        }

        verificarCondicionYTransicion();
    }

    // ---------------------- Ciclo de vida Screen ----------------------

    @Override
    public void show() {
        juego.detenerMusicaSeleccion();
        juego.reproducirMusicaBatalla();
        registerClientListenerIfNeeded();
    }

    @Override
    public void render(float delta) {
        if (partidaTerminada) return;

        // Esperando REVEAL: reintentos + Fallback para no quedar colgados
        if (esperandoReveal && juego.clienteLAN != null && !batallaEnCurso) {
            tiempoDesdeEspera += delta;
            tiempoHastaReintento -= delta;

            if (tiempoHastaReintento <= 0f && reintentosRealizados < MAX_REINTENTOS_PLAY) {
                try {
                    // [SYNC VIDA] Reenviar también vidas si existe ese overload
                    try {
                        int vidaP = contexto.getVidaPropia();
                        int vidaE = contexto.getVidaEnemiga();
                        juego.clienteLAN.getClass()
                            .getMethod("sendPlay", int.class, int.class)
                            .invoke(juego.clienteLAN, vidaP, vidaE);
                        System.out.println("[CLIENTE-LAN] Reenvío PLAY con vidas P=" + vidaP + " E=" + vidaE +
                            " (" + (reintentosRealizados+1) + "/" + MAX_REINTENTOS_PLAY + ")");
                    } catch (Throwable noOverload) {
                        juego.clienteLAN.sendPlay();
                        System.out.println("[CLIENTE-LAN] Reenvío PLAY (sin vidas por overload) " +
                            "(" + (reintentosRealizados+1) + "/" + MAX_REINTENTOS_PLAY + ")");
                    }

                    reintentosRealizados++;
                    tiempoHastaReintento = INTERVALO_REINTENTO;
                } catch (Exception ex) {
                    System.out.println("[CLIENTE-LAN] Error reenvío PLAY: " + ex.getMessage());
                }
            }
            if (reintentosRealizados >= MAX_REINTENTOS_PLAY) {
                if (lanModoEstricto) {
                    // Modo estricto: NO resolver localmente; seguimos esperando.
                    // Sólo reintentar y mostrar overlay.
                    tiempoHastaReintento = INTERVALO_REINTENTO;
                    System.out.println("[CLIENTE-LAN] Timeout REVEAL (modo estricto). Se sigue esperando sin fallback.");
                } else {
                    System.out.println("[CLIENTE-LAN] Timeout de REVEAL: forzando resolución local (fallback OFFLINE).");
                    esperandoReveal = false;
                    reintentosRealizados = 0;
                    tiempoDesdeEspera = 0f;
                    tiempoHastaReintento = 0f;
                    if (!batallaEnCurso) iniciarBatallaConSiguiente();
                }
            }
        }
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int vidaUsuario = contexto.getVidaPropia();
        int vidaEnemigo = contexto.getVidaEnemiga();

        batch.begin();
        batch.draw(fondo, 0, 0, 1920, 1080);

        // Campo: ranuras
        for (Ranura ranura : ranuras) {
            if (ranura.getCarta() != null) {
                batch.draw(ranura.getCarta().getImagen(), ranura.getX(), ranura.getY(), ranura.getAncho(), ranura.getAlto());
            }
        }

        // Mano de tropas (abajo)
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

        // Mano de efectos (derecha)
        dibujarManoEfectos();

        // Ranura efecto jugador/enemigo
        if (efectoEnRanuraJugador != null) {
            batch.draw(efectoEnRanuraJugador.getImagen(), EFECTO_JUG_X1, EFECTO_JUG_Y1, EFECTO_FULL_W, EFECTO_FULL_H);
        }
        if (efectoEnRanuraEnemigo != null) {
            batch.draw(efectoEnRanuraEnemigo.getImagen(), EFECTO_ENE_X1, EFECTO_ENE_Y1, EFECTO_FULL_W, EFECTO_FULL_H);
        }

        if (arrastrandoEfecto && cartaEfectoSeleccionada != null) {
            batch.draw(cartaEfectoSeleccionada.getImagen(), efectoDragX, efectoDragY, ANCHO_CARTA, ALTURA_CARTA);
        }

        // Botón SIGUIENTE (en modo offline o tras combate)
        if (mostrarBotonSiguiente) {
            batch.draw(imgSiguiente, BOTON_PLAY_X, BOTON_PLAY_Y, BOTON_PLAY_ANCHO, BOTON_PLAY_ALTO);
        }

        // Overlay "esperando" cuando se envió PLAY y aún no llegó REVEAL
        if (esperandoReveal && esperaPlayImg != null) {
            batch.draw(esperaPlayImg, ESPERA2_X, ESPERA2_Y, ESPERA2_W, ESPERA2_H);
        }

        // Texto de turno
        String textoTurno = String.valueOf(turnoActual);
        layoutTurno.setText(fuenteTurno, textoTurno);
        float textX = TURNO_X + (TURNO_ANCHO - layoutTurno.width) / 2f;
        float textY = TURNO_Y + (TURNO_ALTO + layoutTurno.height) / 2f;
        fuenteTurno.setColor(Color.BLACK);
        fuenteTurno.draw(batch, layoutTurno, textX, textY);

        // Niveles permitidos
        dibujarNivelesDisponibles();

        // Iconos de vida (parpadeo)
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

        // Barras de vida + números
        dibujarBarraVida();

        // Resaltado de ranura hover al arrastrar carta
        if (ranuraHover != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));
            shapeRenderer.rect(ranuraHover.getX(), ranuraHover.getY(), ranuraHover.getAncho(), ranuraHover.getAlto());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Resaltado de ranura de efecto al arrastrar efecto
        if (arrastrandoEfecto && hoverRanuraEfectoJugador) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));
            shapeRenderer.rect(EFECTO_JUG_X1, EFECTO_JUG_Y1, EFECTO_FULL_W, EFECTO_FULL_H);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Fase de combate: highlight por columnas + resolución por columnas
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

                    // Revertir buffs temporales aplicados por efecto de turno (ambos lados)
                    if (!snapshotsEfectoTurnoJugador.isEmpty()) {
                        for (SnapshotStats s : snapshotsEfectoTurnoJugador) s.revertir();
                        snapshotsEfectoTurnoJugador.clear();
                    }
                    if (!snapshotsEfectoTurnoEnemigo.isEmpty()) {
                        for (SnapshotStats s : snapshotsEfectoTurnoEnemigo) s.revertir();
                        snapshotsEfectoTurnoEnemigo.clear();
                    }

                    // Revertir flags/solicitudes de efecto de ContextoBatalla
                    contexto.revertirEfectosTurno();

                    // Limpiar ranuras de efecto (duran 1 turno) y flags de aplicación
                    if (efectoEnRanuraJugador != null) efectoEnRanuraJugador = null;
                    if (efectoEnRanuraEnemigo != null) efectoEnRanuraEnemigo = null;
                    efectoJugadorAplicadoEsteTurno = false;
                    efectoEnemigoAplicadoEsteTurno = false;

                    // Control de invocaciones
                    if (!contexto.isInvocacionesIlimitadasEsteTurno()
                        && invocacionesTropaEsteTurno >= MAX_INVOC_TROPAS_TURNO) {
                        System.out.println("[INVOCACIÓN] Límite de invocaciones alcanzado.");
                    }

                    if (turnoActual < MAX_TURNO) {
                        pasarSiguienteTurno();
                    } else {
                        evaluarFinalPorVidaTrasUltimoTurno();
                    }
                }
            }

            // Pintar highlight columna actual
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

            // Reset flags de turno LAN
            esperandoReveal = false;
            playEnviadoEsteTurno = false;
            reintentosRealizados = 0;
            tiempoDesdeEspera = 0f;
            tiempoHastaReintento = 0f;

            otorgarCartasPorTurno();
            verificarCondicionYTransicion();
        } else {
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
    }

    private boolean puedeInvocarPorNivel(CartaTropa carta) {
        if (contexto.isInvocacionLibreEsteTurno()) return true;
        if (turnoActual < 1 || turnoActual > nivelesPorTurno.size()) return false;
        List<Integer> nivelesPermitidos = nivelesPorTurno.get(turnoActual - 1);
        return nivelesPermitidos.contains(carta.getNivel());
    }

    private void verificarCondicionYTransicion() {
        if (partidaTerminada) return;

        int vidaPropia = contexto.getVidaPropia();
        int vidaEnemiga = contexto.getVidaEnemiga();

        if (vidaEnemiga <= 0 && vidaPropia > 0) {
            partidaTerminada = true;
            juego.reproducirMusicaVictoria();
            juego.setScreen(new PantallaVictoria(juego));
            return;
        }

        if (vidaPropia <= 0 && vidaEnemiga > 0) {
            partidaTerminada = true;
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
            return;
        }

        if (vidaEnemiga <= 0 && vidaPropia <= 0) {
            partidaTerminada = true;
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

        // Barra jugador (negro = fondo / blanco = daño recibido)
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(VIDA_X_JUGADOR, VIDA_Y, VIDA_BARRA_ANCHO, VIDA_BARRA_ALTO);
        shapeRenderer.setColor(Color.WHITE);
        float anchoPerdidoJugador = VIDA_BARRA_ANCHO * (1 - (float) contexto.getVidaPropia() / contexto.getVidaMaxima());
        shapeRenderer.rect(VIDA_X_JUGADOR, VIDA_Y, anchoPerdidoJugador, VIDA_BARRA_ALTO);

        // Barra enemigo
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

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
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
        if (esperaPlayImg != null) esperaPlayImg.dispose();

        for (CartaTropa c : manoTropas) if (c != null) c.dispose();
        for (CartaTropa c : mazoTropasRestantes) if (c != null) c.dispose();
        for (CartaEfecto e : manoEfectos) if (e != null) e.dispose();
        for (CartaEfecto e : mazoEfectosRestantes) if (e != null) e.dispose();
    }

    // ----------------- LAN: Listener y aplicación de REVEAL -----------------

    private void registerClientListenerIfNeeded() {
        if (clientListenerRegistered) return;
        if (juego.clienteLAN == null) return;

        juego.clienteLAN.setOnMessage(json -> {
            String type = null;
            try { type = json.get("type").getAsString(); } catch (Exception ignored) {}
            if (type != null) type = type.trim().toUpperCase();

            if ("MATCHED".equals(type)) {
                System.out.println("[LAN-RECV] " + json.toString());
                return;
            }

            if ("REVEAL".equals(type)) {
                Gdx.app.postRunnable(() -> {
                    System.out.println("[LAN-RECV] REVEAL: " + json.toString());

                    // 1) PISAR VIDAS PRIMERO con lo que diga el servidor
                    try {
                        if (json.has("vidaP")) contexto.setVidaPropia(json.get("vidaP").getAsInt());
                        if (json.has("vidaE")) contexto.setVidaEnemiga(json.get("vidaE").getAsInt());
                        System.out.println("[SYNC VIDA] Jugador=" + contexto.getVidaPropia() +
                            " / Enemigo=" + contexto.getVidaEnemiga());
                    } catch (Exception e) {
                        System.out.println("[SYNC VIDA] Error al sincronizar vidas del REVEAL: " + e.getMessage());
                    }

                    // 2) Aplicar invocaciones y efectos revelados por el servidor
                    try {
                        if (json.has("playerInvokes"))        applyPlayerInvokesFromJsonArray(json.getAsJsonArray("playerInvokes"));
                        if (json.has("enemyInvokes"))         applyEnemyInvokesFromJsonArray(json.getAsJsonArray("enemyInvokes"));
                        if (json.has("playerEffectInvokes"))  applyPlayerEffectInvokesFromJsonArray(json.getAsJsonArray("playerEffectInvokes"));
                        if (json.has("enemyEffectInvokes"))   applyEnemyEffectInvokesFromJsonArray(json.getAsJsonArray("enemyEffectInvokes"));
                    } catch (Exception e) {
                        System.out.println("[LAN-RECV] Error aplicando REVEAL: " + e.getMessage());
                    }

                    // 3) Salir del estado de espera
                    esperandoReveal = false;
                    reintentosRealizados = 0;
                    tiempoDesdeEspera = 0f;
                    tiempoHastaReintento = 0f;
                    revealRecibidoEsteTurno = true;

                    // 4) (MUY IMPORTANTE) Si ya hay fin de partida con estas vidas, terminar sin animar
                    if (!partidaTerminada) {
                        int vp = contexto.getVidaPropia();
                        int ve = contexto.getVidaEnemiga();

                        if (ve <= 0 && vp > 0) {
                            partidaTerminada = true;
                            juego.reproducirMusicaVictoria();
                            juego.setScreen(new PantallaVictoria(juego));
                            return;
                        }
                        if (vp <= 0 && ve > 0) {
                            partidaTerminada = true;
                            juego.reproducirMusicaDerrota();
                            juego.setScreen(new PantallaDerrota(juego));
                            return;
                        }
                        if (vp <= 0 && ve <= 0) {
                            partidaTerminada = true;
                            juego.reproducirMusicaDerrota();
                            juego.setScreen(new PantallaDerrota(juego));
                            return;
                        }
                    }

                    // 5) Iniciar animación de combate del turno (ambos clientes parten del MISMO estado)
                    if (!batallaEnCurso && !partidaTerminada) {
                        iniciarBatallaConSiguiente();
                    }
                });
                return;
            }


            if ("OPPONENT_DISCONNECTED".equals(type)) {
                System.out.println("[CLIENTE-LAN] Rival desconectado.");
            }
        });

        clientListenerRegistered = true;
    }

    private void applyPlayerInvokesFromJsonArray(JsonArray arr) {
        if (arr == null) return;
        for (JsonElement e : arr) {
            try {
                JsonObject o = e.getAsJsonObject();
                int slot = o.has("slot") ? o.get("slot").getAsInt() : -1;
                String cls = o.has("class") ? o.get("class").getAsString() : null;
                if (slot >= 0 && cls != null && slot < 5) {
                    CartaTropa instancia = createTropaInstanceByName(cls);
                    if (instancia != null) {
                        ranuras.get(slot).setCarta(instancia);
                        System.out.println("[LAN-APPLY] player invoke slot=" + slot + " cls=" + cls);
                    }
                }
            } catch (Exception ex) {
                System.out.println("[LAN-ERR] applyPlayerInvokes: " + ex.getMessage());
            }
        }
    }

    private void applyEnemyInvokesFromJsonArray(JsonArray arr) {
        if (arr == null) return;
        for (JsonElement e : arr) {
            try {
                JsonObject o = e.getAsJsonObject();
                int slot = o.has("slot") ? o.get("slot").getAsInt() : -1;
                String cls = o.has("class") ? o.get("class").getAsString() : null;
                if (slot >= 0 && cls != null && slot < 5) {
                    int enemyIndex = slot + 5;
                    CartaTropa instancia = createTropaInstanceByName(cls);
                    if (instancia != null) {
                        ranuras.get(enemyIndex).setCarta(instancia);
                        System.out.println("[LAN-APPLY] enemy invoke enemySlot=" + enemyIndex + " cls=" + cls);
                    }
                }
            } catch (Exception ex) {
                System.out.println("[LAN-ERR] applyEnemyInvokes: " + ex.getMessage());
            }
        }
    }

    private void applyPlayerEffectInvokesFromJsonArray(JsonArray arr) {
        if (arr == null) return;
        for (JsonElement e : arr) {
            try {
                JsonObject o = e.getAsJsonObject();
                String cls = o.has("class") ? o.get("class").getAsString() : null;
                if (cls != null) {
                    CartaEfecto efecto = createEfectoInstanceByName(cls);
                    if (efecto != null) {
                        efectoEnRanuraJugador = efecto;
                        System.out.println("[LAN-APPLY] player effect " + cls);

                        if (efecto.esInstantaneo()) {
                            // Aplicar inmediatamente del lado local
                            aplicarEfectoLado(true, true);
                            efectoJugadorAplicadoEsteTurno = true;
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("[LAN-ERR] applyPlayerEffectInvokes: " + ex.getMessage());
            }
        }
    }

    private void applyEnemyEffectInvokesFromJsonArray(JsonArray arr) {
        if (arr == null) return;
        for (JsonElement e : arr) {
            try {
                JsonObject o = e.getAsJsonObject();
                String cls = o.has("class") ? o.get("class").getAsString() : null;
                if (cls != null) {
                    CartaEfecto efecto = createEfectoInstanceByName(cls);
                    if (efecto != null) {
                        if (efectoEnRanuraEnemigo == null) {
                            efectoEnRanuraEnemigo = efecto;
                            System.out.println("[LAN-APPLY] enemy effect " + cls);

                            if (efecto.esInstantaneo()) {
                                // Aplicar inmediatamente del lado enemigo (desde mi perspectiva)
                                aplicarEfectoLado(false, true);
                                efectoEnemigoAplicadoEsteTurno = true;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("[LAN-ERR] applyEnemyEffectInvokes: " + ex.getMessage());
            }
        }
    }

    private CartaTropa createTropaInstanceByName(String name) {
        if (name == null) return null;
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

        System.out.println("[LAN] No se pudo crear tropa: " + name);
        return null;
    }

    private CartaEfecto createEfectoInstanceByName(String name) {
        if (name == null) return null;
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

        System.out.println("[LAN] No se pudo crear efecto: " + name);
        return null;
    }

    /**
     * Evalúa el resultado de la partida cuando se llega al último turno (MAX_TURNO)
     * y ninguno de los dos jugadores ha muerto todavía.
     */
    private void evaluarFinalPorVidaTrasUltimoTurno() {
        int vidaPropia = contexto.getVidaPropia();
        int vidaEnemiga = contexto.getVidaEnemiga();

        if (vidaPropia > vidaEnemiga) {
            partidaTerminada = true;
            juego.reproducirMusicaVictoria();
            juego.setScreen(new PantallaVictoria(juego));
            System.out.println("[FIN] Victoria por mayor vida tras el último turno.");
        } else if (vidaEnemiga > vidaPropia) {
            partidaTerminada = true;
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
            System.out.println("[FIN] Derrota por menor vida tras el último turno.");
        } else {
            partidaTerminada = true;
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
            System.out.println("[FIN] Empate: ambos con la misma vida.");
        }
    }
}
