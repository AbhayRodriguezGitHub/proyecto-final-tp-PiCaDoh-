package mijuego.picadoh;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import mijuego.picadoh.cartas.RegistroCartas;

import java.io.File;

/**
 * Clase principal del juego.
 * - Inicializa recursos globales.
 * - Conecta el cliente LAN con descubrimiento automático (host null).
 *
 * Nota: clienteLAN NO es final para que otras pantallas (ej. PantallaMenu) puedan
 * reasignarlo o recrearlo si lo necesitan.
 */
public class Principal extends Game {
    public SpriteBatch batch;

    // clienteLAN con autodescubrimiento en la LAN (host = null).
    public mijuego.red.ClienteLAN clienteLAN;

    private boolean cursorPersonalizadoUsado = true;

    //  Música global
    private Music musicaMenu;
    private Music musicaSeleccion;
    private Music musicaBatalla1;
    private Music musicaBatalla2;
    private Music musicaVictoria; // Nueva
    private Music musicaDerrota;  // Nueva
    private Music musicaEmpate;   // Nueva
    private Music musicaActual;

    //  Volumen global
    private float volumenMusica = 1f;

    private boolean modoVentana = false;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Cursor robusto (no debe crashear aunque falte el PNG)
        try {
            aplicarCursor();
        } catch (Throwable t) {
            System.err.println("[CURSOR] Error aplicando cursor (fallback a sistema): " + t.getMessage());
            try { Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow); } catch (Throwable ignored) {}
        }

        cargarMusica();
        cargarMusicaSeleccion();
        cargarMusicaBatalla();
        cargarMusicaCondicion(); // Carga música de victoria/derrota/empate
        reproducirMusica(); // Menú por defecto

        // ----- Conexión LAN -----
        this.clienteLAN = new mijuego.red.ClienteLAN(this, null, 0);
        try {
            boolean ok = clienteLAN.connect();
            if (ok) {
                System.out.println("[LAN] Cliente conectado (autodescubrimiento activo).");
                clienteLAN.setOnMessage(json -> System.out.println("[LAN-RECV] " + json));
            } else {
                System.out.println("[LAN] No se pudo conectar al servidor (clienteLAN.connect() devolvió false).");
            }
        } catch (Throwable t) {
            System.out.println("[LAN] Error al iniciar cliente LAN: " + t.getMessage());
        }
        // -------------------------

        setScreen(new PantallaIntro(this));
    }

    private void cargarMusica() {
        musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/menus/musica_menu.mp3"));
        musicaMenu.setLooping(true);
        musicaMenu.setVolume(volumenMusica);
    }

    public void reproducirMusica() {
        if (musicaActual == musicaMenu && musicaMenu.isPlaying()) {
            return;
        }
        detenerMusicaActual();
        if (musicaMenu != null) {
            musicaMenu.setLooping(true);
            musicaMenu.setVolume(volumenMusica);
            musicaMenu.play();
            musicaActual = musicaMenu;
        }
    }

    public void pausarMusica() {
        if (musicaMenu != null && musicaMenu.isPlaying()) {
            musicaMenu.pause();
        }
    }

    public void detenerMusica() {
        if (musicaMenu != null) {
            musicaMenu.stop();
        }
    }

    private void cargarMusicaSeleccion() {
        musicaSeleccion = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/menus/musica_seleccion.mp3"));
        musicaSeleccion.setLooping(true);
        musicaSeleccion.setVolume(volumenMusica);
    }

    public void reproducirMusicaSeleccion() {
        detenerMusicaActual();
        if (musicaSeleccion != null) {
            musicaSeleccion.setLooping(true);
            musicaSeleccion.setVolume(volumenMusica);
            musicaSeleccion.play();
            musicaActual = musicaSeleccion;
        }
    }

    public void pausarMusicaSeleccion() {
        if (musicaSeleccion != null && musicaSeleccion.isPlaying()) {
            musicaSeleccion.pause();
        }
    }

    public void detenerMusicaSeleccion() {
        if (musicaSeleccion != null) {
            musicaSeleccion.stop();
        }
    }

    // ───────────────────────────────
    // Control total de músicas
    // ───────────────────────────────

    /** Detiene las músicas de batalla (ambas pistas). */
    public void detenerMusicaBatalla() {
        if (musicaBatalla1 != null) musicaBatalla1.stop();
        if (musicaBatalla2 != null) musicaBatalla2.stop();
    }

    /** Detiene las músicas de condición (victoria / derrota / empate). */
    public void detenerMusicaCondicion() {
        if (musicaVictoria != null) musicaVictoria.stop();
        if (musicaDerrota != null)  musicaDerrota.stop();
        if (musicaEmpate != null)   musicaEmpate.stop();
    }

    /** Apaga absolutamente TODAS las músicas activas del juego. */
    public void detenerTodasLasMusicas() {
        detenerMusicaActual();     // la que esté marcada como actual
        detenerMusica();           // menú
        detenerMusicaSeleccion();  // selección
        detenerMusicaBatalla();    // batalla
        detenerMusicaCondicion();  // victoria/derrota/empate
    }

    private void cargarMusicaBatalla() {
        musicaBatalla1 = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/campos/MUSICABATALLA1.mp3"));
        musicaBatalla2 = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/campos/MUSICABATALLA2.mp3"));
        musicaBatalla1.setVolume(volumenMusica);
        musicaBatalla2.setVolume(volumenMusica);
    }

    public void reproducirMusicaBatalla() {
        detenerMusicaActual();
        reproducirMusicaEnBucleAlternado(musicaBatalla1, musicaBatalla2);
    }

    private void reproducirMusicaEnBucleAlternado(final Music m1, final Music m2) {
        musicaActual = m1;
        m1.play();
        m1.setOnCompletionListener(music -> {
            musicaActual = m2;
            m2.play();
        });
        m2.setOnCompletionListener(music -> reproducirMusicaEnBucleAlternado(m1, m2));
    }

    private void cargarMusicaCondicion() {
        musicaVictoria = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/condicion/VICTORIA.mp3"));
        musicaVictoria.setLooping(true);
        musicaVictoria.setVolume(volumenMusica);

        musicaDerrota = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/condicion/DERROTA.mp3"));
        musicaDerrota.setLooping(true);
        musicaDerrota.setVolume(volumenMusica);

        musicaEmpate = Gdx.audio.newMusic(Gdx.files.internal("lwjgl3/assets/condicion/EMPATE.mp3"));
        musicaEmpate.setLooping(true);
        musicaEmpate.setVolume(volumenMusica);
    }

    public void reproducirMusicaVictoria() {
        detenerMusicaActual();
        if (musicaVictoria != null) {
            musicaVictoria.setVolume(volumenMusica);
            musicaVictoria.play();
            musicaActual = musicaVictoria;
        }
    }

    public void reproducirMusicaDerrota() {
        detenerMusicaActual();
        if (musicaDerrota != null) {
            musicaDerrota.setVolume(volumenMusica);
            musicaDerrota.play();
            musicaActual = musicaDerrota;
        }
    }

    public void reproducirMusicaEmpate() {
        detenerMusicaActual();
        if (musicaEmpate != null) {
            musicaEmpate.setVolume(volumenMusica);
            musicaEmpate.play();
            musicaActual = musicaEmpate;
        }
    }

    public void detenerMusicaActual() {
        if (musicaActual != null && musicaActual.isPlaying()) {
            musicaActual.stop();
        }
    }

    // ───────────────────────────────
    // Volumen
    // ───────────────────────────────
    public float getVolumenMusica() {
        return volumenMusica;
    }

    public void setVolumenMusica(float volumen) {
        this.volumenMusica = MathUtils.clamp(volumen, 0f, 1f);
        if (musicaMenu != null) musicaMenu.setVolume(volumenMusica);
        if (musicaSeleccion != null) musicaSeleccion.setVolume(volumenMusica);
        if (musicaBatalla1 != null) musicaBatalla1.setVolume(volumenMusica);
        if (musicaBatalla2 != null) musicaBatalla2.setVolume(volumenMusica);
        if (musicaVictoria != null) musicaVictoria.setVolume(volumenMusica);
        if (musicaDerrota != null) musicaDerrota.setVolume(volumenMusica);
        if (musicaEmpate != null) musicaEmpate.setVolume(volumenMusica);
    }

    // ───────────────────────────────
    // Cursor personalizado (robusto, sin crash)
    // ───────────────────────────────
    public void aplicarCursor() {
        if (!cursorPersonalizadoUsado) {
            try {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            } catch (Throwable ignored) {}
            return;
        }
        try {
            setCursorPersonalizado();
        } catch (Throwable t) {
            System.err.println("[CURSOR] setCursorPersonalizado falló: " + t.getMessage());
            try {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Intenta cargar "lwjgl3/assets/ui/CURSOR.png" desde varias ubicaciones
     * (absolute → local → internal) y crea un cursor escalado con límites.
     * Si falla, NO crashea: deja el cursor del sistema.
     */
    public void setCursorPersonalizado() {
        final String rel = "lwjgl3/assets/ui/CURSOR.png";
        FileHandle fh = findAssetMulti(rel);

        if (fh == null || !fh.exists()) {
            System.err.println("[CURSOR] No se encontró el PNG en ninguna ubicación conocida: " + rel);
            try { Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow); } catch (Throwable ignored) {}
            return;
        }

        Pixmap original = null;
        Pixmap scaled = null;
        try {
            original = new Pixmap(fh);

            // Calcular escala (segura) desde 1920x1080
            int screenWidth = Math.max(1, Gdx.graphics.getWidth());
            int screenHeight = Math.max(1, Gdx.graphics.getHeight());
            float refWidth = 1920f;
            float refHeight = 1080f;
            float scale = (screenWidth / refWidth + screenHeight / refHeight) / 2f;
            if (scale <= 0f) scale = 1f;

            int newWidth = Math.max(1, Math.round(original.getWidth() * scale));
            int newHeight = Math.max(1, Math.round(original.getHeight() * scale));

            // Límite típico para cursores (muchas plataformas 32/64/128/256). Usamos 256.
            int MAX_CURSOR = 256;
            newWidth = Math.min(newWidth, MAX_CURSOR);
            newHeight = Math.min(newHeight, MAX_CURSOR);

            // Ajustar a potencia de 2 (algunas impls lo prefieren)
            newWidth = nextPowerOfTwo(newWidth);
            newHeight = nextPowerOfTwo(newHeight);
            newWidth = Math.min(newWidth, MAX_CURSOR);
            newHeight = Math.min(newHeight, MAX_CURSOR);

            scaled = new Pixmap(newWidth, newHeight, Pixmap.Format.RGBA8888);
            scaled.drawPixmap(original, 0, 0, original.getWidth(), original.getHeight(), 0, 0, newWidth, newHeight);

            int xHotspot = newWidth / 2;
            int yHotspot = newHeight / 6;

            Cursor cursor = Gdx.graphics.newCursor(scaled, xHotspot, yHotspot);
            Gdx.graphics.setCursor(cursor);

            System.out.println("[CURSOR] Cursor aplicado desde: " + fh.path() +
                " (" + original.getWidth() + "x" + original.getHeight() +
                " → " + newWidth + "x" + newHeight + ")");
        } catch (Throwable t) {
            System.err.println("[CURSOR] Falló la creación del cursor: " + t.getMessage());
            try { Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow); } catch (Throwable ignored) {}
        } finally {
            if (original != null) original.dispose();
            if (scaled != null) scaled.dispose();
        }
    }

    /**
     * Busca un asset probando varias opciones para tolerar diferencias de working directory:
     * - absolute("lwjgl3/assets/..."), local(...), internal(...),
     * - variantes con separador nativo y con user.dir delante.
     */
    private FileHandle findAssetMulti(String relativePath) {
        // 1) absolute tal como lo usás
        FileHandle fh = Gdx.files.absolute(relativePath);
        if (fh.exists()) return fh;

        // 2) local
        fh = Gdx.files.local(relativePath);
        if (fh.exists()) return fh;

        // 3) internal (por si IDE empaqueta resources)
        fh = Gdx.files.internal(relativePath);
        if (fh.exists()) return fh;

        // 4) con separador nativo
        String nativeRel = relativePath.replace("/", File.separator).replace("\\", File.separator);
        fh = Gdx.files.absolute(nativeRel);
        if (fh.exists()) return fh;
        fh = Gdx.files.local(nativeRel);
        if (fh.exists()) return fh;
        fh = Gdx.files.internal(nativeRel);
        if (fh.exists()) return fh;

        // 5) prefijar con user.dir (working dir)
        String userDir = System.getProperty("user.dir", ".");
        String withUserDir = userDir + File.separator + nativeRel;
        fh = Gdx.files.absolute(withUserDir);
        if (fh.exists()) return fh;

        // 6) subir un nivel por si se ejecuta desde submódulo
        String upOne = userDir + File.separator + ".." + File.separator + nativeRel;
        fh = Gdx.files.absolute(upOne);
        if (fh.exists()) return fh;

        // no encontrado
        return null;
    }

    public boolean isCursorPersonalizadoUsado() {
        return cursorPersonalizadoUsado;
    }

    public void setCursorPersonalizadoUsado(boolean usado) {
        this.cursorPersonalizadoUsado = usado;
        aplicarCursor();
    }

    private int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        int power = 1;
        while (power < n) power <<= 1;
        return power;
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (musicaMenu != null) musicaMenu.dispose();
        if (musicaSeleccion != null) musicaSeleccion.dispose();
        if (musicaBatalla1 != null) musicaBatalla1.dispose();
        if (musicaBatalla2 != null) musicaBatalla2.dispose();
        if (musicaVictoria != null) musicaVictoria.dispose();
        if (musicaDerrota != null) musicaDerrota.dispose();
        if (musicaEmpate != null) musicaEmpate.dispose();
        // cerrar cliente LAN si está abierto
        try {
            if (clienteLAN != null) clienteLAN.close();
        } catch (Throwable ignored) {}
        super.dispose();
    }

    public boolean isModoVentana() {
        return modoVentana;
    }

    public void setModoVentana(boolean modoVentana) {
        this.modoVentana = modoVentana;
    }
}
