;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Parameter Specification – Ports                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.param.tree.port
  (:require [scad-tarmi.core :as tarmi-core]
            [dactyl-keyboard.cots :as cots]
            [dactyl-keyboard.param.schema.anch :as anch]
            [dactyl-keyboard.param.schema.parse :as parse]
            [dactyl-keyboard.param.schema.valid :as valid]
            [dactyl-keyboard.param.stock :as stock]
            [dactyl-keyboard.compass :as compass]))

(def raws
  "A flat version of a special part of a user configuration.
  Default values and parsers here are secondary. Validators are used."
  [["# Port configuration options\n\n"
    "Each heading in this document represents a recognized configuration key "
    "in [YAML files for the DMOTE application](configuration.md).\n\n"
    "This specific document describes options for the shape "
    "and position of any individual port. One set of such "
    "options will exist for each entry in `ports`, a section "
    "whose place in the larger hierarchy can be seen [here](options-main.md). "
    "Example uses for ports:\n"
    "\n"
    "* One port for the connection between the two halves of a reflected "
    "keyboard without a central housing. Such ports are usually TRRS or "
    "4P4C (“RJ9”), but you can use practically anything with enough wires.\n"
    "* An external USB port for interfacing with your computer, such as a "
    "full-size USB A port. You might want this when your MCU either has no "
    "such port attached or the attached port is too weak for direct human "
    "use (cf. `shelf`) or difficult to get into a good position.\n"
    "* Additional USB ports, connected via internal hub or to an "
    "integrated microphone clip, phone charger etc.\n"
    "* A speaker for QMK audio.\n"
    "* An LCD screen for QMK video.\n"
    "* An exotic human interface device, such as a large rotary encoder or "
    "trackball, not supported by this application as a type of keyboard "
    "switch.\n"
    "* Assortment drawers built into a large rear or central housing.\n"
    "\n"
    "Notice ports attached directly to microcontroller "
    "boards are treated in the `mcu` section, not here.\n"]
   [[:include]
    {:default false :parse-fn boolean :validate [::valid/include]}
    "If `true`, include the port. The main use of this option is for "
    "disabling ports defined in other configuration files. "
    "The default value is `false` for consistency with other inclusion "
    "parameters."]
   [[:body]
    {:default :auto :parse-fn keyword :validate [::valid/body]}
    "A code identifying the [body](configuration.md) in which the port is cut."]
   [[:type]
    {:default :custom-cuboid, :parse-fn keyword
     :validate [(set (conj (keys cots/port-facts)
                           :custom-cuboid :custom-cylindroid))]}
    "A code identifying a common type of port. "
    "The following values are recognized.\n\n"
    (cots/support-list cots/port-facts) "\n"
    "* `custom-cuboid`, a cuboid shape of any size.\n"
    "* `custom-cylindroid`, which has an ellipse as its cross-section in the "
    "xy plane.\n\n"
    "Only the `custom-*` types use the `size` parameter (below). "
    "For making anything other than a cuboid or cylindroid socket as a custom "
    "port, get as close as possible with `tweaks`, then make your own "
    "adapter and/or widen the socket with a soldering iron or similar "
    "tools to fit a more complex object."]
   [[:size]
    {:default 1 :parse-fn parse/pad-to-3-tuple
     :validate [::tarmi-core/point-3d]}
    "An `[x, y, z]` vector specifying the size of the port in mm. "
    "This is used only with `custom-*` port types.\n\n"
    "For a cuboid, the interpretation is straightforward. "
    "For a cylindroid, `x` and `y` are the two diameters of the elliptic "
    "cross-section, while `z` extrudes that cross-section."]
   [[:alignment]
    "How the port lines itself up at its position."]
   [[:alignment :segment]
    {:default 0, :validate [#{0 1 2}]}
    "Which vertical segment of the port itself to place at its anchor. "
    "The default value here is 0, meaning the ceiling of the port."]
   [[:alignment :side]
    {:default :N, :parse-fn keyword, :validate [compass/all]}
    "Which wall or corner of the port itself to place at its anchor. "
    "The default value here is the open face of the port."]
   [[:anchoring]
    anch/anchoring-metadata
    "Where to place the port. "
    stock/anchoring-documentation]
   [[:holder]
    "A map describing a positive addition to the case on five "
    "sides of the port: Every side but the front."]
   [[:holder :include]
    {:default false :parse-fn boolean :validate [::valid/include]}
    "If `true`, build a wall around the port."]
   [[:holder :alias]
    stock/alias-metadata
    "A name for the holder, to allow anchoring other features to it."]
   [[:holder :type]
    {:default :exterior-insert, :parse-fn keyword
     ;; Canonical types are named so as to reserve :interior-insert (where an
     ;; item is inserted from inside the body) and (fully) :enclosed).
     :validate [#{:exterior-insert :through-hole}]}
    "The type of a holder governs the placement of its walls. "
    "All holders with non-zero `thickness` (below`) have walls in the local "
    "xy plane. `through-hole` holders have *only* those walls. They’re "
    "intended for components that mount securely from both sides, such as "
    "rotary encoders with a threaded axis and a nut. "
    "`exterior-insert` holders add a wall at the bottom to secure an item that "
    "is inserted from outside the case.\n\n"
    "Regardless of `type`, you can use `tweaks` to put extra holes in a "
    "holder, for wiring."]
   [[:holder :thickness]
    {:default 0 :parse-fn num :validate [::valid/thickness]}
    "A number specifying the thickness of the holder’s walls on each side, "
    "in mm."]])
