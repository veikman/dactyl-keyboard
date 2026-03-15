<!--This document was generated and is intended for rendering to HTML on GitHub. Edit the source files, not this file.-->

# Port configuration options

Each heading in this document represents a recognized configuration key in [YAML files for the DMOTE application](configuration.md).

This specific document describes options for the shape and position of any individual port. One set of such options will exist for each entry in `ports`, a section whose place in the larger hierarchy can be seen [here](options-main.md). Example uses for ports:

* One port for the connection between the two halves of a reflected keyboard without a central housing. Such ports are usually TRRS or 4P4C (“RJ9”), but you can use practically anything with enough wires.
* An external USB port for interfacing with your computer, such as a full-size USB A port. You might want this when your MCU either has no such port attached or the attached port is too weak for direct human use (cf. `shelf`) or difficult to get into a good position.
* Additional USB ports, connected via internal hub or to an integrated microphone clip, phone charger etc.
* A speaker for QMK audio.
* An LCD screen for QMK video.
* An exotic human interface device, such as a large rotary encoder or trackball, not supported by this application as a type of keyboard switch.
* Assortment drawers built into a large rear or central housing.

Notice ports attached directly to microcontroller boards are treated in the `mcu` section, not here.


## Table of contents
- Parameter <a href="#user-content-include">`include`</a>
- Parameter <a href="#user-content-body">`body`</a>
- Parameter <a href="#user-content-type">`type`</a>
- Parameter <a href="#user-content-size">`size`</a>
- Section <a href="#user-content-alignment">`alignment`</a>
    - Parameter <a href="#user-content-alignment-segment">`segment`</a>
    - Parameter <a href="#user-content-alignment-side">`side`</a>
- Section <a href="#user-content-anchoring">`anchoring`</a>
- Section <a href="#user-content-holder">`holder`</a>
    - Parameter <a href="#user-content-holder-include">`include`</a>
    - Parameter <a href="#user-content-holder-alias">`alias`</a>
    - Parameter <a href="#user-content-holder-type">`type`</a>
    - Parameter <a href="#user-content-holder-thickness">`thickness`</a>

## Parameter <a id="include">`include`</a>

If `true`, include the port. The main use of this option is for disabling ports defined in other configuration files. The default value is `false` for consistency with other inclusion parameters.

## Parameter <a id="body">`body`</a>

A code identifying the [body](configuration.md) in which the port is cut.

## Parameter <a id="type">`type`</a>

A code identifying a common type of port. The following values are recognized.

* `modular-4p4c-616e`: modular connector 4P4C, socket 616E, minus the vertical stripe.
* `usb-c`: USB C.
* `usb-full-2b`: full-size USB 2 B.
* `usb-full-3b`: full-size USB 3 B.
* `usb-full-a`: full-size USB A.
* `usb-micro-2b`: USB micro 2 B.
* `usb-mini-b`: USB mini B.
* `custom-cuboid`, a cuboid shape of any size.
* `custom-cylindroid`, which has an ellipse as its cross-section in the xy plane.

Only the `custom-*` types use the `size` parameter (below). For making anything other than a cuboid or cylindroid socket as a custom port, get as close as possible with `tweaks`, then make your own adapter and/or widen the socket with a soldering iron or similar tools to fit a more complex object.

## Parameter <a id="size">`size`</a>

An `[x, y, z]` vector specifying the size of the port in mm. This is used only with `custom-*` port types.

For a cuboid, the interpretation is straightforward. For a cylindroid, `x` and `y` are the two diameters of the elliptic cross-section, while `z` extrudes that cross-section.

## Section <a id="alignment">`alignment`</a>

How the port lines itself up at its position.

### Parameter <a id="alignment-segment">`segment`</a>

Which vertical segment of the port itself to place at its anchor. The default value here is 0, meaning the ceiling of the port.

### Parameter <a id="alignment-side">`side`</a>

Which wall or corner of the port itself to place at its anchor. The default value here is the open face of the port.

## Section <a id="anchoring">`anchoring`</a>

Where to place the port. The concept of anchoring is explained [here](options-anchoring.md), along with the parameters available in this section.

## Section <a id="holder">`holder`</a>

A map describing a positive addition to the case on five sides of the port: Every side but the front.

### Parameter <a id="holder-include">`include`</a>

If `true`, build a wall around the port.

### Parameter <a id="holder-alias">`alias`</a>

A name for the holder, to allow anchoring other features to it.

### Parameter <a id="holder-type">`type`</a>

The type of a holder governs the placement of its walls. All holders with non-zero `thickness` (below`) have walls in the local xy plane. `through-hole` holders have *only* those walls. They’re intended for components that mount securely from both sides, such as rotary encoders with a threaded axis and a nut. `exterior-insert` holders add a wall at the bottom to secure an item that is inserted from outside the case.

Regardless of `type`, you can use `tweaks` to put extra holes in a holder, for wiring.

### Parameter <a id="holder-thickness">`thickness`</a>

A number specifying the thickness of the holder’s walls on each side, in mm.

⸻

This document was generated from the application CLI.
