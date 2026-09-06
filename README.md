## The DMOTE application

This is a CAD program for designing a keyboard that suits you. By editing [text
files of parameters](doc/intro.md), you can change:

- Switch type: ALPS, Kailh (PG1511) or MX.
- Microcontroller type: Lots of options.
- Size and shape.
    - Row and column curvature and tilt (tenting).
    - Exceptions at any level, down to the position of individual keys.
- Minor features like LED strips and wrist rests.

### Bundled designs

Here are two of the application’s bundled designs for concave, columnar,
ergonomic keyboards, as examples of what you can do:

[![Image of the second working
Concertina](https://viktor.eikman.se/image/concertina-2-front-view/display)](https://viktor.eikman.se/gallery/the-concertina/)

⤤ The [Concertina](https://viktor.eikman.se/article/the-concertina/), a diploid
keyboard with 20 thumb keys and 3 assortment drawers.

[![Image of the second working DMOTE](http://viktor.eikman.se/image/dmote-2-top-down-view/display)](https://viktor.eikman.se/gallery/the-dmote/)

⤤ The [DMOTE keyboard](http://viktor.eikman.se/article/the-dmote/), short for
“Dactyl-ManuForm, Opposable Thumb Edition”.

A version of the 46-key Dactyl-ManuForm, based on [Tom Short’s
original](https://github.com/tshort/dactyl-keyboard), is another bundled
design, and the default build target.

### Related projects

The DMOTE application is one of many forks in the
[Dactyl](https://github.com/adereth/dactyl-keyboard) genus. There’s a small
family of accessory projects for the application:

* [`dmote-keycap`](https://github.com/veikman/dmote-keycap):
  A library used directly by the DMOTE application to model both featureless
  maquettes for use in easily rendered previews, and “minimal” keycaps you can
  print and use to save space.
* [`dmote-topography`](https://github.com/veikman/dmote-topography):
  Smooth organic topography for height maps you can use as wrist rests,
  including the example bundled in the [resource folder](resources/heightmap)
  of this project.
* [`dmote-drawer`](https://github.com/veikman/dmote-drawer):
  Assortment drawers for the Concertina, a bundled keyboard design.
* [`dmote-beam`](https://github.com/veikman/dmote-beam):
  Configurable clips for stabilizing a split keyboard by holding a
  connecting rod between the two halves.

Like its parent, the DMOTE project is based on Matthew Farrell’s
[`scad-clj`](https://github.com/farrellm/scad-clj). A second small family of
general CAD libraries for `scad-clj` have grown up around the DMOTE project.

* [`scad-app`](https://github.com/veikman/scad-app): A simple way to turn
  `scad-clj` specifications into applications, stripping out some boilerplate.
  See also [`cad-template`](https://github.com/veikman/cad-template), which is
  all boilerplate for starting new CAD projects that are ready to render.
* [`scad-tarmi`](https://github.com/veikman/scad-tarmi): Commonplace abstractions.
* [`scad-klupe`](https://github.com/veikman/scad-klupe): Threaded fasteners.

### License

Copyright © 2015–2026 Matthew Adereth, Tom Short, Viktor Eikman et al.

The source code for generating the models (everything excluding the
[things/](things/) and [resources/](resources/) directories) is distributed
under the [GNU AFFERO GENERAL PUBLIC LICENSE Version 3](LICENSE). The generated
models and PCB designs are distributed under the [Creative Commons
Attribution-NonCommercial-ShareAlike License Version 3.0](LICENSE-models).
