package mijuego.picadoh.batalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
    private final Texture imgSiguiente;  // textura SIGUIENTE.png
    private Texture vida2Img;
    private Texture vida3Img;
    private Texture vida4Img;

    // Coordenadas/tamaño imágenes de vida (JUGADOR)
    private static final float VIDA_IMG_X = 347f;
    private static final float VIDA_IMG_Y = 469f;
    private static final float VIDA_IMG_W = 1908f - 1670f;
    private static final float VIDA_IMG_H = 623f - 469f;

    // Coordenadas/tamaño imágenes de vida (ENEMIGO)
    private static final float VIDA_ENE_IMG_X = 1671f;
    private static final float VIDA_ENE_IMG_Y = 469f;
    private static final float VIDA_ENE_IMG_W = 1915f - 1675f;
    private static final float VIDA_ENE_IMG_H = 623f - 469f;

    private final ContextoBatalla contexto;
    private final List<CartaEfecto> efectosDisponibles; // selección del jugador (7)

    // ======= Mano / Mazos =======
    private final List<CartaTropa> manoTropas;
    private final List<CartaEfecto> manoEfectos; // mano de efectos (se dibuja a la derecha)

    private final List<CartaTropa> mazoTropasRestantes;    // tropas por robar
    private final List<CartaEfecto> mazoEfectosRestantes;  // efectos por robar

    private CartaTropa cartaSeleccionada;
    private int cartaHoverIndex = -1;

    private float cartaDragX = 0;
    private float cartaDragY = 0;
    private boolean arrastrando = false;

    private final float ANCHO_CARTA = 100f;
    private final float ALTURA_CARTA = 150f;

    // Límite de cartas en mano
    private static final int MAX_CARTAS_MANO = 7;

    // Mano TROPAS (abajo-izquierda)
    private final float Y_CARTA_MANO = 40f;
    private final float TROPAS_X_INICIO = 0f;     // << arranca en la punta izquierda
    private final float ESPACIO_CARTAS = 10f;

    // Mano EFECTOS (abajo-derecha)
    private final float Y_CARTA_MANO_DERECHA = 40f;
    private final float EFECTOS_BORDE_DER = 1920f; // << anclado al borde derecho

    private final List<Ranura> ranuras;
    private final ShapeRenderer shapeRenderer;
    private Ranura ranuraHover = null;

    // Coordenadas y tamaño de las barras de vida
    private final int VIDA_BARRA_ANCHO = 153;
    private final int VIDA_BARRA_ALTO = 15;
    private final int VIDA_Y = 496;
    private final int VIDA_X_JUGADOR = 421;
    private final int VIDA_X_ENEMIGO = 1746;

    // Fuente para el texto de vida
    private final BitmapFont fuenteVida;
    private final GlyphLayout layout;

    // Variables para animación de batalla por ranuras
    private int ranuraActual = -1;
    private float tiempoHighlight = 0f;
    private final float DURACION_HIGHLIGHT = 0.5f;
    private boolean batallaEnCurso = false;

    // Control para mostrar imagen SIGUIENTE.png en botón play
    private boolean mostrarBotonSiguiente = false;

    private final int BOTON_PLAY_X = 870;
    private final int BOTON_PLAY_Y = 430;
    private final int BOTON_PLAY_ANCHO = 205;
    private final int BOTON_PLAY_ALTO = 220;

    // Sistema de turnos
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

    // Control de fin de partida
    private boolean partidaTerminada = false;

    public PantallaBatalla(Principal juego, ContextoBatalla contexto, List<CartaEfecto> efectosDisponibles) {
        this.juego = juego;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VALLEMISTICO.png"));
        this.imgSiguiente = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/SIGUIENTE.png"));
        vida2Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA2.png"));
        vida3Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA3.png"));
        vida4Img = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VIDA4.png"));
        this.contexto = contexto;
        this.efectosDisponibles = efectosDisponibles;

        // ======= Inicialización de manos y mazos =======
        this.manoTropas = new ArrayList<>();
        this.manoEfectos = new ArrayList<>();

        // Empezamos con todas las tropas disponibles del jugador
        List<CartaTropa> disponibles = new ArrayList<>(contexto.getTropasPropias());
        Collections.shuffle(disponibles);

        // Robar 3 iniciales a la mano con límite
        for (int i = 0; i < 3 && !disponibles.isEmpty() && manoTropas.size() < MAX_CARTAS_MANO; i++) {
            manoTropas.add(disponibles.remove(0));
        }
        // Lo que queda se convierte en el mazo de robo por turnos
        this.mazoTropasRestantes = new ArrayList<>(disponibles);

        // Efectos: todos van al mazo; mano empieza vacía
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

        // Ranuras propias (jugador)
        ranuras.add(new Ranura(36, 254, 267, 180, false));
        ranuras.add(new Ranura(437, 254, 267, 180, false));
        ranuras.add(new Ranura(833, 254, 267, 180, false));
        ranuras.add(new Ranura(1229, 254, 267, 180, false));
        ranuras.add(new Ranura(1615, 254, 270, 180, false));

        // Ranuras enemigas
        ranuras.add(new Ranura(22, 645, 283, 183, true));
        ranuras.add(new Ranura(412, 645, 286, 183, true));
        ranuras.add(new Ranura(813, 645, 286, 183, true));
        ranuras.add(new Ranura(1213, 645, 282, 183, true));
        ranuras.add(new Ranura(1615, 645, 283, 183, true));

        // Niveles por turno
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

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (batallaEnCurso || partidaTerminada) return false;

                int yInvertida = Gdx.graphics.getHeight() - screenY;

                // Botón PLAY invisible
                if (screenX >= BOTON_PLAY_X && screenX <= BOTON_PLAY_X + BOTON_PLAY_ANCHO &&
                    yInvertida >= BOTON_PLAY_Y && yInvertida <= BOTON_PLAY_Y + BOTON_PLAY_ALTO) {
                    iniciarBatallaConSiguiente();
                    System.out.println("[INPUT] Botón PLAY presionado → inicia batalla");
                    return true;
                }

                // Selección de carta en mano (solo TROPAS por ahora)
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
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
                if (batallaEnCurso || partidaTerminada) return false;

                if (arrastrando && cartaSeleccionada != null) {
                    cartaDragX = screenX - ANCHO_CARTA / 2;
                    cartaDragY = Gdx.graphics.getHeight() - screenY - ALTURA_CARTA / 2;

                    int mouseY = Gdx.graphics.getHeight() - screenY;
                    ranuraHover = null;
                    for (Ranura ranura : ranuras) {
                        if (ranura.contiene(screenX, mouseY) && (ranura.getCarta() == null)) {
                            ranuraHover = ranura;
                            break;
                        }
                    }
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (batallaEnCurso || partidaTerminada) return false;

                if (arrastrando && cartaSeleccionada != null) {
                    int mouseY = Gdx.graphics.getHeight() - screenY;
                    for (Ranura ranura : ranuras) {
                        if (ranura.contiene(screenX, mouseY) && ranura.getCarta() == null) {
                            if (puedeInvocarPorNivel(cartaSeleccionada)) {
                                ranura.setCarta(cartaSeleccionada);
                                if (!cartaSeleccionada.invocar()) {
                                    manoTropas.remove(cartaSeleccionada);
                                }
                            } else {
                                System.out.println("[INVOCACIÓN BLOQUEADA] No puedes invocar carta de nivel "
                                    + cartaSeleccionada.getNivel() + " en el turno " + turnoActual);
                            }
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
                if (batallaEnCurso || partidaTerminada) {
                    cartaHoverIndex = -1;
                    return false;
                }

                int yInvertida = Gdx.graphics.getHeight() - screenY;
                cartaHoverIndex = -1;
                for (int i = 0; i < manoTropas.size(); i++) {
                    float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
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

    // Método que inicia la batalla y muestra la imagen SIGUIENTE
    private void iniciarBatallaConSiguiente() {
        if (!batallaEnCurso) {
            ejecutarBatalla();
            mostrarBotonSiguiente = true;
        }
    }

    private void ejecutarBatalla() {
        System.out.println("[COMBATE] Iniciando animación de batalla...");

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

                int dañoEnemigo = cartaEnemigo.getAtaque();
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
                    contexto.restarVidaPropia(dañoRestante);
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
            contexto.restarVidaPropia(ranuraEnemigo.getCarta().getAtaque());
            System.out.println("[COMBATE] Ataque directo al jugador por " + ranuraEnemigo.getCarta().getAtaque() + " desde ranura enemiga " + (i + 1));
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

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int vidaUsuario = contexto.getVidaPropia();
        int vidaEnemigo = contexto.getVidaEnemiga();

        batch.begin();

        // Fondo
        batch.draw(fondo, 0, 0, 1920, 1080);

        // Cartas en ranuras
        for (Ranura ranura : ranuras) {
            if (ranura.getCarta() != null) {
                batch.draw(ranura.getCarta().getImagen(), ranura.getX(), ranura.getY(), ranura.getAncho(), ranura.getAlto());
            }
        }

        // Mano TROPAS (excepto la que se arrastra)
        for (int i = 0; i < manoTropas.size(); i++) {
            CartaTropa carta = manoTropas.get(i);
            if (carta == cartaSeleccionada) continue;

            float x = TROPAS_X_INICIO + i * (ANCHO_CARTA + ESPACIO_CARTAS);
            float y = Y_CARTA_MANO;
            if (i == cartaHoverIndex) y += 20;

            batch.draw(carta.getImagen(), x, y, ANCHO_CARTA, ALTURA_CARTA);
        }

        // Carta Tropa arrastrando
        if (arrastrando && cartaSeleccionada != null) {
            batch.draw(cartaSeleccionada.getImagen(), cartaDragX, cartaDragY, ANCHO_CARTA, ALTURA_CARTA);
        }

        // Mano EFECTOS (visual)
        dibujarManoEfectos();

        // Botón SIGUIENTE
        if (mostrarBotonSiguiente) {
            batch.draw(imgSiguiente, BOTON_PLAY_X, BOTON_PLAY_Y, BOTON_PLAY_ANCHO, BOTON_PLAY_ALTO);
        }

        // Turno
        String textoTurno = String.valueOf(turnoActual);
        layoutTurno.setText(fuenteTurno, textoTurno);
        float textX = TURNO_X + (TURNO_ANCHO - layoutTurno.width) / 2f;
        float textY = TURNO_Y + (TURNO_ALTO + layoutTurno.height) / 2f;
        fuenteTurno.setColor(Color.BLACK);
        fuenteTurno.draw(batch, layoutTurno, textX, textY);

        // Niveles disponibles
        dibujarNivelesDisponibles();

        // Imágenes de vida por umbral
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

        // Resaltado de ranura al arrastrar
        if (ranuraHover != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));
            shapeRenderer.rect(ranuraHover.getX(), ranuraHover.getY(), ranuraHover.getAncho(), ranuraHover.getAlto());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Animación por ranuras
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

                    if (turnoActual < MAX_TURNO) {
                        pasarSiguienteTurno();
                    } else {
                        // Turno 22 finalizado: decidir por vida
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

    // Dibuja la mano de efectos a la derecha (sin interacción por ahora)
    private void dibujarManoEfectos() {
        for (int i = 0; i < manoEfectos.size(); i++) {
            // Primera carta queda con su borde derecho en X=1920
            float x = (EFECTOS_BORDE_DER - ANCHO_CARTA) - (ANCHO_CARTA + ESPACIO_CARTAS) * i;
            float y = Y_CARTA_MANO_DERECHA;
            batch.draw(manoEfectos.get(i).getImagen(), x, y, ANCHO_CARTA, ALTURA_CARTA);
        }
    }

    private void pasarSiguienteTurno() {
        if (turnoActual < MAX_TURNO) {
            turnoActual++;
            System.out.println("[TURNO] Avanzando al turno " + turnoActual);

            // Robos SOLO una vez por turno y NUNCA después del último turno
            otorgarCartasPorTurno();

            // Revisión de estados después del robo
            verificarCondicionYTransicion();
        } else {
            // Ya en el turno final: no avanzar, no robar
            System.out.println("[TURNO] Ya estamos en el turno final (" + turnoActual + "), no se avanza ni se roban cartas.");
            verificarCondicionYTransicion();
        }
    }

    // Reglas de robo con límite de mano:
    // - Siempre 1 Tropa si hay espacio.
    // - Si el turno es múltiplo de 3: además 1 Efecto si hay espacio.
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

        // Barra jugador
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

    @Override public void resize(int width, int height) {}
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

        // Liberar cartas (si estas instancias son propiedad de esta pantalla)
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

        // Si ya se definió por 0 de vida, usa la lógica normal
        if (vidaPropia <= 0 || vidaEnemiga <= 0) {
            verificarCondicionYTransicion();
            return;
        }

        if (vidaPropia > vidaEnemiga) {
            // VICTORIA por vida
            partidaTerminada = true;
            System.out.println("[PARTIDA] Fin de turno 22: gana el JUGADOR por tener más vida (" + vidaPropia + " > " + vidaEnemiga + ")");
            juego.reproducirMusicaVictoria();
            juego.setScreen(new PantallaVictoria(juego));
        } else if (vidaPropia < vidaEnemiga) {
            // DERROTA por vida
            partidaTerminada = true;
            System.out.println("[PARTIDA] Fin de turno 22: gana el ENEMIGO por tener más vida (" + vidaEnemiga + " > " + vidaPropia + ")");
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
        } else {
            // Empate exacto -> Pantalla de empate
            partidaTerminada = true;
            System.out.println("[PARTIDA] Fin de turno 22: empate perfecto de vida -> PANTALLA EMPATE");
            juego.reproducirMusicaEmpate();
            juego.setScreen(new PantallaEmpate(juego));
        }
    }
}
