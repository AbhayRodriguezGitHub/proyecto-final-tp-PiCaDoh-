package mijuego.picadoh.efectos;

import java.util.*;
import java.lang.reflect.Constructor;

public final class RegistroEfectos {
    private RegistroEfectos() {}

    private static final List<Class<? extends CartaEfecto>> EFECTOS = List.of(
        Acelereitor.class,
        EscudoReal.class,
        ExplosionForzal.class,
        MagiaBendita.class,
        SenoraArmadura.class,
        Tyson.class,
        AnarquiaNivel.class,
        Bombardrilo.class,
        Monarquia.class,
        Rebelion.class,
        MalDeAmores.class,
        MagoDel8.class
    );

    public static List<Class<? extends CartaEfecto>> efectosDisponibles() {
        return EFECTOS;
    }

    public static CartaEfecto crear(Class<? extends CartaEfecto> clazz) {
        try {
            Constructor<? extends CartaEfecto> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo instanciar " + clazz.getSimpleName(), e);
        }
    }

    public static Optional<CartaEfecto> crearPorNombre(String nombre) {
        for (Class<? extends CartaEfecto> c : EFECTOS) {
            // Coincidencia por nombre de clase
            if (c.getSimpleName().equalsIgnoreCase(nombre)) {
                return Optional.of(crear(c));
            }

            try {
                CartaEfecto tmp = crear(c);
                if (tmp.getNombre().equalsIgnoreCase(nombre)) {
                    return Optional.of(tmp);
                }
            } catch (RuntimeException ignored) {}
        }
        return Optional.empty();
    }

    public static List<CartaEfecto> aleatorias(int n, Random rnd) {
        List<Class<? extends CartaEfecto>> copia = new ArrayList<>(EFECTOS);
        Collections.shuffle(copia, rnd);
        int k = Math.min(n, copia.size());
        List<CartaEfecto> out = new ArrayList<>(k);
        for (int i = 0; i < k; i++) out.add(crear(copia.get(i)));
        return out;
    }
}
