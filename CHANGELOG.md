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



