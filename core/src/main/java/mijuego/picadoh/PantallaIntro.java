package mijuego.picadoh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class PantallaIntro implements Screen {

    private final Principal juego;

    private static volatile boolean fxInit = false;

    private Object stage;
    private Object mediaView;
    private Object mediaPlayer;

    private final List<File> videos = new ArrayList<>();
    private int idx = 0;
    private boolean cerrando = false;

    public PantallaIntro(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        try {
            juego.detenerMusicaActual();
            asegurarJavaFX();

            agregarSiExiste("lwjgl3/assets/intro/INTRO1.mp4");
            agregarSiExiste("lwjgl3/assets/intro/INTRO2.mp4");
            agregarSiExiste("lwjgl3/assets/intro/INTRO3.mp4");
            agregarSiExiste("lwjgl3/assets/intro/INTRO4.mp4");

            if (videos.isEmpty()) {
                Gdx.app.log("INTRO", "No hay videos, voy al menú.");
                irMenu();
                return;
            }

            fxRun(() -> {
                try {
                    Class<?> Stage = cls("javafx.stage.Stage");
                    Class<?> StackPane = cls("javafx.scene.layout.StackPane");
                    Class<?> Scene = cls("javafx.scene.Scene");
                    Class<?> Parent = cls("javafx.scene.Parent");
                    Class<?> MediaView = cls("javafx.scene.media.MediaView");
                    Class<?> EventHandler = cls("javafx.event.EventHandler");
                    Class<?> KeyCombination = cls("javafx.scene.input.KeyCombination");

                    stage = newInstance(Stage);
                    mediaView = newInstance(MediaView);

                    Object root = newInstance(StackPane);
                    // Fondo negro para bandas
                    try {
                        root.getClass().getMethod("setStyle", String.class)
                            .invoke(root, "-fx-background-color: black;");
                    } catch (Exception ignored) {}

                    Method getChildren = root.getClass().getMethod("getChildren");
                    Object children = getChildren.invoke(root);
                    children.getClass().getMethod("add", Object.class).invoke(children, mediaView);

                    Object scene = Scene.getConstructor(Parent, double.class, double.class)
                        .newInstance(root, (double) Gdx.graphics.getWidth(), (double) Gdx.graphics.getHeight());
                    Stage.getMethod("setScene", cls("javafx.scene.Scene")).invoke(stage, scene);
                    Stage.getMethod("setAlwaysOnTop", boolean.class).invoke(stage, true);
                    Stage.getMethod("setFullScreen", boolean.class).invoke(stage, true);

                    try {
                        Stage.getMethod("setFullScreenExitHint", String.class).invoke(stage, "");
                        Object NO_MATCH = KeyCombination.getField("NO_MATCH").get(null);
                        Stage.getMethod("setFullScreenExitKeyCombination", KeyCombination)
                            .invoke(stage, NO_MATCH);
                    } catch (Exception ignored) {}

                    Object eventHandler = Proxy.newProxyInstance(
                        EventHandler.getClassLoader(),
                        new Class[]{EventHandler},
                        (proxy, method, args) -> {
                            if ("handle".equals(method.getName())) {
                                Gdx.app.postRunnable(this::saltar);
                            }
                            return null;
                        }
                    );
                    scene.getClass().getMethod("setOnKeyPressed", EventHandler).invoke(scene, eventHandler);
                    scene.getClass().getMethod("setOnMouseClicked", EventHandler).invoke(scene, eventHandler);

                    Object closeHandler = Proxy.newProxyInstance(
                        EventHandler.getClassLoader(),
                        new Class[]{EventHandler},
                        (proxy, method, args) -> {
                            if ("handle".equals(method.getName())) {
                                Gdx.app.postRunnable(this::cerrarYMenu);
                            }
                            return null;
                        }
                    );
                    stage.getClass().getMethod("setOnCloseRequest", EventHandler).invoke(stage, closeHandler);

                    // Mostrar y al frente
                    Stage.getMethod("show").invoke(stage);
                    try { Stage.getMethod("toFront").invoke(stage); } catch (Exception ignored) {}

                    // Ajustamos y reproducimos
                    ajustarEscala();
                    reproducirActual();
                } catch (Exception e) {
                    Gdx.app.error("INTRO", "Error creando UI JavaFX", e);
                    Gdx.app.postRunnable(this::irMenu);
                }
            });

        } catch (Exception e) {
            Gdx.app.error("INTRO", "Fallo inicializando JavaFX", e);
            irMenu();
        }
    }

    private void agregarSiExiste(String rutaAbs) {
        FileHandle fh = Gdx.files.absolute(rutaAbs);
        if (fh.exists()) {
            File f = fh.file();
            videos.add(f);
            Gdx.app.log("INTRO", "Archivo: " + f.getAbsolutePath() + " | exists=true");
        } else {
            Gdx.app.log("INTRO", "Falta: " + rutaAbs);
        }
    }

    private void reproducirActual() {
        if (idx < 0 || idx >= videos.size()) { cerrarYMenu(); return; }
        fxRun(() -> {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.getClass().getMethod("stop").invoke(mediaPlayer);
                    mediaPlayer.getClass().getMethod("dispose").invoke(mediaPlayer);
                }

                Class<?> Media = cls("javafx.scene.media.Media");
                Class<?> MediaPlayer = cls("javafx.scene.media.MediaPlayer");
                String uri = videos.get(idx).toURI().toString();
                Object media = Media.getConstructor(String.class).newInstance(uri);
                mediaPlayer = MediaPlayer.getConstructor(Media).newInstance(media);

                // Reproducir automáticamente
                mediaPlayer.getClass().getMethod("setAutoPlay", boolean.class).invoke(mediaPlayer, true);

                mediaPlayer.getClass().getMethod("setOnEndOfMedia", Runnable.class)
                    .invoke(mediaPlayer, (Runnable) this::siguiente);

                mediaPlayer.getClass().getMethod("setOnReady", Runnable.class)
                    .invoke(mediaPlayer, (Runnable) this::ajustarEscala);

                Class<?> MV = cls("javafx.scene.media.MediaView");
                Method setMediaPlayer = MV.getMethod("setMediaPlayer", MediaPlayer);
                setMediaPlayer.invoke(mediaView, mediaPlayer);

                try {
                    MV.getMethod("setPreserveRatio", boolean.class).invoke(mediaView, true);
                    MV.getMethod("setSmooth", boolean.class).invoke(mediaView, true);
                } catch (Exception ignored) {}

                ajustarEscala();
            } catch (Exception e) {
                Gdx.app.error("INTRO", "Error reproduciendo: " + videos.get(idx), e);
                Gdx.app.postRunnable(this::siguiente);
            }
        });
    }

    private void siguiente() {
        if (cerrando) return;
        idx++;
        if (idx >= videos.size()) cerrarYMenu();
        else reproducirActual();
    }

    private void saltar() {
        if (cerrando) return;
        if (idx >= videos.size() - 1) cerrarYMenu();
        else siguiente();
    }

    private void cerrarYMenu() {
        if (cerrando) return;
        cerrando = true;
        fxRun(() -> {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.getClass().getMethod("stop").invoke(mediaPlayer);
                    mediaPlayer.getClass().getMethod("dispose").invoke(mediaPlayer);
                    mediaPlayer = null;
                }
                if (stage != null) {
                    try { stage.getClass().getMethod("setAlwaysOnTop", boolean.class).invoke(stage, false); } catch (Exception ignored) {}
                    stage.getClass().getMethod("close").invoke(stage);
                    stage = null;
                }
            } catch (Exception ignored) {}
            Gdx.app.postRunnable(() -> {
                irMenu();
                devolverFocoVentanaLwjgl();
            });
        });
    }

    private void irMenu() {
        juego.setScreen(new PantallaMenu(juego));
    }

    private void ajustarEscala() {
        fxRun(() -> {
            try {
                double sw = Gdx.graphics.getWidth();
                double sh = Gdx.graphics.getHeight();

                Class<?> MV = cls("javafx.scene.media.MediaView");
                Method setPreserveRatio = MV.getMethod("setPreserveRatio", boolean.class);
                Method setFitWidth = MV.getMethod("setFitWidth", double.class);
                Method setFitHeight = MV.getMethod("setFitHeight", double.class);

                setPreserveRatio.invoke(mediaView, true);

                setFitWidth.invoke(mediaView, sw);
                setFitHeight.invoke(mediaView, sh);

                try {
                    Object scene = stage.getClass().getMethod("getScene").invoke(stage);
                    Object root = scene.getClass().getMethod("getRoot").invoke(scene);
                    root.getClass().getMethod("setStyle", String.class)
                        .invoke(root, "-fx-background-color: black; -fx-alignment: center;");
                } catch (Exception ignored) {}

            } catch (Exception ignored) {}
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Entrada duplicada por si JavaFX no capta algo
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.justTouched()) {
            fxRun(this::saltar);
        }
    }

    @Override public void resize(int width, int height) { ajustarEscala(); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { cerrarYMenu(); }
    @Override public void dispose() {}

    private static void asegurarJavaFX() throws Exception {
        if (fxInit) return;
        synchronized (PantallaIntro.class) {
            if (fxInit) return;

            try {
                Class<?> Platform = cls("javafx.application.Platform");
                Method startup = Platform.getMethod("startup", Runnable.class);
                startup.invoke(null, (Runnable) () -> {});
            } catch (NoSuchMethodException ignored) {
                // versiones de JavaFX que ya arrancan por JFXPanel
            } catch (Exception e) {
                // si falla startup, seguimos
            }

            try {
                Class<?> JFXPanel = cls("javafx.embed.swing.JFXPanel");
                newInstance(JFXPanel); // fuerza Toolkit si no estaba
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Falta módulo javafx-swing en el classpath.", e);
            }

            fxInit = true;
        }
    }

    private static void fxRun(Runnable r) {
        try {
            Class<?> Platform = cls("javafx.application.Platform");
            Method isFxThread = Platform.getMethod("isFxApplicationThread");
            boolean onFx = (boolean) isFxThread.invoke(null);
            if (onFx) {
                r.run();
            } else {
                Method runLater = Platform.getMethod("runLater", Runnable.class);
                runLater.invoke(null, r);
            }
        } catch (Exception e) {
            Gdx.app.error("INTRO", "Platform.runLater falló", e);
        }
    }

    private void devolverFocoVentanaLwjgl() {
        try {
            Class<?> Lwjgl3Graphics = Class.forName("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics");
            if (Lwjgl3Graphics.isInstance(Gdx.graphics)) {
                Object graphics = Gdx.graphics;
                Object window = Lwjgl3Graphics.getMethod("getWindow").invoke(graphics);
                long handle = (Long) window.getClass().getMethod("getWindowHandle").invoke(window);

                Class<?> GLFW = Class.forName("org.lwjgl.glfw.GLFW");
                try { GLFW.getMethod("glfwShowWindow", long.class).invoke(null, handle); } catch (Throwable ignored) {}
                try { GLFW.getMethod("glfwFocusWindow", long.class).invoke(null, handle); } catch (Throwable ignored) {}
                try { GLFW.getMethod("glfwRequestWindowAttention", long.class).invoke(null, handle); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        }
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private static Object newInstance(Class<?> c) throws Exception {
        Constructor<?> cons = c.getDeclaredConstructor();
        cons.setAccessible(true);
        return cons.newInstance();
    }
}
