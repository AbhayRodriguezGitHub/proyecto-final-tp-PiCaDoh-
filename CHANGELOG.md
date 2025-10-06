# Changelog

## [0.00] - 2025-05-24

### Added
- Estructura base del proyecto creada con LibGDX (usando Liftoff).
- Carpeta `core`, `html` y `lwjgl3` creada.
- Integración inicial con GitHub.
- Inclusión de este archivo de cambios (`CHANGELOG.md`).
- Creación del proyecto Pi-Ca-Doh!.
- Configuración del entorno de desarrollo con Gradle.
- README inicial con descripción, tecnologías y pasos para compilar.
- Soporte para plataforma de escritorio (Windows).

## 0.01
Version de desarollador, incio de vista de menu, cursor (FrontEnd)
Desarollador (Costado superior izquierdo) Coordenadas para ayuda en el trabajo.

## 0.02
Agregado Menu de configuracion al tocar la tuerquita.
Agregado modo ventana y cambio de resoluciones. (Falta pulirlas, agregarle escabilidad a los checkboxs)

## 0.03
Modificaciones en Menu Configuracion
Arreglados varios problemas de checkbox
Sacado apartado de elegir resolucion, Resolucion escalable total al elegir modo ventana (Por defecto 1024x768)
Agregado apartado de configuracion de volumen que despues se implementara al igual que Manual

## 0.04
Modificaciones en Menu Principal
Boton Salir agregado
Musica de intro para menu principal y menu configuracion agregada

## 0.05
Modificaciones en Menu Configuracion:
(1) Slider de Sonido agregado y funcionando a la perfeccion.
Ahora se puede modificar el sonido del juego a como gustes.
(2) Seccion de modo ventana corregido
Ahora si lo marcas, sales de la configuracion y vuelves a entrar, la cruz del modo ventana seguira estando.

## 0.06 GRAN ACTUALIZACION.
Ya empezamos con la logica del MODO BATALLA.
(1) Logica al Boton Batalla
Ahora al tocar boton BATALLA te lleva a la Pantalla de seleccion de TROPAS
(2) Seccion de Eleccion de TROPAS
Logica Agregada a la pantalla de Seleccion de Tropas. Puedes elegir 15 Cartas y pasa a la siguiente Pantalla (Fase de Eleccion Tropa)
(3) Seccion de Eleccion De Efectos.
Logica al Flujo de esta seccion tambien agregada. Puedes Elegir hasta 7 Cartas entre 14 Opciones.
(4) Nuevos Recursos Agregados
- Pantallas De Elecciones
- 3 Cartas Tipo Tropa
- 5 Cartas Tipo Efecto
Cabe recalcar que no son la fase final de las imagenes, Se arreglaran los mini-errores visuales que tienen algunas de estas.

## 0.07
Agregada Nueva Musica.
Ahora la Seccion de Eleccion contiene una Musica Diferente!

## 0.08
Cambio de ARRAY a CLASES con HERENCIA (TROPAS Y EFEECTOS)
Arreglado error, los dos botones funcionan a la vez (Salir y Batalla)
La carta que elijes se imprime por consola y es exactamente la que eligiste en las fases de seleccion.
(Menos en los modo ventana, ahi siguen los errores)

## 0.09
Comenzando con la logica de Pantalla Batalla.
Se carga el primer campo de batalla (VALLE MISTICO)
Se cargan 2 Musicas para la parte de la batalla.
Se te asignan 3 cartas directamente a la mano, Alazar de las 15 que Seleccionaste!
- Las cartas aparecen en Negro, no se genera la imagen de la carta. Aunque si la tocas, se muestra por consola que carta seleccionaste.

## 0.010
Seguimos con la logica de Pantalla Batalla.
Ya Aparecen las imagenes de las cartas, ahora la carta se puede arrastrar por el campo y invocar en Cualquiera de las 5 Ranuras Dispónibles.

## 0.011
Pequeños Cambios.
Ahora puedes Invocar Tanto en las ranuras del Campo Rival como en las de tu Campo.
Ahora al invocar una carta no desaparece de tu mano, al invocarla 2 veces si desaparece de tu mano.
Esto pensandolo en la logica de desarollador, para tener tanto cartas enemigas como propias para empezar con la logica del modo batalla.

## 0.012
Ya agregamos sistema de Barra de Vida!
La misma siempre empieza con 80, Marcada como numero y con barra, a medida que vas recibiendo daños, la barra negra se va volviendo blanca y el numero se va restando!
Al llegar a 50 se pone amarillo y al llegar a 20 se pone rojo...
Ya se agrego el sistema de ATK y DEF, Las cartas atacan recto y se agrego iluminacion amarilla tenue por ranura en cada ranura que esta la batalla en juego.
Al Tocar ESPACIO o el BOTON PLAY de la Pantalla, Se Cambia la imagen de PLAY A En Juego mostrando que estan en FASE DE BATALLA!

## 0.013
Agregada funcion de TURNOS, del 1 al 22.
Agregada nueva Fuente para los Numeros en los Turnos. (ARIAL FAT.oft)
Corregido bug de que en medio de ejecucion batalla puedes invocar cartas... Cuando estas dentro de Batalla se bloquea el arrastre de las cartas de la mano.

## 0.014
Agregada funcion de RESTRICCION DE NIVELES POR TURNOS, del 1 al 22.
En base a la escala de niveles, ahora hay restriccion de cartas segun el turno que esta y el nivel disponible.
Ademas, se muestra en Pantalla con la fuente de arial.otf En Blanco, los niveles disponibles en el turno.

## 0.015
Agregada Pantalla Derrota y Pantalla Victoria.
Segun la condicion que se de, Si la vida del Usuario o la del Rival llega Primero a 0, Cambia a Determinada Pantalla. Con Imagen, Sonido y Opciones:
Volver al Inicio (Al Clickearlo vuelve al Menu Principal)
Creditos (Todavia nada, se piensa que al clickearlo se ponga un .mp4 que sean los creditos y despues vuelve al menu principal)

## 0.016
Agregada Cambio de Cara en la barra de vida,
Al Llegar a 50, 20 y 0 Puntos de Vida.
Tanto del Usuario como del Enemigo.
Al llegar a 0 no se ve la cara final (IMG: VIDA4.png( (Se piensa agregar un efecto de victoria o derrota antes de pasar ala siguiente pantalla, asi tardando unos segundos en la pantalla y se ve la cara final.)

## 0.017 GRAN ACTUALIZACION
Agregada Pantalla de Empate.
Agregada Muerte Subita en Ultimo turno, Si despues del turno 22 ninguno de los dos jugadores llega a 0 Puntos de vida: el que tiene menos vida se da por perdedor, el otro ganador, en caso de ambos tener misma vida despues del turno 22, Se da Empate.
PantallaEmpate con Imagen, Musica, y Boton Invisible de Regreso a PantallaMenu
Se creo la mecanica de Robo de cartas. Puedes tener hasta 7 cartas en mano de las que elegiste. ( Tanto Tropa [IZQUIERDA] como Efecto [DERECHA] ).
Agarras 1 cartas tropa por turno, cada multiplo de 3 agarras 1 tropa y efecto.
Si tienes 7 cartas tipo tropa, no puedes robar mas, hasta usar 1 carta desaparece y ahi podras volver a robar 1 carta al siguiente turno.
Se edito en PantallaSeleccionEfecto El tiempo, cuando eliges una carta se dan los mismos microsegundos de espera que la Seleccion de carta tropa, Hasta las siguientes dos cartas a elegir
Corregido error de Dispose() de PantallaSeleccionEfecto

## 0.018 GRAN ACTUALIZACION
Agregada logica a las Cartas efectos.
Agregada logica a la Ranura Efecto.
Ahora se pueden seleccionar las cartas tipo efecto, arrastrar por el campo, Invocar en la Ranura tipo Efecto, Y Las Cartas tipo efectos invocadas ya hacen Efecto a las cartas tipo tropa del jugador invocadas
(Solo Durante el Turno de Invocacion, Despues se AutoDestruyen, Y Los Efectos se van)

## 0.019
Cambios en PantallaBatalla:
Se añadio restriccion de invocacion de cartas, Solo se pueden Invocar 2 cartas tipo tropa por turno.
Restriccion No valida con efecto, osea puedes invocar 2 cartas tipo tropa y 1 carta tipo efecto sin problemas.

## 0.020
Agregadas Nuevas Cartas Tipo Tropa.
- Cambiado los Modelos de Base de Cartas, Tanto de Tipo Tropa como de Efecto.
Agregadas (4) Nuevas Tropas:
-ALKALINE -NAPPO -JUVERGOT -KONJISMA
Cambiado de Codigo en PantallaSeleccionTropa Agregado Registro de Cartas.
Mejorado, Centrado y Agrandado Las cartas en las Fases de Seleccion.

## 0.021
Agregadas (5) Nuevas Tropas:
-BARBARROZ -SAGA -CABEZAGOL -PLANTAGUERRA -BANDY

## 0.022
Agregadas (5) Nuevas Tropas:
-BARBILLON -EL CHAVO DEL 7 -ZAMBA -VIERNES 12 -LARRO

## 0.023
Agregadas (7) Nuevas Tropas:
-HORNERO -INDIANA LEGONS -MIFALDA -NARIO -NOSIC -TAZITOTA -TOROTO
( Completo Pack Inicial de CARTAS NIVEL 1 [19] )

## 0.024
Agregadas (17) Nuevas Tropas:
-ALFREDDO -BADABUN -BALLESTERO
-BULLYNERO -BOMBASTIC -BOCHINI -BIGOTE BRAVO
-DNK -EDGE -LIBERTAD -PHOENIX -RINYU -SLIM -TIRO DE MUERTE -TROLLBOX -VECINO -VERDOSO
( Completo Pack Inicial de CARTAS NIVEL 2 [19] )

## 0.025
Agregadas (15) Nuevas Tropas:
-50 PESOS -BLANDER -BROCO-LI
-BROMAS -CREED -DESTRUIDOR -FIERRO -GAIDEN
-GINGARA -JC -MARTSON -MONTANA -SINSONRISA -VENGANZA -X-MAN
( Completo Pack Inicial de CARTAS NIVEL 3 [16] )

## 0.026
Agregadas (13) Nuevas Tropas:
-AGENTE 46 -EL CHAPA -DANTA
-ESCORPION -INJERST -KENEDDY -KING -SAMUBAT
-SPAN -TINTON -ULTRACABALLERO -YAGA -ZOMGOD
( Completo Pack Inicial de CARTAS NIVEL 4 [14] )

## 0.027
Agregadas (11) Nuevas Tropas:
-AVATAR -CHYPER -MARADO
-OUROUN -QUTULU -SOBRINO -TENNYSON -TRANS -VERGAL -ÑENSEI -JANSINSKI
( Completo Pack Inicial de CARTAS NIVEL 5 [12] )
[COMPLETO PACK DE CARTAS TROPAS INICIAL: 80]
TOTAL TROPAS: 80
NIVEL 1: 19
NIVEL 2: 19
NIVEL 3: 16
NIVEL 4:14
NIVEL 5:12

## 0.028
Editado tipo de Imagen de las Cartas Efectos, Cambiado el Sistema de Orden de los mismos con RegistroEfectos (Aunque tambien se agregan desde PantallaSeleccionEfecto)
Agregado (1) Nuevo Efecto:
-TYSON

## 0.029
Agregadas nuevas funciones en ContextoBatalla como EfectosInstantaneo

Agregados (2) Nuevos Efectos:
-ANARQUIA DE NIVEL -BOMBARDRILO


## 0.030
Agregados (4) Nuevos Efectos:
-MONARQUIA -REBELION -MAL DE AMORES
 -MAGO DEL 8
Editado efecto -TYSON agregada funcion EsInstantaneo en el mismo.

## 0.031
Agregados (2) Nuevos Efectos:
-INTERCAMBIO -PARACETAMOL

## 0.032
Agregados (1) Nuevo Efecto:
-AVARICIOSO

## 0.033
Agregados (5) Nuevos Efectos:
-AGENTE DE TRANSITO -ESCUDO FALSO -ESCUDO PLATINADO -GANGSTERIO -ORIKALKUS
( Completo Pack Inicial de CARTAS EFECTO [20] )
[COMPLETO PACK DE CARTAS TOTALES DEL JUEGO BASE: 100 ]
(  -  80 Tropas y 20 Efectos  -  )

## 0.034
AGREGADA TABERNA
Agregada Funcion a Boton Taberna Base.
Dentro de Taberna ya estan las 10 Pistas Ambientales. Con la opcion de cambiar entre estos tocando la Radio.
Tambien la Flecha atras tiene su Funcion tocandola y volviendo al Menu Principal.

## 0.035 GRAN ACTUALIZACION - Agrego de JAVA FX -
Agregadas 4 Intros Antes del Comienzo del Juego.
Estas Se pueden saltar tocando Espacio, Enter, Click, ESC Y Otros.
Agregados todos y funcionando gracias al Nuevo Software Integrado (JavaFX) Que Permite reproducir Contenido multimedia
[En Este Caso Reproduce en nueva Ventana los .mp4 de Cada INTRO]


## --- Apartir de las Proximas Versiones, Todas estan Commiteadas en la Rama Master ---

## 0.036
Agregados los 5 SALONES DE LA FAMA (Del Nivel 1 al Nivel 5)
Donde puedes ver todas las cartas de determinado nivel en cada Salon. Agregado el Flujo entre cada uno de estos del 1 al 5 y a Volver a Taberna!

## 0.037
Solucionado error de Resoluciones en Computadoras Externas.
Ya puedes jugar el Videojuego en la Resolucion que quieras y no solo en 1920x1080, Gracias a la libreria Viewport se re-escala la imagen y los botones a la resolucion nativa de tu pantalla.
(   Ya se puede jugar sin problemas ni recortes en Las Computadoras del Colegio   )

## 0.038
Primer Pantalla de Presentacion Añadida.
PANTALLAS DE PRESENTACION AÑADIDAS (1)
- ALKALINE
Ahora en PantallaSalon1 Al tocar la carta de ALKALINE, se abre su pantalla de presentacion, Mostrando una imagen del misma en una pose epica, Con Su nombre en grande y su Historia.

## 0.039
PANTALLAS DE PRESENTACION AÑADIDAS (4)
-BANDY -BARBARROZ -CABEZAGOL -BARBILLON

## 0.040
PANTALLAS DE PRESENTACION AÑADIDAS (12)
-HORNERO -INDIANA LEGONS -LARRO -MIFALDA -NARIO -NOSIC -PLANTA GUERRA -SAGA -ZAMBA -TOROTO -TAZITOTA -VIERNES 12
 [ COMPLETADAS PRESENTACIONES Y HISTORIA DEL SALON DE LA FAMA 1 - BRONCE ]

## 0.041
PANTALLAS DE PRESENTACION AÑADIDAS (19)
-ALFREDO -BADABUN -BALLESTERO -BARBOT -BIGOTE BRAVO -BOCHINI -BOMBASTIC -BULLYNERO -D.N.K -EDGE -LIBERTAD -NAPPO -PHONIX -RINYU -SLIM SHABY -TIRO DE MUERTE -TROLLBOX -VECINO -VERDOSO
 [ COMPLETADAS PRESENTACIONES Y HISTORIA DEL SALON DE LA FAMA 2 - PLATA ]

## 0.042
PANTALLAS DE PRESENTACION AÑADIDAS (16)
-50 PESOS -BLANDER -BROCO-LI -EL BROMAS -CREED -DESTRUIDOR -FIERRO MARTIN -GAIDEN -GINGARA -JC -MAFIOSA ROSA -MARTSON -MONTANA -SIN SONRISA -VENGANZA -X-MAN
 [ COMPLETADAS PRESENTACIONES Y HISTORIA DEL SALON DE LA FAMA 3 - ORO ]

## 0.043
PANTALLAS DE PRESENTACION AÑADIDAS (14)
-AGENTE 46 -EL CHAPA -DANTA -ESCORPION -INJERST -JUVERGOT -KENEDDY -KING DADDY -SAMUBAT -SPAN -TINTON -ULTRA CABALLERO -YAGA -ZOMGOD
 [ COMPLETADAS PRESENTACIONES Y HISTORIA DEL SALON DE LA FAMA 4 - DIAMANTE ]

## 0.044

PANTALLAS DE PRESENTACION AÑADIDAS (12)
-AVATAR -CHYPER -JANSINSKI -KONJISMA -MARADO -ÑENSEI -OUROUN -QUTULU -EL SOBRINO -TENNYSON -TRANS -VERGAL
 [ COMPLETADAS PRESENTACIONES Y HISTORIA DEL SALON DE LA FAMA 5 - RUBI ]
{ SALON DE LA FAMA COMPLETO: 80 HISTORIAS COMPLETAS, IMAGENES DE PRESENTACION DE TODAS LAS CARTAS DEL JUEGO HASTA LA FECHA ECHAS. HISTORIAS INTERCONECTADAS UNAS CON OTRAS, COMPARTIENDO PLANETAS DEL MULTI-VERSO DE PICADOH, TODAS UBICADAS EN SAN PEDRITO }

## 0.045
- Agregado Pantalla de Limite de Contenido (Con Respecto a Pantalla Rangos)
- Agregado Comienzo de Historia de Libro. ( Solamente Pagina 1 )


## 0.046
- Agregada TODA la historia del Libro Perdido PICADOH en Taberna (16 Paginas)

## 0.047
- Agregada Pantalla de Creditos en Taberna, Todavia no funciona al 100% (No para el audio de taberna cuando entra al Video)

## 0.048
- Agregada Funcion de Pausar Musica con CNTRL + P
- Agregada Funcion de Renudar Musica con CNTRL + T
Esto para ver los creditos sin problema ni musica de por medio.

## 0.049
- Cambiadas las imagenes de Condicion de: VICTORIA, DERROTA Y EMPATE.
- Ahora en cualquier de estas pantallas puedes decidir si: Volver al inicio o Ir a la taberna.

## 0.050
- Agregado MANUAL en PantallaConfiguraciona
- Agregado ICONO en la Ventana del Juego

## 0.051
Se han deshabilitado permanentemente (2) funciones de testing que permitían saltar las reglas de juego durante las pruebas internas.
Restricción de Invocación de Cartas:
Comportamiento anterior (Tester): Se permitía la invocación de cartas en el campo propio y en el campo enemigo.
Comportamiento actual (Juego): La invocación ahora está limitada estrictamente a las cinco (5) ranuras asignadas al campo del jugador.
Uso de Cartas:
Comportamiento anterior (Tester): Las cartas persistían en la mano para un segundo uso antes de ser descartadas.
Comportamiento actual (Producción): Toda carta invocada en una ranura se remueve de la mano inmediatamente (uso único).
[ NOTA: Se mantiene activo el Visor de Coordenadas por las dudas, Aunque en las Proximas Versiones ya encaramos al Juego en RED - LAN / JUEGO BASE COMPLETO ]

## 0.052
Agregadas todas ACTUALIZACIONES del CHANGELOG de la version 0.11 a 0.52 INCLUSIVE.

## --- Apartir de las Proximas Versiones, Todas estan en "0.1XX" , "1" Es por que ya se Empieza con la RED. Despues sigue el numero de versionado normal. ---

## 0.153 

Comienzos de RED LAN - Fases de Eleccion
Ahora al ejecurar el LWJGL3 Launcher lo toma como un SOCKET, CLIENTE1 (Esta Permitido ejecutar varias instancias) Lo Ejecutas Otra vez, CLIENTE 2.
Los dos se conectan mediante ServidorLAN.java
Al tocar batalla pasas a la Seleccion de Tropa, una vez elegidas las 15, pasas ala seleccion de Efecto, una vez elegido los 7, Aparece el Mensaje: "Esperando Rival..." una vez CLIENTE 2 elija todo lo suyo, Se pasan los 2 a PantallaBatalla.
(PantallaBatalla Todavia no esta en RED)




















