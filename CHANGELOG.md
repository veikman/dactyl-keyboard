# Change log
This log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/). It picks up from DMOTE
version 0.2.0, thus covering only a fraction of the project’s history.

## [Unreleased]
### Changed
- The default type of microcontroller has changed, from `promicro` to
  `rpi-pico`.
- Modified the design of keycaps on the Concertina, using and matching
  the full set of caps bundled with `dmote-keycap` v0.8.0.
- The `ports` feature has been generalized.
    - The port type `custom` has been renamed to `custom-cuboid`
      to accommodate the new `custom-cylindroid` type of port.
    - The default port-holder `thickness` has changed from 1 mm to 0
      to accommodate the new `through-hole` type of holder.
    - Ports no longer face nominal north (away from the user) by default.
      Instead, they face upward. This is mainly so that `size` specifications
      for the `custom-cylindroid` type makes sense, but it also makes it easier
      to add gadgets other than switches, such as rotary encoders, to the top
      of the keyboard.
- Started using YAML anchors in bundled configuration files for deduplication.
  This is not a change in application logic, API or design; the YAML parser
  expands each reference to the same effect as before.

### Added
- The port type `custom-cylindroid`. This enables round ports.
- The port-holder type `through-hole`. This enables ports that are walled,
  but not at the bottom.
- The MCU type `rpi-pico`.
- Bundled designs:
    - Added a `kailh.yaml` file for PG1511-style switches on a
      Concertina.
    - Added an `encoder` folder for rotary encoders on a Concertina,
      and a 66-key build variant to use them.
- Conical tops for the bosses of heat-set inserts. This is intended to prevent
  slicing software from adding support material inside the hollows.

### Fixed
- Fixed problems related to keycap size by upgrading to v0.8.0 of the
  `dmote-keycap` library.

## [Version 0.7.0] - 2021-04-10
### Changed
- Switched the default build target (with GNU Make) from the DMOTE to the
  Dactyl-ManuForm.
- More precise control.
    - Replaced the way that key-cluster walls are measured. Instead of separate
      settings for `bevel`, `parallel` and `perpendicular`, there is now only
      one setting, called `segments`, with more power, including the ability to
      vary size (was `thickness`) by segment.
    - Integrated bottom-plate fasteners and their bosses with the `flanges`
      section of configuration introduced in v0.6.0. This invalidates most of
      the `bottom-plate` settings that were previously available under
      `main-body`, `central-housing` and `wrist-rest`, but adds more power:
      Customizable boss `segments` richer than those of keys, heat-set
      inserts universally available, the option of different kinds of screws
      for different bottom plates (e.g. shorter in wrist rest) etc.
- Fewer side effects.
    - Disabled responsiveness to predicted resting key clearance by default.
      This can be re-enabled with a new parameter (`use-key-style`).
    - Disabled responsiveness to flange screw size (including the “new”
      bottom-plate flanges) for making flange bosses.
    - Changed the meaning of the `at-ground` parameter for tweak nodes,
      so that hulling to ground is no longer a side effect of it.
      Added a corresponding, explicit `to-ground` parameter.
- Made `false` the default value of all Boolean parameters.
    - Stopped including arbitrary shapes for tweaks in `above-ground` contexts
      by default.
    - Replaced `positive` for arbitrary shapes with an inverted `cut` parameter.
    - Set `include-threading` for bolts to `false` by default.
- Completed migration from long-form names for the points of the compass,
  like `north`, to short-form names, like `N`. This leaves only short forms,
  so the distinction itself is abolished.
    - The shorter names are now used for nested configuration by key.
    - The shorter names are used for fasteners through the rear housing,
      under a new section (`sides`).
    - The longer names, previously optional for MCU shelf sides, are no
      longer permitted there.
- Moved and replaced some (other) parameters:
    - Made the `central-housing` → `adapter` → `receivers` → `thickness` →
      `bridge` parameter a section, with its function inherited by `tangential`
      in that new section.
    - Renamed MCU shelf `sides` to `rim` to prevent confusion with the
      adjacent `bevel` section of parameters, which is indexed by sides
      (compass points).
    - Replaced central-housing interface settings for `adapter` → `offset`
      with a `segments` map, as for key-cluster walls and flange bosses.
    - Replaced the `positive` property of tweaks (default true) with a `cut`
      property (default false). Its effects are identical, but the values are
      inverted.
    - Migrated to `scad-app` v1.0.0, which includes renaming the
      `minimum-face-size` parameter to `minimum-facet-size`, even in the DMOTE
      configuration interface.
- Dropped support for arbitrary YAML inclusions through GNU Make.
- Bundled designs:
    - Stopped including threading on bolts in most models, for faster renders
      and reduced sensitivity to printer accuracy.
    - M3 screws instead of M4 in `config/base.yaml`.
    - DMOTE:
        - Slightly thicker bottom plate.
        - Smaller bosses for screws attaching the wrist’s bottom plate.
    - Concertina:
        - Reduced to one size of keycaps.
        - Improved USB B port and MCU shelf.
        - More tactile, variegated thumb clusters.
        - Pinky-finger and thumb-cluster keys closer.
        - Deeper, fewer holes through bottom plate.
    - Slight tuning of DFM settings for more common printers and more modern
      slicers.

### Added
- Added support for Kailh’s PG1511 series switches and similar MX clones
  without lateral recesses in the lower body.
- Added a nominal clearance parameter to make it easier to design for multiple
  different types of switches and keycaps.
- Added support for custom key mounting plate size.
- Added several new optional behaviours for arbitrary shapes (`reflect`,
  `to-ground`, `shadow-ground`, `polyfill`).
- Extended the concept of a combined bottom plate to include the central
  housing.
- Added a parameter for central-housing interface fastener-receiver radial
  thickness.
- Added thinning of central-housing adapter lips based on DFM error setting.
- Added precise control over segments for each node on the central housing
  interface’s adapter.
- Added a parameter for disabling all threading to improve low-resolution
  rendering performance.
- Added a tutorial for getting started designing from scratch.
- Bundled designs:
    - Added a configuration fragment for removing the rear housing of the DMOTE.

### Fixed
- Made the application of the DFM `error-general` setting to bolts more
  consistent.

### Migration guide
Here is an example of adaptation from the old wall-building syntax to the new:

```diff
-      parallel: 4
-      perpendicular: -16
+      segments:
+        "2":
+          intrinsic-offset: [0, 4, -16]
```

That is an excerpt from `config/macropad/base.yaml`, preserving the shape of
the wall.

To preserve the old behaviour of a tweak node that should affect the keyboard
case body, add `above-ground: true` to it. Where you had `positive: false`,
replace it with `cut: true`. Where you had `at-ground: true`, replace it with
`to-ground: true`.

Migrating bottom-plate settings is a lot more complex, owing to the volume of
changes. Please inspect bundled configurations.

## [Version 0.6.0] - 2020-09-09
### Changed
- Moved and replaced some parameters:
    - The `case` section of parameters was **renamed** to `main-body` to
      avoid ambiguity with respect to other bodies, a new concept.
        - The top-level parameter `split` was both moved into the `main-body`
          section and renamed to `reflect` to avoid misleading the user about
          how it interacts with the new `central-housing` feature.
        - The `case-side` style of mounting wrist rests was similarly renamed
          to `partner-side`, and `plinth-side` to `wrist-side`.
        - The `tweaks` parameter has moved out of `main-body` to the top level,
          because it can now be used for other bodies.
    - Multiple sections for anchoring have been **renamed** from `position` to
      `anchoring` to reduce ambiguity with respect to bodies.
        - Parameters named `corner` have been **renamed** to `side`. This is
          because they now take codes for cardinal compass points as well as
          actual corners.
        - The single `offset` parameter has been split into intrinsic and
          extrinsic parameters.
        - The parameter governing how to source the angle of fasteners for a
          wrist rest mount has been renamed from `anchoring` to `authority` to
          remove ambiguity.
    - Parameters for key-cluster walls have been restructured.
        - Parameters formerly sorted under side-specific settings have been
          promoted to nest directly under `wall` and can now be used without
          selecting a side.
        - As a consequence of more flexible selection criteria, the `bevel`
          setting can now be side-specific.
        - Wall `extent` is now strictly numeric. Extending a wall to ground
          is governed by the new parameter `to-ground` and can now be done
          from any segment, no longer just segment 4.
        - The vertical segment sequence for key walls has been shortened and
          simplified. Wall `thickness` has been expanded into three dimensions,
          replacing three parameters under `main-body`:
          `key-mount-corner-margin`, `key-mount-thickness` and `web-thickness`.
    - All parameters governing individual properties of threaded bolts have
      been removed in favour of more powerful new parameters based on options
      exposed by a new library (`scad-klupe`) that draws bolts for the
      application.
      This change provides greater freedom to choose different bolt head types,
      partial threading, no threading (with a diameter suitable for tapping
      holes manually) etc.
    - The `connection` section has been replaced by a general `ports` map.
    - Some parameters governing the mounts of wrist rests have changed to
      strengthen the tweaking system.
        - The fastener `amount` parameter and `height` section have been
          replaced by a list of `heights`, no longer requiring even spacing
          by an `increment` (pitch).
        - The `aliases` section has been moved up one level, out of `blocks`.
        - Parameters governing nut pockets and bosses have all been removed.
          These parameters are replaced by a stronger tweak subsystem where
          you can name individual nuts and target them as negative space,
          creating pockets and bosses more freely.
        - The `angle` parameter has changed in meaning by a quarter turn,
          generally making configured angles smaller and easier to reason
          about, as well as putting mount width on the x axis for easier,
          more consistent tweaks.
    - Heat-set inserts for attaching bottom plates are no longer a separate
      style. Thus the `inserts` option has been removed from the `main-body` →
      `bottom-plate` → `installation` → `style` parameter and replaced by a new
      `include` parameter, governing the same feature independently of style.
- Removed much of the special treatment of the rear housing, no longer needed.
    - Removed the automatic webbing to key mounts.
    - The two parameters named `prefer-rear-housing` and their associated
      functionality, and the `cluster` parameter, were all **removed**, having
      been obviated by normal placement in relation to a wide range of anchors.
    - The `raise` parameter for `connection` has been replaced by an extension
      of the concept of vertical segments to the rear housing.
    - Removed `into-nook`, which automated some fine tuning for placing ports
      and MCUs inside the rear housing. This has been obviated in part by the
      corner-to-side change, which allows the user to target an edge rather
      than a corner of the rear housing, and in part by the extension of
      segments on the rear housing to include segment 2, referring to floor
      level beneath the walls.
    - More direct and exact control of rear-housing size, thickness and
      bevels, using new parameters.
- MCU supports have changed, gaining more power but losing some ease of use,
  to work better with the option of central housing.
    - The default orientation of the MCU PCB has changed, from standing on its
      long edge to lying flat.
        - In the Dactyl-ManuForm model, the MCU now lies flat, whereas in other
          pre-v0.6.0 bundled build targets, it’s still on its long edge because
          of accompanying changes to the configurations.
    - The MCU support style parameter was **removed**.
        - Instead of `lock` and `stop` existing as mutually exclusive styles,
          with the requirement of a key mount as the anchor for a stop, `stop`
          has been removed, named points on or in space around the MCU can now
          be connected using `tweaks`, and such a grip can be freely combined
          with a lock. See the Dactyl-ManuForm’s `mcu-gripper.yaml` for an
          example of how to use tweaks to replace a stop.
    - `mcu` → `support` → `height-factor` was moved to
      `mcu` → `support` → `lock` → `width-factor`.
    - `mcu` → `support` → `lateral-spacing` was moved to
      `mcu` → `support` → `lock` → `plate` → `clearance`.
- Some default values have disappeared altogether.
    - When anchoring a feature to a key mount, there is no longer a default
      value for the vertical segment of the key mount. The previous default was
      3.
    - The default `side` (previously `corner`) of a target anchor is now `nil`
      (YAML: `null` or omission), meaning the centre (no side).
- Whether or not anchoring one feature to another imposes rotation is now more
  consistent and more directly controllable.
    - The MCU's `rotation` parameter has been removed in favour of richer
      general anchoring options.
    - 2D features that stick to the floor now get less special treatment and no
      longer take 2D offset coordinates.
- The default size of a tweak has changed from the size of a key-mount corner
  post to a nodule of 10⁻⁹ mm³, effectively a mere point. This matches the
  way that the rear and central housings, and some other features, are now
  drawn: As polyhedra made up of points rather than bodies with volume.
  This change was precipitated by moving key-mount thickness control out of
  its privileged position on `main-body`.
- The alcove generated for the front end of an MCU PCBA now uses the general
  DFM setting (`error-general`). The `mcu` → `margin` setting was removed.
- Anchors for bottom-plate mounting screws no longer have domed caps by
  default. These are still available but are governed by a new parameter.
  Domes were made optional to make it easier to tap threads rather than print
  them, and use longer screws than was originally intended.
- Mounts for wrist rests no longer have peaked caps by default. Instead, tops
  are bevelled like the sides, and the body of the mount has its nominal size
  at its base instead of at its top only, to help with tweaking.
- The `foot-plates` feature has been removed. Its functionality is now
  relatively easy to replicate with `secondaries` and `tweaks`.
- File names have changed where they correspond directly to bodies.

### Added
- Documentation:
    - An execution guide, as a new document branched off from the introduction.
    - A general guide to concepts in the configuration layer.
    - A guide to anchoring parameters.
    - A guide to arbitrary shapes.
    - Tables of content in auto-generated documents.
    - Stock descriptions of recurring parameters.
- Support for a number of different types of MCUs beyond the Pro Micro:
  Common Teensies as well as the Elite-C and Proton C.
- A formal concept of bodies, making it possible to choose which OpenSCAD
  output file to target for a given tweak etc.
    - Central housing, a new feature adding a body separate from the main body.
    - Custom bodies.
    - Flanges: Arbitrarily positioned screws for connecting custom bodies to
      their parent bodies.
- Improved anchoring.
    - Homogeneous anchoring parameters, including new intrinsic and extrinsic
      translation and rotation of all anchorable features.
        - Full anchoring parameters for key clusters, wrist-rest mounts etc.
        - The ability to preserve orientation in anchoring without working via
          a secondary.
    - Anchoring to the MCU PCBA.
    - Anchoring to a wider array of the parts of a key mount, including
      different sides of its wall, using cardinal and intercardinal compass
      points, whereas before, only points intermediate between cardinals and
      intercardinals could be used.
- The `by-key` section of parameters has become more flexible.
  It is now possible to influence all keys on a row, without duplicating
  settings for all columns in that row.
    - `sides` is now a selection criterion just like `clusters`, `columns` and
      `rows`, matching the general strengthening of the concept of sides in
      this version.
- An MCU shelf. This type of MCU support corresponds directly to the
  Dactyl-ManuForm’s `teensy-holder` object and is therefore not new, but
  it has some parameters to extend its functionality.
- A new `backing-thickness` parameter to help control how far way from an MCU
  lock its fastener will extend.
- Support for an arbitrary number of ports.
    - Support for standard types of ports, including different USB connectors
      and a modular connector (616E for 4P4C, previously emulated in
      configuration).
- Extensions to bottom plates for projections of the anchors used to fasten
  such plates to the case. This restores a feature of the upstream
  Dactyl-ManuForm.
- Enhancements to `tweaks`.
    - Negative tweaks for subtracting material. So far, these are only used for
      3D tweaks to the main body and central housing but they could easily be
      extended to wrist rests and bottom plating.
    - The ability to target ports, including port holders, and the plate of an
      MCU lock, including a set of vertical segments. The rear housing has also
      gotten vertical segments.
    - Intrinsic rotation.
- Support for specifying sizes with less than 3-tuples.
- Support for specifying angles with mathematical formulae using π.
- A `size` property of `secondaries`, for terser `tweaks`.
- The ability to override specific coordinates for secondary named positions.
- A GNU Make target for the Dactyl-ManuForm.

### Fixed
- More accurate and printer-friendly spaces for the wings on ALPS-style
  switches.
- Respect for key spacing settings without curvature.
- Fixed a regression that made the interior of case walls too thick.
- Made `transpile.sh` sensitive to configuration changes again.
- A more categorical fix for `dmote-keycap` parameter support, achieved by
  migrating that library’s parsing logic into the library itself.
  See version 0.5.1.
- Better placement of heat-set inserts. More careful interaction with
  a plate offset and with the bottom plate.
- Slightly improved user feedback when configuration files contain structural
  problems.

### Developer
- Improved REPL support.
- New namespaces:
    - `anch`, collecting collectors of anchor points.
    - `body`, collecting logic specific to the new concept of bodies.  Code
      previously in the single-file `body` module, but not specific to the main
      body, moved into the `key` and `mask` namespaces. The `key` namespace
      branched into a package of several modules as a result. A new `assembly`
      module within `body` took the composition functions from `core`.
    - `compass`, gathering code from `generics` and `matrix` with refactoring
      to improve the compass metaphor for feature placement.
    - `cots`, gathering information on commercial off-the-shelf goods.
    - `mask`, taking the above-ground mask function out of the main body
      module, and the bottom-plate masks too.
    - `mcu`, breaking MCU features out of `auxf`.
    - `misc`, which collects everything that remained of `generics` after
      compass code moved out. This makes two `misc` modules.
    - `poly`, collecting helper functions for making polyhedra.
    - `tweak`, breaking tweak plating out of `body` and `bottom`.
    - Split the `schema` module into separate modules for parsers, specs
      and reusable stock structures.
- The parameter interpreter now bans `nil` only as a function of explicit
  validators, no longer categorically.
- Removed functions from the derived user configuration.
- Added a folder of configuration files under `test/config` for manual
  regression testing.

### Migration guide

Compare versions of `config/dmote/base.yaml` to see how to migrate your old
configuration files. Salient points:

- Move a setting like `parameters` → `wall` → `north` → `extent` into `sides` →
  `north` → `parameters` → `wall` → `extent`.
    - Replace a `full` extent with `3` and set `to-ground` to `true`.
- Replace each `fasteners` → `diameter` with a `bolt-properties` → `m-diameter`
  setting, and each bolt length setting with `bolt-properties` → `total-length`
  or `threaded-length`, depending on whether you want the head to count towards
  length. For the MCU lock, the term is `fastener-properties` to avoid
  confusion with the bolt of a lock.
- To compensate for the changed default orientation of the MCU in an existing
  custom configuration, use the moved `intrinsic-rotation` setting for your MCU
  support, with the value `[0, π/2, 0]`.
- Rename `corner` parameters to `side`.
- Rename `anchoring` → `offset` to `intrinsic-offset`, unless you want to make
  sure it happens in global vector space, in which case rename it to
  `extrinsic-offset`.
- For features that were not previously rotated with their anchors and should
  not be, add `preserve-orientation` to `anchoring`.
- Remove `connection` and add equivalent settings to the new `ports` map.

## [Version 0.5.1] - 2019-10-16
### Fixed
- Added a parser for one more of `dmote-keycap`’s parameters (`supported`),
  thus allowing supports to be turned off for keycap models rendered through
  this application.

## [Version 0.5.0] - 2019-07-21
### Changed
- Secondary aliases (`secondaries`) are now a map and case `tweaks` are
  likewise nested underneath a layer of names. Both of these structural
  changes add power to the configuration layer, reducing the need for
  duplication of data.
- Fixes that make the height of the case easier to manage constitute changes
  to previous behaviour:
    - Key mounting plates, which were previously located immediately on top of
      the nominal position of each key, are now below the nominal. In other
      words, key mounting plates have dropped down by the thickness of the
      mounting plates. This means that, on a completely flat keyboard, the
      configured height of each key mount is now the height of the case.
        - When you change key mount thickness, the difference is now internal
          to the keyboard and will no longer affect the relative positions of
          the switches.
        - To match this change, mount thickness is no longer a factor in
          computing curvature. This in turn affects the nominal positions of
          keys. It effectively recalibrates the scale of matrix separation,
          correcting it so that the default value of value of zero is more
          likely to create a good design.
    - Webbing and case walls etc. are (still) governed by key mounts, so they
      have also dropped down.
    - Similarly, the configured height of the rear housing is now its actual
      height, not the centre height of the cuboids that make up its corners.
- Moved bundled YAML.
    - The entire `resources/opt` folder is now at `config`.
    - Most of `resources/opt/default.yaml` has been renamed (to
      `config/dmote/base.yaml`) and is now less privileged.
    - A simple `make` has the same effect as before but passes more
      arguments to achieve it. Without arguments, the Clojure
      application now describes an unusable single-button keyboard.
- Changes to the bundled 62-key DMOTE configuration:
    - Switched from M3 to M4 screws for attaching the bottom plate and for
      locking the MCU PCB in place. This makes for quicker previews and easier
      printing.
    - Switched from flat to conical points for bottom-plate fasteners, just
      so the holes are easier to slice without getting interior supports.
    - Minor tweaks, like renaming the `maquette-dsa` style to `default`
      and removing a secondary anchor obviated by new controls for rear-housing
      post thickness.
- Replaced the nut boss in an MCU lock bolt with printed threading of the hole.

### Added
- Key mounting plates responsive to key `unit-size` for ease with oblong keys.
- A bundled 12-key macropad configuration, mainly as an object lesson.
- A `split` parameter at the highest level. This is false by default and absent
  in `config/base.yaml`, to enable macropads and relatively regular keyboards.
  It’s true in `config/dmote/base.yaml`.
    - An `include` parameter for the connection metasocket. This is false by
      default and true in `config/dmote/base.yaml`. Its effect is contingent
      upon the `split` parameter.
- An `include` parameter for MCU PCBA support. This is false by
  default and absent in `config/base.yaml`, mainly to enable tutorials where
  the MCU support does not pose a distraction, and partly to allow custom
  alternatives to the supported styles.
- More finely grained control for dimensions previously governed by general
  case webbing thickness.
    - New parameters for rear-housing wall and roof thickness.
    - A new parameter for connection socket wall thickness.
- A new implementation of the `solid` style of wrist rest attachment,
  with a bundled example configuration powered by the new structure of
  `secondaries` and case `tweaks`. In the example, the case and wrist rest
  are one piece of plastic. In a previous implementation, removed in version
  0.3.0, the two were separate pieces that snapped together, which put more
  requirements on the shape of the case and was not useful enough.
- A new DFM parameter, `fastener-plate-offset`, for tighter holes through
  bottom plates.
- A new bundled configuration fragment, `config/dmote/mx.yaml`, imposing
  MX-style switches on the DMOTE.
- More features added to `config/dactyl_manuform/base.yaml`,
  reconstructing part of the classic upstream shape as a configuration.
- A primitive means of combining YAML files by passing them as targets to Make.
  This does not work as intended and may be removed in future.

### Fixed
- Renamed a file (from `aux` to `auxf`) to work around file system
  restrictions inherited from MS DOS into current versions of Windows.
- Corrected placement of wrist-rest fastener anchors for the thickness of the
  bottom plate.
- Fixed a bad function call for `stop`-style MCU support.
- Slightly more accurate models of switches.

### Developer
- In the interest of versatility, the Clojure code no longer refers to any YAML
  files. Instead, the default configuration values that are built into the code
  itself are slightly richer, to prevent crashing without YAML files.
- Restructured the makefile, renaming some of the phony targets (e.g.
  `visualization` to `vis`) and removing others for the present. `make all` no
  longer exercises as much of the code.

## [Version 0.4.0] - 2019-06-06
### Changed
- Moved and replaced some parameters:
    - Both the `keycaps` and `switches` sections have been replaced. There is
      now a `keys` section that defines one or more `styles`, as well as a
      `key-style` parameter in the nesting `by-key` section of parameters.
    - The `to-ground` key for case tweaks has been renamed to `at-ground` for
      clarity with respect to a new `above-ground` key.
    - The general `error` parameter for DFM has been renamed `error-general`.
- Changed default.yaml from a 60-key layout with a user-facing 1-key `aux0`
  cluster to a 62-key layout (31 on each half) with a 2-key `aux0` cluster at
  the opposite corner and facing away from the user.

### Added
- Support for multiple, named styles of keys.
    - This includes some with enough detail on the keycaps to permit printing.
      These printable models are now among the outputs of the application.
- Improved case `tweaks`.
    - Tweaks are no longer restricted to key aliases.
        - Any named feature can be used in a leaf node.
        - It is no longer necessary to specify a corner or segment for a tweak.
    - Tweaks can now target bottom plates without impacting case walls.
      There is a new configuration key for this (`above-ground`, which must be
      turned off to see the new behaviour).
    - Added a `secondaries` parameter for named features that are just points
      in space, placed in relation to other named features. Used in `tweaks`,
      these secondaries give greater freedom in shaping the case.
- Added a `resolution` section to the parameters.
    - By default, already-existing resolution parameters (both of them are for
      wrist-rest pads) will be disabled by a default-negative new `include`
      parameter in the new section.
    - The new section also provides a means of rendering curved surfaces
      elsewhere in more detail.
- Added a `thickness` parameter specific to the threaded anchors for
  bottom-plate screws.

### Fixed
- Improved the fit of a bottom plate for the case, at the cost of
  greater rendering complexity.
- Reduced risk and impact of collision between nut bosses built into the
  rear housing and the interior negative of the socket for connecting the
  two halves, by reducing the thickness of one part of the negative.

### Developer
- Took advantage of new developments in general-purpose libraries:
    - Outsourced file authoring to `scad-app` for improved CPU thread scaling,
      rendering feedback and face-size constraints.
    - `scad-tarmi` lofting replaced `pairwise-hulls` and `triangle-hulls`.
    - `scad-tarmi` flex functions obviated several separate functions for
      object placement and reasoning about that placement.
    - `scad-tarmi` coordinate specs replaced locals.
    - Featureful `dmote-keycap` models replaced internal maquettes.
- Made the rosters of models and module definitions reactive to the
  configuration.
    - Converted keycaps and switches to OpenSCAD modules.
- Changed the merge order in the `reckon-from-anchor` function to make
  secondaries useful in tweaks.

## [Version 0.3.0] - 2019-02-18
### Changed
- Moved and replaced some options:
    - Dimensions of `keycaps` have moved into nestable `parameters` under
      `by-key`.
    - `key-alias` settings have been merged into `anchor`. `anchor` can now
      refer to a variety of features either by alias or by a built-in and
      reserved name like `rear-housing` or `origin`. In some cases, it is now
      possible to anchor features more freely as a result.
    - Moved `case` → `rear-housing` → `offsets` into
      `case` → `rear-housing` → `position`.
    - Moved `case` → `rear-housing` → `distance` into
      `case` → `rear-housing` → `position` → `offsets` as `south`.
    - Renamed the `key-corner` input of `case` → `foot-plates` → `polygons`
      to `corner`.
    - Removed the option `case` → `rear-housing` → `west-foot` in favour of
      more general `foot-plates` functionality.
    - Removed `wrist-rest` → `shape` → `plinth-base-size` in favour of settings
      (in a new `spline` section) that do not restrict you to a cuboid.
    - Removed `wrist-rest` → `shape` → `chamfer`. You can achieve the old
      chamfered, boxy look by setting spline resolution to 1 and manually
      positioning the corners of the wrist rest for it.
    - Moved `wrist-rest` → `shape` → `lip-height` to
      `wrist-rest` → `shape` → `lip` → `height`.
    - Moved `wrist-rest` → `shape` → `pad` → `surface-heightmap`
      to `wrist-rest` → `shape` → `pad` → `surface` → `heightmap` → `filepath`.
    - Substantial changes to `wrist-rest` → `fasteners`, which has been castled
      to `wrist-rest` → `mounts` and is now a list.
- Removed the implementation of the `solid` style of wrist rest attachment.
  This was reimplemented in version 0.5.0.
- Removed the option `wrist-rest` → `fasteners` → `mounts` → `plinth-side` →
  `pocket-scale`, obviated by a new generic `dfm` feature.
- Renamed the ‘finger’ key cluster to ‘main‘.
- As a side effect of outsourcing the design of threaded fasteners to
  `scad-tarmi`, the `flat` style of bolt head has been renamed to
  the more specific `countersunk`.
- Removed `create-models.sh`, adding equivalent functionality to the Clojure
  application itself (new flags: `--render`, `--renderer`).
- Added intermediate `scad` and `stl` folders under `things`.
- Split generated documentation (`options.md`) into four separate documents
  (`options-*.md`).

### Added
- This log.
- Support for generating a bottom plate that closes the case.
    - This includes support for a separate plate for the wrist rest, and a
      combined plate that joins the two models.
- Improvements to wrist rests.
    - Arbitrary polygonal outlines and vertically rounded edges, without a
      height map.
    - Tilting.
    - Support for placing wrist rests in relation to their point
      of attachment to the case using a new `anchoring` parameter.
    - Support for multiple mount points.
    - Support for naming the individual blocks that anchor a wrist rest.
    - Support for placing wrist rests in relation to a specific corner of a key.
      In the previous version, the attachment would be to the middle of the key.
    - Parametrization of mould wall thickness.
    - Parametrization of sprues.
- Support for naming your key clusters much more freely, and/or adding
  additional clusters. Even the new ‘main’ cluster is optional.
    - Support for a `cluster` parameter to `case` → `rear-housing` →
      `position`. The rear housing would previously be attached to the finger
      cluster.
    - Support for a `cluster` parameter to `case` → `leds` → `position`.
      LEDs would previously be attached to the finger cluster.
    - Support for anchoring any cluster to any other, within logical limits.
- Parametrization of keycap sizes, adding support for sizes other than 1u in
  both horizontal dimensions, as well as diversity in keycap height and
  clearance.
- Support for a filename whitelist in the CLI.
- Support for placing `foot-plates` in relation to objects other than keys.
- Support for generic compensation for slicer and printer inaccuracies in the
  xy plane through a new option, `dfm` → `error`.

### Fixed
- Improved support for Windows by using `clojure.java.io/file` instead of
  string literals with Unix-style file-path separators.
- Strengthened parameter validation for nested sections.

### Developer
- Significant restructuring of the code base for separation of concerns.
    - Switched to docstring-first function definitions.
    - Shifted more heavily toward explicit namespacing and took the opportunity
      to shorten some function names in the matrix module and elsewhere.
- Added a dependency on `scad-tarmi` for shorter OpenSCAD code and more
  capable models of threaded fasteners.
- Rearranged derived parameter structure somewhat to support arbitrary key
  clusters and the use of aliases for more types of objects (other than keys).
- Removed the `new-scad` function without replacement.
- Removed a dependency on `unicode-math`. The requisite version of the library
  had not been deployed to Clojars and its use was cosmetic.

[Unreleased]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.7.0...HEAD
[Version 0.7.0]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.6.0...dmote-v0.7.0
[Version 0.6.0]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.5.1...dmote-v0.6.0
[Version 0.5.1]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.5.0...dmote-v0.5.1
[Version 0.5.0]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.4.0...dmote-v0.5.0
[Version 0.4.0]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.3.0...dmote-v0.4.0
[Version 0.3.0]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.2.0...dmote-v0.3.0
