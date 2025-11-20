package mijuego.mundo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import mijuego.picadoh.Principal;
import mijuego.picadoh.taberna.PantallaTaberna;

public class PantallaMundo3D implements Screen {

    private final Principal juego;

    private PerspectiveCamera cam;
    private ModelBatch modelBatch;
    private Environment environment;

    private Model modelBilly;
    private ModelInstance billy;

    private Model modelPiso;
    private ModelInstance piso;

    private float velocidad = 10f;           // velocidad base
    private float velocidadCorriendo = 18f;  // velocidad al correr
    private float velocidadActual = velocidad;

    private final Vector3 direccionTmp = new Vector3();

    // ------------------------
    // FÍSICAS DEL SALTO
    // ------------------------
    private float alturaY = 1f;          // posición vertical real de Billy
    private float velocidadY = 0f;       // velocidad vertical
    private float gravedad = -30f;       // gravedad
    private boolean enElSuelo = true;    // si toca el piso

    public PantallaMundo3D(Principal juego) {
        this.juego = juego;

        modelBatch = new ModelBatch();

        cam = new PerspectiveCamera(
            67,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        cam.position.set(0f, 10f, 20f);
        cam.lookAt(0f, 0f, 0f);
        cam.near = 0.1f;
        cam.far = 300f;
        cam.update();

        environment = new Environment();
        environment.set(
            new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f)
        );
        environment.add(new DirectionalLight()
            .set(1f, 1f, 1f, -1f, -0.8f, -0.5f));

        ModelBuilder mb = new ModelBuilder();

        modelPiso = mb.createBox(
            100f, 1f, 100f,
            new Material(ColorAttribute.createDiffuse(Color.FOREST)),
            com.badlogic.gdx.graphics.VertexAttributes.Usage.Position
                | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal
        );
        piso = new ModelInstance(modelPiso);
        piso.transform.setToTranslation(0f, -0.5f, 0f);

        modelBilly = mb.createBox(
            1f, 2f, 1f,
            new Material(ColorAttribute.createDiffuse(Color.SKY)),
            com.badlogic.gdx.graphics.VertexAttributes.Usage.Position
                | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal
        );
        billy = new ModelInstance(modelBilly);
        billy.transform.setToTranslation(0f, alturaY, 0f);

        // -----------------------
        // INPUT (ESC, ENTER, SPACE)
        // -----------------------
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    juego.setScreen(new PantallaTaberna(juego));
                    return true;
                }

                // ENTER = pelea de cartas
                if (keycode == Input.Keys.ENTER) {
                    iniciarBatallaDeCartasDummy();
                    return true;
                }

                // ESPACIO = SALTO
                if (keycode == Input.Keys.SPACE) {
                    saltar();
                    return true;
                }

                return false;
            }
        });
    }

    // -----------------------
    // LÓGICA DE SALTO
    // -----------------------
    private void saltar() {
        if (enElSuelo) {
            enElSuelo = false;
            velocidadY = 12f;   // fuerza del salto
        }
    }

    @Override
    public void show() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    @Override
    public void render(float delta) {
        actualizarMovimiento(delta);
        actualizarSalto(delta);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.2f, 0.4f, 0.7f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        cam.update();

        modelBatch.begin(cam);
        modelBatch.render(piso, environment);
        modelBatch.render(billy, environment);
        modelBatch.end();
    }

    // -----------------------------------
    // MOVIMIENTO + SHIFT CORRER
    // -----------------------------------
    private void actualizarMovimiento(float delta) {

        // SHIFT = correr
        boolean correr = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        velocidadActual = correr ? velocidadCorriendo : velocidad;

        direccionTmp.setZero();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            direccionTmp.z -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            direccionTmp.z += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            direccionTmp.x -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            direccionTmp.x += 1f;
        }

        if (!direccionTmp.isZero()) {
            direccionTmp.nor().scl(velocidadActual * delta);

            billy.transform.translate(direccionTmp);

            Vector3 posBilly = new Vector3();
            billy.transform.getTranslation(posBilly);

            cam.position.set(posBilly.x, posBilly.y + 8f, posBilly.z + 15f);
            cam.lookAt(posBilly);
        }
    }

    // -----------------------------
    // ACTUALIZAR SALTO
    // -----------------------------
    private void actualizarSalto(float delta) {

        if (!enElSuelo) {
            velocidadY += gravedad * delta;
            alturaY += velocidadY * delta;

            if (alturaY <= 1f) {   // altura del piso
                alturaY = 1f;
                velocidadY = 0f;
                enElSuelo = true;
            }

            // actualizar posición
            Vector3 pos = new Vector3();
            billy.transform.getTranslation(pos);
            billy.transform.setToTranslation(pos.x, alturaY, pos.z);
        }
    }

    private void iniciarBatallaDeCartasDummy() {
        System.out.println("[MUNDO3D] (ENTER) -> Acá iría la pelea de cartas contra un NPC.");
    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        modelBatch.dispose();
        modelBilly.dispose();
        modelPiso.dispose();
    }
}
