package mijuego.picadoh.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import mijuego.picadoh.Principal;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        // Establece el tamaño por defecto de la ventana
        configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());


        // Intentamos cargar iconos solo si existen y son legibles.
        // Rutas probables (IDE y paquete). Ajustá si las tenés en otro lugar.
        String[] candidatePaths = new String[] {
            "lwjgl3/assets/icon/ICON.png",
            "lwjgl3/assets/icon/ICON.ico",
            "icon/ICON.png",
            "icon/ICON.ico"
        };

        List<String> validPaths = new ArrayList<>();
        for (String p : candidatePaths) {
            try {
                File f = new File(p);
                if (f.exists() && f.isFile()) {
                    // Comprobamos que ImageIO pueda decodificar (evita crash por formato corrupto)
                    try {
                        if (ImageIO.read(f) != null) {
                            validPaths.add(p);
                        } else {
                            System.err.println("[ICON] Archivo encontrado pero no legible por ImageIO: " + p);
                        }
                    } catch (Exception e) {
                        System.err.println("[ICON] Error intentando leer con ImageIO (probablemente .ico no soportado por ImageIO): " + p);
                        // Para .ico ImageIO suele no soportar; intentamos añadir .ico igualmente pero solo si no causa error posterior.
                        // Sin embargo para mayor seguridad no agregamos .ico aquí. Si querés forzar .ico, movelo a resources y probamos.
                    }
                }
            } catch (Throwable t) {
                // nunca fallar el launcher por el icono
                System.err.println("[ICON] Excepción comprobando icono " + p + " -> " + t.getMessage());
            }
        }

        if (!validPaths.isEmpty()) {
            // Convertimos a arreglo y asignamos
            String[] arr = validPaths.toArray(new String[0]);
            try {
                configuration.setWindowIcon(arr);
            } catch (Throwable t) {
                // No debe fallar el arranque por un icono; lo registramos y seguimos sin icono
                System.err.println("[ICON] setWindowIcon falló: " + t.getMessage());
            }
        } else {
            System.out.println("[ICON] No se encontraron iconos válidos. Se usará el icono por defecto.");
        }

        return configuration;
    }
}
