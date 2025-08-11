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
    private int turnoActual = 1;


    private final List<List<Integer>> nivelesPorTurno = new ArrayList<>();


    private final BitmapFont fuenteTurno;
    private final GlyphLayout layoutTurno;


    private final BitmapFont fuenteNiveles;
    private final GlyphLayout layoutNiveles = new GlyphLayout();

    private final int TURNO_X = 727;
    private final int TURNO_Y = 515; // base Y para dibujo
    private final int TURNO_ANCHO = 68; // 795 - 727
    private final int TURNO_ALTO = 65;  // 580 - 515


    private final float AREA_NIVELES_X = 1105f;
    private final float AREA_NIVELES_WIDTH = 157f;
    private final float AREA_NIVELES_Y = 498f;
    private final float AREA_NIVELES_HEIGHT = 73f;

    // NUEVO: control de fin de partida para no reentrar múltiples veces
    private boolean partidaTerminada = false;


    public PantallaBatalla(Principal juego, ContextoBatalla contexto, List<CartaEfecto> efectosDisponibles) {
        this.juego = juego;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.fondo = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/VALLEMISTICO.png"));
        this.imgSiguiente = new Texture(Gdx.files.absolute("lwjgl3/assets/campos/SIGUIENTE.png"));
        this.contexto = contexto;
        this.efectosDisponibles = efectosDisponibles;
        this.manoTropas = new ArrayList<>();

        this.fuenteVida = new BitmapFont();
        this.layout = new GlyphLayout();


        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("lwjgl3/assets/fonts/arial.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 48;  // Tamaño de fuente en pixeles para nitidez
        parameter.magFilter = Texture.TextureFilter.Linear;   // filtrado para suavizar
        parameter.minFilter = Texture.TextureFilter.Linear;
        this.fuenteTurno = generator.generateFont(parameter);

        this.fuenteTurno.setColor(Color.BLACK);
        generator.dispose();

        this.layoutTurno = new GlyphLayout();


        FreeTypeFontGenerator genNiv = new FreeTypeFontGenerator(Gdx.files.internal("lwjgl3/assets/fonts/arial.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter pNiv = new FreeTypeFontGenerator.FreeTypeFontParameter();
        pNiv.size = 28; // ajusta tamaño si es necesario
        pNiv.magFilter = Texture.TextureFilter.Linear;
        pNiv.minFilter = Texture.TextureFilter.Linear;
        this.fuenteNiveles = genNiv.generateFont(pNiv);
        this.fuenteNiveles.setColor(Color.WHITE);
        genNiv.dispose();

        // Se agregan 3 cartas aleatorias a la mano del jugador al inicio
        List<CartaTropa> disponibles = new ArrayList<>(contexto.getTropasPropias());
        Collections.shuffle(disponibles);
        for (int i = 0; i < 3 && i < disponibles.size(); i++) {
            manoTropas.add(disponibles.get(i));
        }

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

        // Inicialización de niveles permitidos por turno (índice 0 = turno 1)
        nivelesPorTurno.add(List.of(1));            // Turno 1
        nivelesPorTurno.add(List.of(1, 2));         // Turno 2
        nivelesPorTurno.add(List.of(1, 2));         // Turno 3
        nivelesPorTurno.add(List.of(1, 2, 3));      // Turno 4
        nivelesPorTurno.add(List.of(1, 2, 3));      // Turno 5
        nivelesPorTurno.add(List.of(2, 3, 4));      // Turno 6
        nivelesPorTurno.add(List.of(2, 3, 4));      // Turno 7
        nivelesPorTurno.add(List.of(2, 4));         // Turno 8
        nivelesPorTurno.add(List.of(3, 4));         // Turno 9
        nivelesPorTurno.add(List.of(4, 5));         // Turno 10
        nivelesPorTurno.add(List.of(4, 5));         // Turno 11
        nivelesPorTurno.add(List.of(5));            // Turno 12
        nivelesPorTurno.add(List.of(1, 5));         // Turno 13
        nivelesPorTurno.add(List.of(1, 2, 5));      // Turno 14
        nivelesPorTurno.add(List.of(1, 2, 3));      // Turno 15
        nivelesPorTurno.add(List.of(1, 2, 3, 4));   // Turno 16
        nivelesPorTurno.add(List.of(1, 3));         // Turno 17
        nivelesPorTurno.add(List.of(2, 4));         // Turno 18
        nivelesPorTurno.add(List.of(3, 5));         // Turno 19
        nivelesPorTurno.add(List.of(1, 2, 3, 4, 5));// Turno 20
        nivelesPorTurno.add(List.of(1, 2, 3, 4, 5));// Turno 21
        nivelesPorTurno.add(List.of(1, 2, 3, 4, 5));// Turno 22

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (partidaTerminada) return false; // bloquear inputs si ya terminó la partida
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
                if (batallaEnCurso || partidaTerminada) return false; // BLOQUEO durante batalla o si la partida terminó

                int yInvertida = Gdx.graphics.getHeight() - screenY;

                // Botón PLAY invisible
                if (screenX >= BOTON_PLAY_X && screenX <= BOTON_PLAY_X + BOTON_PLAY_ANCHO &&
                    yInvertida >= BOTON_PLAY_Y && yInvertida <= BOTON_PLAY_Y + BOTON_PLAY_ALTO) {
                    iniciarBatallaConSiguiente();
                    System.out.println("[INPUT] Botón PLAY presionado → inicia batalla");
                    return true;
                }

                // Selección de carta en mano
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
                if (batallaEnCurso || partidaTerminada) return false; // BLOQUEO durante batalla o fin de partida

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
                if (batallaEnCurso || partidaTerminada) return false; // BLOQUEO durante batalla o fin de partida

                if (arrastrando && cartaSeleccionada != null) {
                    int mouseY = Gdx.graphics.getHeight() - screenY;
                    for (Ranura ranura : ranuras) {
                        if (ranura.contiene(screenX, mouseY) && ranura.getCarta() == null) {
                            // NUEVO: validación por niveles/turno
                            if (puedeInvocarPorNivel(cartaSeleccionada)) {
                                ranura.setCarta(cartaSeleccionada);
                                if (!cartaSeleccionada.invocar()) {
                                    manoTropas.remove(cartaSeleccionada);
                                }
                            } else {
                                System.out.println("[INVOCACIÓN BLOQUEADA] No puedes invocar carta de nivel "
                                    + cartaSeleccionada.getNivel() + " en el turno " + turnoActual);
                                // Opcional: aquí agregar feedback visual/sonoro
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
                    return false; // no mostrar hover durante batalla o fin de partida
                }

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

        // VERIFICAR CONDICIONES DE VICTORIA/DERROTA inmediatamente después de aplicar daño
        verificarCondicionYTransicion();
    }

    @Override
    public void show() {
        juego.detenerMusicaSeleccion();
        juego.reproducirMusicaBatalla();
    }

    @Override
    public void render(float delta) {
        // Si la partida terminó, el flujo normal ya estará interrumpido porque se hizo setScreen desde verificarCondicionYTransicion.
        // Aún así chequeamos partidaTerminada para no ejecutar lógica adicional.
        if (partidaTerminada) return;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(fondo, 0, 0, 1920, 1080);

        // Dibuja cartas en ranuras
        for (Ranura ranura : ranuras) {
            if (ranura.getCarta() != null) {
                batch.draw(ranura.getCarta().getImagen(), ranura.getX(), ranura.getY(), ranura.getAncho(), ranura.getAlto());
            }
        }

        // Dibuja cartas en mano (excepto la que se está arrastrando)
        for (int i = 0; i < manoTropas.size(); i++) {
            CartaTropa carta = manoTropas.get(i);
            if (carta == cartaSeleccionada) continue;

            float x = 40 + i * (ANCHO_CARTA + 10);
            float y = Y_CARTA_MANO;
            if (i == cartaHoverIndex) y += 20;

            batch.draw(carta.getImagen(), x, y, ANCHO_CARTA, ALTURA_CARTA);
        }

        // Dibuja carta seleccionada arrastrando
        if (arrastrando && cartaSeleccionada != null) {
            batch.draw(cartaSeleccionada.getImagen(), cartaDragX, cartaDragY, ANCHO_CARTA, ALTURA_CARTA);
        }

        // Dibuja imagen SIGUIENTE.png sobre el botón play si toca
        if (mostrarBotonSiguiente) {
            batch.draw(imgSiguiente, BOTON_PLAY_X, BOTON_PLAY_Y, BOTON_PLAY_ANCHO, BOTON_PLAY_ALTO);
        }

        // --- Dibuja número de turno centrado en el área con fuente nítida (color NEGRO) ---
        String textoTurno = String.valueOf(turnoActual);
        layoutTurno.setText(fuenteTurno, textoTurno);
        float textX = TURNO_X + (TURNO_ANCHO - layoutTurno.width) / 2f;
        float textY = TURNO_Y + (TURNO_ALTO + layoutTurno.height) / 2f;

        // nos aseguramos color NEGRO para el turno
        fuenteTurno.setColor(Color.BLACK);
        fuenteTurno.draw(batch, layoutTurno, textX, textY);

        // --- Dibuja niveles disponibles ---
        dibujarNivelesDisponibles();

        batch.end();

        dibujarBarraVida();

        // Resalta ranura bajo el cursor al arrastrar carta
        if (ranuraHover != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0f, 0.6f, 1f, 0.3f));
            shapeRenderer.rect(ranuraHover.getX(), ranuraHover.getY(), ranuraHover.getAncho(), ranuraHover.getAlto());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Animación de iluminación y ataques
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
                    pasarSiguienteTurno();
                }
            }

            // Dibujo del highlight amarillo translúcido para la ranura actual
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(1f, 1f, 0f, 0.3f)); // Amarillo translúcido

            switch (ranuraActual) {
                case 0:
                    shapeRenderer.rect(0, 230, 338, 630);
                    break;
                case 1:
                    shapeRenderer.rect(370, 230, 385, 630);
                    break;
                case 2:
                    shapeRenderer.rect(777, 230, 379, 630);
                    break;
                case 3:
                    shapeRenderer.rect(1175, 230, 376, 630);
                    break;
                case 4:
                    shapeRenderer.rect(1562, 230, 357, 630);
                    break;
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void pasarSiguienteTurno() {
        if (turnoActual < 22) {
            turnoActual++;
            System.out.println("[TURNO] Avanzando al turno " + turnoActual);
        } else {
            System.out.println("[TURNO] Se alcanzó el turno máximo: " + turnoActual);
            // Aquí podrías agregar lógica para terminar batalla o mostrar mensaje
        }
        // Verificamos condición al cambiar de turno también (por si hubo daño directo que mató)
        verificarCondicionYTransicion();
        // Aquí podrías agregar lógica para invocar cartas o resetear estados para el nuevo turno
    }

    /**
     * Comprueba si la carta puede invocarse según el nivel permitido en el turnoActual.
     */
    private boolean puedeInvocarPorNivel(CartaTropa carta) {
        if (turnoActual < 1 || turnoActual > nivelesPorTurno.size()) {
            return false; // Fuera de rango de turnos definidos
        }
        List<Integer> nivelesPermitidos = nivelesPorTurno.get(turnoActual - 1);
        return nivelesPermitidos.contains(carta.getNivel());
    }

    /**
     * Verifica la condición de fin de partida y realiza la transición a la pantalla correspondiente.
     * - Si la vida del enemigo llegó a 0 primero -> PantallaVictoria
     * - Si la vida del jugador llegó a 0 primero  -> PantallaDerrota
     *
     * Nota: si ambas vidas quedan <= 0 simultáneamente, actualmente se considera DERROTA.
     */
    private void verificarCondicionYTransicion() {
        if (partidaTerminada) return;

        int vidaPropia = contexto.getVidaPropia();
        int vidaEnemiga = contexto.getVidaEnemiga();

        if (vidaEnemiga <= 0 && vidaPropia > 0) {
            // VICTORIA
            partidaTerminada = true;
            System.out.println("[PARTIDA] VICTORIA detectada - cambio a PantallaVictoria");
            // reproducir musica de victoria y cambiar pantalla
            juego.reproducirMusicaVictoria();
            juego.setScreen(new PantallaVictoria(juego));
            return;
        }

        if (vidaPropia <= 0 && vidaEnemiga > 0) {
            // DERROTA
            partidaTerminada = true;
            System.out.println("[PARTIDA] DERROTA detectada - cambio a PantallaDerrota");
            juego.reproducirMusicaDerrota();
            juego.setScreen(new PantallaDerrota(juego));
            return;
        }

        if (vidaEnemiga <= 0 && vidaPropia <= 0) {
            // Empate simultáneo: regla actual -> DERROTA (puedes cambiar esto)
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
    }

    public ContextoBatalla getContexto() {
        return contexto;
    }

    public List<CartaEfecto> getEfectosDisponibles() {
        return efectosDisponibles;
    }
}
