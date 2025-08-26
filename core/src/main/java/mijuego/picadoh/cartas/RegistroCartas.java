package mijuego.picadoh.cartas;

import java.util.*;
import java.lang.reflect.Constructor;

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
        ZomGod.class



    );

    public static List<Class<? extends CartaTropa>> tropasDisponibles() {
        return TROPAS;
    }

    public static CartaTropa crear(Class<? extends CartaTropa> clazz) {
        try {
            Constructor<? extends CartaTropa> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo instanciar " + clazz.getSimpleName(), e);
        }
    }

    public static Optional<CartaTropa> crearPorNombre(String nombre) {
        for (Class<? extends CartaTropa> c : TROPAS) {
            if (c.getSimpleName().equalsIgnoreCase(nombre)) {
                return Optional.of(crear(c));
            }
            try {
                CartaTropa tmp = crear(c);
                if (tmp.getNombre().equalsIgnoreCase(nombre)) {
                    return Optional.of(tmp);
                }
            } catch (RuntimeException ignored) {}
        }
        return Optional.empty();
    }

    public static List<CartaTropa> aleatorias(int n, Random rnd) {
        List<Class<? extends CartaTropa>> copia = new ArrayList<>(TROPAS);
        Collections.shuffle(copia, rnd);
        int k = Math.min(n, copia.size());
        List<CartaTropa> out = new ArrayList<>(k);
        for (int i = 0; i < k; i++) out.add(crear(copia.get(i)));
        return out;
    }
}
