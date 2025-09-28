package mijuego.picadoh.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import mijuego.picadoh.Principal;

/**
 * Clase de lanzamiento para escritorio (usando LWJGL3).
 * Esta clase configura la ventana y lanza el juego principal
 */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        // Esta línea es necesaria para soporte en macOS y ayuda en Windows.
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    /** Crea y lanza la aplicación con la clase principal del juego. */
    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Principal(), getDefaultConfiguration());
    }

    /**
     * Configura los parámetros de la ventana del juego
     */
    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        // Título de la ventana
        configuration.setTitle("Pi-Ca-Doh!");

        // Activa VSync para evitar "screen tearing"
        configuration.useVsync(true);

        // Establece los FPS en base a la frecuencia del monitor
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        // Establece el tamaño de la ventana (debe coincidir con el fondo de menú)
        configuration.setWindowedMode(1920, 1080);

        // Íconos de ventana (se pueden comentar si aún no tenés los archivos)
        configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());

        // Asegurate de tener estos íconos en core/assets/ o comentá esta línea
        // configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}
