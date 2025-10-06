package mijuego.picadoh.cartas;

import java.util.*;
import java.lang.reflect.Constructor;

/**
 * Registro/fábrica central de cartas de tropa.
 * Añade utilidades para trabajar con nombres de clase (FQCN) útiles para red/LAN.
 */
public final class RegistroCartas {
    private RegistroCartas() {}

    private static final List<Class<? extends CartaTropa>> TROPAS = List.of(
        Guardiancito.class,
        Barbot.class,
        MafiosaRosa.class,
        Nappo.class,
        Juvergot.class,
        Konjisma.class,
        Alkaline.class,
        Saga.class,
        CabezaGol.class,
        PlantaGuerra.class,
        Bandy.class,
        Barbarroz.class,
        Barbillon.class,
        Viernes12.class,
        Zamba.class,
        Larro.class,
        Chavo7.class,
        Hornero.class,
        Mifalda.class,
        Indiana.class,
        Nario.class,
        Nosic.class,
        Toroto.class,
        Tazitota.class,
        Alfreddo.class,
        Badabun.class,
        Ballestero.class,
        Bigote.class,
        Bochini.class,
        Bombastic.class,
        Bullynero.class,
        Dnk.class,
        Edge.class,
        Libertad.class,
        Phonix.class,
        Rinyu.class,
        Slim.class,
        Tiro.class,
        TrollBox.class,
        Vecino.class,
        Verdoso.class,
        Montana.class,
        Pesos.class,
        Jc.class,
        Martson.class,
        Gingara.class,
        SinSonrisa.class,
        Venganza.class,
        Xman.class,
        Gaiden.class,
        Fierro.class,
        Destruidor.class,
        Creed.class,
        Bromas.class,
        Brocoli.class,
        Blander.class,
        Agente.class,
        Chapa.class,
        Danta.class,
        Escorpion.class,
        Injerst.class,
        Keneddy.class,
        King.class,
        Samubat.class,
        Span.class,
        Tinton.class,
        UltraCaballero.class,
        Yaga.class,
        ZomGod.class,
        Avatar.class,
        Chyper.class,
        Marado.class,
        Ouroun.class,
        Qutulu.class,
        Sobrino.class,
        Tennyson.class,
        Trans.class,
        Vergal.class,
        Ñensei.class,
        Jansinski.class
    );

    /**
     * Devuelve la lista de clases disponibles (referencias Class).
     */
    public static List<Class<? extends CartaTropa>> tropasDisponibles() {
        return TROPAS;
    }

    /**
     * Crea una instancia de CartaTropa usando la Class provista.
     * Lanza RuntimeException si falla (igual que antes).
     */
    public static CartaTropa crear(Class<? extends CartaTropa> clazz) {
        try {
            Constructor<? extends CartaTropa> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo instanciar " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Intenta crear una tropa a partir de su nombre simple (sin paquete) o por su nombre
     * (retorna Optional.empty() si no existe/ocurre error).
     */
    public static Optional<CartaTropa> crearPorNombre(String nombre) {
        for (Class<? extends CartaTropa> c : TROPAS) {
            if (c.getSimpleName().equalsIgnoreCase(nombre)) {
                try {
                    return Optional.of(crear(c));
                } catch (RuntimeException ignored) {}
            }
            try {
                CartaTropa tmp = crear(c);
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
     * Útil cuando recibís por red una lista de class names (ej. "mijuego.picadoh.cartas.Gardiancito").
     */
    public static Optional<CartaTropa> crearPorClassName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) return Optional.empty();
        try {
            Class<?> cls = Class.forName(fqcn);
            if (!CartaTropa.class.isAssignableFrom(cls)) {
                System.out.println("[RegistroCartas] La clase " + fqcn + " no es CartaTropa.");
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            Class<? extends CartaTropa> ct = (Class<? extends CartaTropa>) cls;
            // Si la clase está en el registro TROPAS, usamos crear(ct) para consistencia; si no, intentamos reflexionar.
            if (TROPAS.contains(ct)) {
                return Optional.of(crear(ct));
            } else {
                // intentar crear por reflexión directa
                Constructor<? extends CartaTropa> ctor = ct.getDeclaredConstructor();
                ctor.setAccessible(true);
                return Optional.of(ctor.newInstance());
            }
        } catch (ClassNotFoundException cnf) {
            System.out.println("[RegistroCartas] Clase no encontrada: " + fqcn);
        } catch (Exception ex) {
            System.out.println("[RegistroCartas] Error instanciando por FQCN " + fqcn + " -> " + ex.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Devuelve una lista de nombres de clase totalmente cualificados (FQCN) de todas las tropas registradas.
     * Útil para enviarlo por red o debug.
     */
    public static List<String> availableClassNames() {
        List<String> out = new ArrayList<>(TROPAS.size());
        for (Class<? extends CartaTropa> c : TROPAS) out.add(c.getName());
        return out;
    }

    /**
     * Devuelve el nombre de clase totalmente cualificado (FQCN) de la instancia dada,
     * o null si la instancia es null.
     */
    public static String getClassName(CartaTropa carta) {
        if (carta == null) return null;
        return carta.getClass().getName();
    }

    /**
     * Selecciona n cartas aleatorias (instancias) usando Random provisto.
     */
    public static List<CartaTropa> aleatorias(int n, Random rnd) {
        List<Class<? extends CartaTropa>> copia = new ArrayList<>(TROPAS);
        Collections.shuffle(copia, rnd);
        int k = Math.min(n, copia.size());
        List<CartaTropa> out = new ArrayList<>(k);
        for (int i = 0; i < k; i++) out.add(crear(copia.get(i)));
        return out;
    }
}
