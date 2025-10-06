package mijuego.picadoh.efectos;

import java.util.*;
import java.lang.reflect.Constructor;

/**
 * Registro/fábrica central de cartas de efecto.
 * Añade utilidades para trabajar con nombres de clase (FQCN) útiles para red/LAN.
 */
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
        MagoDel8.class,
        Paracetamol.class,
        Intercambio.class,
        Avaricioso.class,
        AgenteDeTransito.class,
        EscudoFalso.class,
        EscudoPlatinado.class,
        Gangsterio.class,
        Orikalkus.class
    );

    /**
     * Devuelve la lista de clases disponibles (Class objects).
     */
    public static List<Class<? extends CartaEfecto>> efectosDisponibles() {
        return EFECTOS;
    }

    /**
     * Crea una instancia de CartaEfecto usando la Class provista.
     * Lanza RuntimeException si falla (comportamiento consistente con lo anterior).
     */
    public static CartaEfecto crear(Class<? extends CartaEfecto> clazz) {
        try {
            Constructor<? extends CartaEfecto> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo instanciar " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Intenta crear un efecto por nombre (simple name o por nombre interno de la carta).
     * Devuelve Optional.empty() si no lo encuentra.
     */
    public static Optional<CartaEfecto> crearPorNombre(String nombre) {
        for (Class<? extends CartaEfecto> c : EFECTOS) {
            // Coincidencia por nombre de clase simple
            if (c.getSimpleName().equalsIgnoreCase(nombre)) {
                try {
                    return Optional.of(crear(c));
                } catch (RuntimeException ignored) {}
            }

            // Coincidencia por nombre mostrado dentro de la carta (si corresponde)
            try {
                CartaEfecto tmp = crear(c);
                if (tmp != null && tmp.getNombre().equalsIgnoreCase(nombre)) {
                    return Optional.of(tmp);
                }
            } catch (RuntimeException ignored) {}
        }
        return Optional.empty();
    }

    /**
     * Crea una instancia a partir del nombre de clase totalmente cualificado (FQCN).
     * Devuelve Optional.empty() si la clase no se encuentra o no puede instanciarse.
     *
     * Útil cuando recibís por red una lista de class names (ej. "mijuego.picadoh.efectos.Acelereitor").
     */
    public static Optional<CartaEfecto> crearPorClassName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) return Optional.empty();
        try {
            Class<?> cls = Class.forName(fqcn);
            if (!CartaEfecto.class.isAssignableFrom(cls)) {
                System.out.println("[RegistroEfectos] La clase " + fqcn + " no es CartaEfecto.");
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            Class<? extends CartaEfecto> ce = (Class<? extends CartaEfecto>) cls;
            // Si la clase está en el registro EFECTOS, usamos crear(ce) para consistencia; si no, intentamos reflexionar.
            if (EFECTOS.contains(ce)) {
                return Optional.of(crear(ce));
            } else {
                Constructor<? extends CartaEfecto> ctor = ce.getDeclaredConstructor();
                ctor.setAccessible(true);
                return Optional.of(ctor.newInstance());
            }
        } catch (ClassNotFoundException cnf) {
            System.out.println("[RegistroEfectos] Clase no encontrada: " + fqcn);
        } catch (Exception ex) {
            System.out.println("[RegistroEfectos] Error instanciando por FQCN " + fqcn + " -> " + ex.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Devuelve una lista de nombres de clase totalmente cualificados (FQCN) de todos los efectos registrados.
     * Útil para enviarlo por red o debug.
     */
    public static List<String> availableClassNames() {
        List<String> out = new ArrayList<>(EFECTOS.size());
        for (Class<? extends CartaEfecto> c : EFECTOS) out.add(c.getName());
        return out;
    }

    /**
     * Devuelve el nombre de clase totalmente cualificado (FQCN) de la instancia dada,
     * o null si la instancia es null.
     */
    public static String getClassName(CartaEfecto efecto) {
        if (efecto == null) return null;
        return efecto.getClass().getName();
    }

    /**
     * Selecciona n efectos aleatorios (instancias) usando Random provisto.
     */
    public static List<CartaEfecto> aleatorias(int n, Random rnd) {
        List<Class<? extends CartaEfecto>> copia = new ArrayList<>(EFECTOS);
        Collections.shuffle(copia, rnd);
        int k = Math.min(n, copia.size());
        List<CartaEfecto> out = new ArrayList<>(k);
        for (int i = 0; i < k; i++) out.add(crear(copia.get(i)));
        return out;
    }
}
