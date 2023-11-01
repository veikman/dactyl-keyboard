;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Parameter Specification – Main                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.param.tree.main
  (:require [clojure.spec.alpha :as spec]
            [clojure.java.io :refer [file]]
            [scad-tarmi.core :as tarmi-core]
            [scad-app.schema :as appschema]
            [dmote-keycap.data :as capdata]
            [dactyl-keyboard.compass :as compass]
            [dactyl-keyboard.cots :as cots]
            [dactyl-keyboard.param.base :as base]
            [dactyl-keyboard.param.schema.anch :as anch]
            [dactyl-keyboard.param.schema.arb :as arb]
            [dactyl-keyboard.param.schema.parse :as parse]
            [dactyl-keyboard.param.schema.valid :as valid]
            [dactyl-keyboard.param.stock :as stock]
            [dactyl-keyboard.param.tree.central :as central]
            [dactyl-keyboard.param.tree.cluster :as cluster]
            [dactyl-keyboard.param.tree.flange :as flange]
            [dactyl-keyboard.param.tree.nested :as nested]
            [dactyl-keyboard.param.tree.port :as port]
            [dactyl-keyboard.param.tree.restmnt :as restmnt]))


;; Though this module describes the main body of parameters, it contains
;; within it certain sections specified elsewhere. Validators for these
;; sections are created from the detailed specifications by delegation.

(spec/def :nested/parameters (base/delegated-validation nested/raws))
(spec/def :nested/rows (spec/map-of ::valid/flexcoord ::nested-key-configuration))
(spec/def :nested/columns (spec/map-of ::valid/flexcoord ::nested-key-configuration))
(spec/def :nested/clusters (spec/map-of ::valid/key-cluster ::nested-key-configuration))
(spec/def :nested/sides (spec/map-of compass/all ::nested-key-configuration))
(spec/def ::nested-key-configuration
  (spec/keys :opt-un [:nested/parameters :nested/clusters :nested/columns
                      :nested/rows :nested/rows :nested/sides]))

(def raws
  "A flat version of the specification for a complete user configuration.
  This absorbs major subsections from elsewhere."
  [["# General configuration options\n\n"
    "Each heading in this document represents a recognized configuration key "
    "in [YAML files for the DMOTE application](configuration.md).\n\n"
    "Other documents cover special sections of this one in more detail."]
   [[:keys]
    "Keys, that is keycaps and electrical switches, are not the main focus of "
    "this application, but they influence the shape of the case."]
   [[:keys :preview]
    {:default false :parse-fn boolean}
    "If `true`, include models of the keycaps in place on the keyboard. This "
    "is intended for illustration as you work on a design, not for printing."]
   [[:keys :styles]
    {:freely-keyed true :default {:default {}} :parse-fn parse/keycap-map
     :validate [(spec/map-of keyword? ::valid/comprehensive-key-style)]}
    "Here you name all the styles of keys on the keyboard and describe each "
    "style using parameters to the `keycap` function of the "
    "[`dmote-keycap`](https://github.com/veikman/dmote-keycap) library.\n"
    "\n"
    "Key styles determine the size of key mounting plates on the "
    "keyboard and what kind of holes are cut into those plates for the "
    "switches to fit inside. "
    "Negative space is also reserved above the plate for the movement "
    "of the keycap: A function of switch height, switch travel, and keycap "
    "shape.\n"
    "\n"
    "`switch-type`, where you name a type of electromechanical switch, "
    "is one aspect of key style. The DMOTE application supports a superset "
    "of `dmote-keycap`’s switch types, because the added types differ only "
    "in the shape of the hole that goes through the mounting plate: "
    "Differences which are irrelevant to keycaps. "
    "The following options are thus recognized for `switch-type` in a "
    "key style:\n\n"
    (cots/support-list cots/switch-facts)
    "\n\n"
    "In options by key, documented [here](options-nested.md), you specify "
    "which style of key is used for each position on the keyboard."]
   [[:key-clusters]
    {:freely-keyed true
     :default {:main {:matrix-columns [{:rows-below-home 0}]
                      :aliases {}}}
     :parse-fn (parse/map-of keyword
                 (base/parser-with-defaults cluster/raws))
     :validate [(spec/map-of
                  ::valid/key-cluster
                  (base/delegated-validation cluster/raws))]}
    "This section describes the general size, shape and position of "
    "the clusters of keys on the keyboard, each in its own subsection. "
    "It is documented in detail [here](options-clusters.md)."]
   [[:by-key]
    {:heading-template "Special section %s"
     :default {:parameters (base/extract-defaults nested/raws)}
     :parse-fn (parse/nested-key-fn (base/parser-wo-defaults nested/raws) 5)
     :resolve-fn base/soft-defaults
     :validate [::nested-key-configuration]}
    "This section contains a nested structure of parameters. "
    "Each level within controls a smaller part of the keyboard, "
    "eventually reaching the level of specific sides of individual keys. "
    "It’s all documented [here](options-nested.md)."]
   [[:secondaries]
    {:default {}
     :parse-fn (parse/map-of keyword
                             (parse/map-like
                               {:anchoring anch/parse-anchoring
                                :override vec
                                :size parse/pad-to-3-tuple}))
     :validate [(spec/map-of ::valid/alias
                             (spec/keys :opt-un [::anch/anchoring
                                                 :three/override
                                                 :tweak/size]))]}
    "A map where each item provides a name for a position in space. "
    "Such positions exist in relation to other named features of the keyboard "
    "and their names can in turn be used as anchors, most typically as "
    "supplementary targets for `tweaks` (see below). "
    stock/anchoring-documentation
    "\n\n"
    "An example:\n\n"
    "```secondaries:\n"
    "  s0:\n"
    "    anchoring:\n"
    "      anchor: f0\n"
    "      side: SE\n"
    "    override [null, null, 2]\n"
    "    size: 4\n```\n"
    "\n"
    "This example gives the name `s0` to a point near some feature named "
    "`f0`. Populated coordinates in `override` replace corresponding "
    "coordinates given by the anchor, so in the example, `s0` is 2 mm above "
    "the floor and any distance down from the southeast corner "
    "of `f0` in a straight vertical line. In other words, the position of "
    "that corner of `f0` is projected onto the global xy plane at z = 2 "
    "to make `s0`.\n"
    "\n"
    "The `size` parameter is relevant only as a fallback for `tweaks` that "
    "target the position and do not specify a size in turn. If both are "
    "omitted, tweaks fall back still further, to a tiny point.\n"
    "\n"
    "All of these properties are optional."]
   [[:main-body]
    "The main body of the keyboard is the main output of this application. "
    "It may be the only body. "
    "Much of this part of the case is generated from the `wall` parameters "
    "described [here](options-nested.md). "
    "This section deals with lesser features of the main body."]
   [[:main-body :reflect]
    {:default false :parse-fn boolean}
    "If `true`, mirror the main body, producing one version for the right hand "
    "and another for the left. The two halves will be almost identical: "
    "Only chiral parts, such as threaded holes, are exempt from mirroring "
    "with `main-body` → `reflect`.\n\n"
    "You can use this option to make a ‘split’ keyboard, though the two "
    "halves are typically connected by a signalling cable, by a rigid "
    "`central-housing`, or by one or more rods anchored to "
    "some feature such as `rear-housing` or `back-plate`."]
   [[:main-body :rear-housing]
    "The furthest row of a key cluster can be extended into a rear housing "
    "for the MCU and various other features."]
   [[:main-body :rear-housing :include]
    {:default false :parse-fn boolean}
    "If `true`, add a rear housing to the main body."]
   [[:main-body :rear-housing :anchoring]
    anch/anchoring-metadata
    "Where to place the middle of the rear housing. "
    stock/anchoring-documentation]
   [[:main-body :rear-housing :size]
    {:default 1 :parse-fn parse/pad-to-3-tuple
     :validate [::tarmi-core/point-3d]}
    "The exterior measurements of the rear housing, in mm."]
   [[:main-body :rear-housing :bevel]
    "The rear housing can be bevelled."]
   [[:main-body :rear-housing :bevel :exterior]
    {:default 0 :parse-fn num}
    "Insets from the specified `size` in mm for the exterior of the housing."]
   [[:main-body :rear-housing :bevel :interior]
    {:default 0 :parse-fn num}
    "Insets from the specified `size`, minus thickness, in mm, for the "
    "interior of the housing.\n\n"
    "This is separate from the exterior bevel parameter because it can help "
    "with DFM. A higher setting here can reduce the need for internal print "
    "supports."]
   [[:main-body :rear-housing :thickness]
    "The thickness of the rear housing does not influence its external "
    "dimensions. It grows inward."]
   [[:main-body :rear-housing :thickness :walls]
    {:default 1 :parse-fn num}
    "The horizontal thickness in mm of the walls."]
   [[:main-body :rear-housing :thickness :roof]
    {:default 1 :parse-fn num}
    "The vertical thickness in mm of the flat top, inside the insets for "
    "bevels."]
   [[:main-body :rear-housing :fasteners]
    "Threaded bolts can run through the roof of the rear housing, making it a "
    "hardpoint for attachments like a stabilizer to connect the two halves of "
    "a split keyboard."]
   [[:main-body :rear-housing :fasteners :bolt-properties]
    stock/implicit-threaded-bolt-metadata
    stock/threaded-bolt-documentation]
   [[:main-body :rear-housing :fasteners :bosses]
    {:default false :parse-fn boolean}
    "If `true`, add nut bosses to the ceiling of the rear housing for each "
    "fastener. Space permitting, these bosses will have some play on the "
    "north-south axis, to permit adjustment of the angle of the keyboard "
    "halves under a stabilizer."]
   [[:main-body :rear-housing :fasteners :sides]
    "Analogous but independent parameters for the west and east sides."]
   [[:main-body :rear-housing :fasteners :sides :W]
    "The west: A fastener on the inward-facing end of the rear housing."]
   [[:main-body :rear-housing :fasteners :sides :W :include]
    {:default false :parse-fn boolean}
    "If `true`, include this fastener."]
   [[:main-body :rear-housing :fasteners :sides :W :offset]
    {:default 0 :parse-fn num}
    "A one-dimensional offset in mm from the inward edge of the rear "
    "housing to the fastener. You probably want a negative number if any."]
   [[:main-body :rear-housing :fasteners :sides :E]
    "The east: A fastener on the outward-facing end of the rear housing."]
   [[:main-body :rear-housing :fasteners :sides :E :include]
    {:default false :parse-fn boolean}]
   [[:main-body :rear-housing :fasteners :sides :E :offset]
    {:default 0 :parse-fn num}]
   [[:main-body :back-plate]
    "Given that independent movement of each half of a split keyboard is not "
    "useful, each half can include a mounting plate for a stabilizing rod. "
    "That is a straight piece of wood, aluminium, rigid plastic etc. to "
    "connect the two halves mechanically and possibly carry the wire that "
    "connects them electrically.\n\n"
    "This option is similar to rear housing, but the back plate block "
    "provides no interior space for an MCU etc. It is solid, with holes "
    "for threaded fasteners including the option of nut bosses. "
    "Its footprint is not part of a `bottom-plate`."]
   [[:main-body :back-plate :include]
    {:default false :parse-fn boolean}
    "If `true`, include a back plate block. This is not contingent upon "
    "`reflect`."]
   [[:main-body :back-plate :beam-height]
    {:default 1 :parse-fn num}
    "The nominal vertical extent of the back plate in mm. "
    "Because the plate is bottom-hulled to the floor, the effect "
    "of this setting is on the area of the plate above its holes."]
   [[:main-body :back-plate :fasteners]
    "Two threaded bolts run through the back plate."]
   [[:main-body :back-plate :fasteners :bolt-properties]
    stock/implicit-threaded-bolt-metadata
    stock/threaded-bolt-documentation]
   [[:main-body :back-plate :fasteners :distance]
    {:default 1 :parse-fn num}
    "The horizontal distance between the bolts."]
   [[:main-body :back-plate :fasteners :bosses]
    {:default false :parse-fn boolean}
    "If `true`, cut nut bosses into the inside wall of the block."]
   [[:main-body :back-plate :anchoring]
    anch/anchoring-metadata
    "Where to place the middle of the back plate. "
    stock/anchoring-documentation]
   [[:main-body :bottom-plate]
    "Plating underneath the main body. See the top-level `bottom-plates` "
    "section for shared properties of bottom plates."]
   [[:main-body :bottom-plate :include]
    {:default false :parse-fn boolean}
    "If `true`, include a bottom plate for this body."]
   [[:main-body :leds]
    "Support for light-emitting diodes in the case walls.\n\n"
    "This feature of the application is poorly developed and may be removed "
    "in a future version. Consider using `ports` for greater freedom in "
    "the placement of LEDs."]
   [[:main-body :leds :include]
    {:default false :parse-fn boolean}
    "If `true`, cut slots for LEDs out of the case wall, facing "
    "the space between the two halves."]
   [[:main-body :leds :position]
    "Where to attach the LED strip."]
   [[:main-body :leds :position :cluster]
    {:default :main :parse-fn keyword :validate [::valid/key-cluster]}
    "The key cluster at which to anchor the strip."]
   [[:main-body :leds :amount]
    {:default 1 :parse-fn int} "The number of LEDs."]
   [[:main-body :leds :housing-size]
    {:default 1 :parse-fn num}
    "The length of the side on a square profile used to create negative space "
    "for the housings on a LED strip. This assumes the housings are squarish, "
    "as on a WS2818.\n"
    "\n"
    "The negative space is not supposed to penetrate the wall, just make it "
    "easier to hold the LED strip in place with tape, and direct its light. "
    "With that in mind, feel free to exaggerate by 10%."]
   [[:main-body :leds :emitter-diameter]
    {:default 1 :parse-fn num}
    "The diameter of a round hole for the light of an LED."]
   [[:main-body :leds :interval]
    {:default 1 :parse-fn num}
    "The distance between LEDs on the strip. You may want to apply a setting "
    "slightly shorter than the real distance, since the algorithm carving the "
    "holes does not account for wall curvature."]
   [[:central-housing]
    {:heading-template "Section %s"
     :default (base/extract-defaults central/raws)
     :parse-fn (base/parser-with-defaults central/raws)
     :validate [(base/delegated-validation central/raws)]}
    "An optional body separate from the main body, located in between and "
    "connecting the two halves of a reflected main body. "
    "The central housing is documented in detail [here](options-central.md)."]
   [[:wrist-rest]
    "An optional extra body to support the user’s wrist."]
   [[:wrist-rest :include]
    {:default false :parse-fn boolean}
    "If `true`, include a wrist rest with the keyboard."]
   [[:wrist-rest :style]
    {:default :threaded :parse-fn keyword :validate [::valid/wrist-rest-style]}
    "The style of the wrist rest. Available styles are:\n\n"
    "- `threaded`: threaded fasteners connect the case and wrist rest.\n"
    "- `solid`: the case and wrist rest are joined together by `tweaks` "
    "as a single piece of plastic."]
   [[:wrist-rest :preview]
    {:default false :parse-fn boolean}
    "Preview mode. If `true`, this puts a model of the wrist rest in the same "
    "OpenSCAD file as the case. That model is simplified, intended for gauging "
    "distance, not for printing."]
   [[:wrist-rest :anchoring]
    anch/anchoring-metadata
    "Where to place the wrist rest. "
    stock/anchoring-documentation " "
    "For wrist rests, the vertical component of the anchor’s position is "
    "ignored, including any vertical offset."]
   [[:wrist-rest :plinth-height]
    {:default 1 :parse-fn num}
    "The average height of the plastic plinth in mm, at its upper lip."]
   [[:wrist-rest :shape]
    "The wrist rest needs to fit the user’s hand."]
   [[:wrist-rest :shape :spline]
    "The horizontal outline of the wrist rest is a closed spline."]
   [[:wrist-rest :shape :spline :main-points]
    {:default [{:position [0 0]} {:position [1 0]} {:position [1 1]}]
     :parse-fn parse/nameable-spline
     :validate [::valid/nameable-spline]}
    "A list of nameable points, in clockwise order. The spline will pass "
    "through all of these and then return to the first one. Each point can "
    "have two properties:\n\n"
    "- Mandatory: `position`. A pair of coordinates, in mm, relative to other "
    "points in the list.\n"
    "- Optional: `alias`. A name given to the specific point, for the purpose "
    "of placing yet more things in relation to it."]
   [[:wrist-rest :shape :spline :resolution]
    {:default 1 :parse-fn num}
    "The amount of vertices per main point. The default is 1. If 1, only the "
    "main points themselves will be used, giving you full control. A higher "
    "number gives you smoother curves.\n\n"
    "If you want the closing part of the curve to look smooth in high "
    "resolution, position your main points carefully.\n\n"
    "Resolution parameters, including this one, can be disabled in the main "
    "`resolution` section."]
   [[:wrist-rest :shape :lip]
    "The lip is the uppermost part of the plinth, lining and supporting the "
    "edge of the pad. Its dimensions are described here in mm away from the "
    "pad."]
   [[:wrist-rest :shape :lip :height]
    {:default 1 :parse-fn num} "The vertical extent of the lip."]
   [[:wrist-rest :shape :lip :width]
    {:default 1 :parse-fn num} "The horizontal width of the lip at its top."]
   [[:wrist-rest :shape :lip :inset]
    {:default 0 :parse-fn num}
    "The difference in width between the top and bottom of the lip. "
    "A small negative value will make the lip thicker at the bottom. This is "
    "recommended for fitting a silicone mould."]
   [[:wrist-rest :shape :pad]
    "The top of the wrist rest should be printed or cast in a soft material, "
    "such as silicone rubber."]
   [[:wrist-rest :shape :pad :surface]
    "The upper surface of the pad, which will be in direct contact with "
    "the user’s palm or wrist."]
   [[:wrist-rest :shape :pad :height]
    "The piece of rubber extends a certain distance up into the air and down "
    "into the plinth. All measurements in mm."]
   [[:wrist-rest :shape :pad :height :surface-range]
    {:default 1 :parse-fn num}
    "The vertical range of the upper surface. Whatever values are in "
    "a heightmap will be normalized to this scale."]
   [[:wrist-rest :shape :pad :height :lip-to-surface]
    {:default 1 :parse-fn num}
    "The part of the rubber pad between the top of the lip and the point "
    "where the heightmap comes into effect. This is useful if your heightmap "
    "itself has very low values at the edges, such that moulding and casting "
    "it without a base would be difficult."]
   [[:wrist-rest :shape :pad :height :below-lip]
    {:default 1 :parse-fn num}
    "The depth of the rubber wrist support, measured from the top of the lip, "
    "going down into the plinth. This part of the pad just keeps it in place."]
   [[:wrist-rest :shape :pad :surface :edge]
    "The edge of the pad can be rounded."]
   [[:wrist-rest :shape :pad :surface :edge :inset]
    {:default 0 :parse-fn num}
    "The horizontal extent of softening. This cannot be more than half the "
    "width of the outline, as determined by `main-points`, at its narrowest "
    "part."]
   [[:wrist-rest :shape :pad :surface :edge :resolution]
    {:default 1 :parse-fn num}
    "The number of faces on the edge between horizontal points.\n\n"
    "Resolution parameters, including this one, can be disabled in the main "
    "`resolution` section."]
   [[:wrist-rest :shape :pad :surface :heightmap]
    "The surface can optionally be modified by the [`surface()` function]"
    "(https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/"
    "Other_Language_Features#Surface), which requires a heightmap file."]
   [[:wrist-rest :shape :pad :surface :heightmap :include]
    {:default false :parse-fn boolean}
    "If `true`, use a heightmap. The map will intersect the basic pad "
    "polyhedron."]
   [[:wrist-rest :shape :pad :surface :heightmap :filepath]
    {:default (file ".." ".." "resources" "heightmap" "default.dat")}
    "The file identified here should contain a heightmap in a format OpenSCAD "
    "can understand. The path should also be resolvable by OpenSCAD."]
   [[:wrist-rest :rotation]
    "The wrist rest can be rotated to align its pad with the user’s palm."]
   [[:wrist-rest :rotation :pitch]
    {:default 0 :parse-fn parse/compass-incompatible-angle}
    "Tait-Bryan pitch."]
   [[:wrist-rest :rotation :roll]
    {:default 0 :parse-fn parse/compass-incompatible-angle}
    "Tait-Bryan roll."]
   [[:wrist-rest :mounts]
    {:heading-template "Special section %s"
     :default []
     :parse-fn (parse/tuple-of (base/parser-with-defaults restmnt/raws))
     :validate [(spec/coll-of (base/delegated-validation restmnt/raws))]}
    "A list of mounts for threaded fasteners. Each such mount will include at "
    "least one cuboid block for at least one screw that connects the wrist "
    "rest to the case. "
    "This section is used only with the `threaded` style of wrist rest."]
   [[:wrist-rest :sprues]
    "Holes in the bottom of the plinth. You pour liquid rubber through these "
    "holes when you make the rubber pad. Sprues are optional, but the general "
    "recommendation is to have two of them if you’re going to be casting your "
    "own pads. That way, air can escape even if you accidentally block one "
    "sprue with a low-viscosity silicone."]
   [[:wrist-rest :sprues :include]
    {:default false :parse-fn boolean}
    "If `true`, include sprues."]
   [[:wrist-rest :sprues :inset]
    {:default 0 :parse-fn num}
    "The horizontal distance between the perimeter of the wrist rest and the "
    "default position of each sprue."]
   [[:wrist-rest :sprues :diameter]
    {:default 1 :parse-fn num}
    "The diameter of each sprue."]
   [[:wrist-rest :sprues :positions]
    {:default [] :parse-fn (parse/tuple-of anch/parse-anchoring)
     :validate [(spec/coll-of anch/validate-anchoring)]}
    "The positions of all sprues. This is a list where each item needs an "
    "`anchor` naming a main point in the spline. You can add an optional "
    "two-dimensional `offset`."]
   [[:wrist-rest :bottom-plate]
    "Plating underneath the wrist rest. See the top-level `bottom-plates` "
    "section for shared properties of bottom plates.\n"
    "\n"
    "Bottom plates for wrist rests have no ESDS electronics to "
    "protect but serve other purposes: Covering nut pockets, silicone "
    "mould-pour cavities, and plaster or other dense material poured into "
    "plinths printed without a bottom shell."]
   [[:wrist-rest :bottom-plate :include]
    {:default false :parse-fn boolean}
    "If `true`, include a bottom plate for this body."]
   [[:wrist-rest :mould-thickness]
    {:default 1 :parse-fn num}
    "The thickness in mm of the walls and floor of the mould to be used for "
    "casting the rubber pad."]
   [[:bottom-plates]
    "A bottom plate can be added to each body of the keyboard, to close the "
    "case. This is useful mainly to simplify transportation.\n"
    "\n"
    "Bottom plates are largely two-dimensional. The application builds most "
    "of each plate from a set of polygons, trying to match the perimeter of "
    "the case at the ground level (i.e. z = 0).\n"
    "\n"
    "Specifically, there is one polygon per key cluster (limited to wall "
    "edges drawn to the floor), one polygon for each housing, and one "
    "set of polygons for each of the first-level case `tweaks` that use "
    "`at-ground` (or `polyfill`, details [here](options-arbitrary-shapes.md)), "
    "ignoring chunk size and almost ignoring tweaks nested "
    "within lists of tweaks.\n"
    "\n"
    "This methodology is mentioned here because its results are not perfect. "
    "Pending future features in OpenSCAD, a future version may be based on a "
    "more exact projection of the case, but as of 2020, such a projection is "
    "hollow and cannot be convex-hulled without escaping the case, unless "
    "your case is convex to start with.\n"
    "\n"
    "For this reason, while the polygons fill the interior, the perimeter of "
    "the bottom plate is extended by key walls and case `tweaks` as they "
    "would appear at the height of the bottom plate. Even this brutality is "
    "inadequate in a corner case when you have tweaks `to-ground` from below "
    "that should affect the bottom plate. If you require a more exact match, "
    "do a projection of the case without a bottom plate, save it as DXF/SVG "
    "etc. and post-process that file to fill the interior gap."]
   [[:bottom-plates :preview]
    {:default false :parse-fn boolean}
    "Preview mode. If `true`, put a model of the plate in the same file as "
    "the body it closes. Not for printing."]
   [[:bottom-plates :combine]
    {:default false :parse-fn boolean}
    "If `true`, combine bottom plates for as many bodies as possible.\n\n"
    "This can be used with the `solid` style of wrist rest to get a plate "
    "that helps lock the wrist rest to the main body, and with a central "
    "housing to get a single, bilateral plate that extends from side to side. "
    "This larger plate can require a large build volume."]
   [[:bottom-plates :thickness]
    {:default 1 :parse-fn num}
    "The thickness (i.e. height) in mm of all bottom plates you choose to "
    "include. This covers plates for the case and for the wrist rest.\n"
    "\n"
    "The case will not be raised to compensate for this. Instead, the "
    "thickness of the bottom plate will be removed from the bottom of the "
    "main model."]
   [[:custom-bodies]
    (let [custom-body
          [[]  ;; This header will not be rendered and is therefore empty.
           [[:include] {:default false :parse-fn boolean}]
           [[:parent-body] {:default :main :parse-fn keyword}]
           [[:cut] arb/parameter-metadata]]]
      {:freely-keyed true
       :default {}
       :parse-fn (parse/map-of keyword (base/parser-with-defaults custom-body))
       :validate [(spec/map-of ::valid/custom-body
                               (base/delegated-validation custom-body))]})
    "Bodies in addition to those predefined by the application.\n\n"
    "This feature is intended for dividing up a keyboard case into parts for "
    "easier printing and/or easier assembly. "
    "It is not intended for adding novel shapes, such as washable cup holders "
    "to fit into `ports` etc. "
    "Custom bodies can be combined with `tweaks` to add and separate shapes, "
    "but complex novel shapes should typically be designed separately from "
    "this application (see `dmote-beam` for an example), or else added as "
    "features of the application.\n"
    "\n"
    "The structure of the `custom-bodies` section is a map of new body names "
    "to maps of the following parameters:\n"
    "\n"
    "- `include`: If `true`, and the parent body is also set to be included, "
    "make the custom body an output of the application, with its own SCAD "
    "file.\n"
    "- `parent-body`: The name of another body, into which the custom body "
    "fits. By default, the main body (`main`). `auto` cannot be used, but "
    "another custom body can be, so long as there is no loop of custom bodies "
    "being one another’s parents.\n"
    "- `cut`: Where the custom body fits into the parent body.\n"
    "\n"
    "The name of a custom body cannot match that of a predefined body "
    "such as `main`.\n"
    "\n"
    "`cut` takes a map of [arbitrary shapes](options-arbitrary-shapes.md) "
    "and ultimately describes the shape of the custom body. "
    "However, the combined shape of the cut is not the shape of the custom "
    "body itself. Those parts of the parent body that would normally have "
    "fallen within the area instead disappear from the parent body *and* "
    "constitute the entire custom body instead. What’s already inside the area "
    "effectively moves from one body to the other. This behaviour can be "
    "broken by `preview` settings, since they artificially extend bodies for "
    "purposes of visualization.\n"
    "\n"
    "Here’s an example of a custom body named `pet-flap`, with a simple cut, "
    "arbitrarily named `shape`:\n"
    "\n"
    "```custom-bodies:\n"
    "  pet-flap:\n"
    "    include: true\n"
    "    parent-body: central-housing\n"
    "    cut:\n"
    "      shape:\n"
    "      - [origin, {size: 60}]\n```\n"
    "\n"
    "This example describes a cube-shaped area in the middle of the central "
    "housing. The wall there will gets its own SCAD file, so you can print it "
    "separately and then mount it on hinges for small animals to live in your "
    "keyboard.\n"
    "\n"
    "If its parent body is governed by `reflect`, the custom body will also "
    "be reflected, appearing in left- and right-hand versions as separate "
    "outputs of the application."]
   [[:flanges]
    {:freely-keyed true
     :default {}
     :parse-fn (parse/map-of keyword (base/parser-with-defaults flange/raws))
     :validate [(spec/map-of ::valid/original (base/delegated-validation flange/raws))]}
    "This section describes shapes connecting different bodies, with special "
    "features for connecting bottom plates to their parent bodies. "
    "Each flange gets its own subsection. "
    "Flanges are documented in detail [here](options-flanges.md)."]
   [[:tweaks]
    arb/parameter-metadata
    "Additional shapes. This parameter is usually needed to bridge gaps "
    "between the walls of key clusters. The expected value here is a map of "
    "[arbitrary shapes](options-arbitrary-shapes.md).\n"
    "\n"
    "In the following example, `A` and `B` are key aliases that would be "
    "defined elsewhere.\n"
    "\n"
    "```tweaks:\n"
    "  bridge-between-A-and-B:\n"
    "    - chunk-size: 2\n"
    "      hull-around:\n"
    "      - [A, SE]\n"
    "      - [B, NE]\n"
    "      - [A, SSW, 0, 3]\n```\n"
    "\n"
    "The example is interpreted to mean that a plate should be "
    "created stretching from the southeast corner of `A` to the "
    "northeast corner of `B`. Due to `chunk-size` 2, that first "
    "plate will be joined to, but not fully hulled with, a second plate "
    "from `B` back to a different corner of `A`, with a longer stretch "
    "of wall segments (0 through 3 inclusive) running down the "
    "south-by-southwest corner of `A`.\n"
    "\n"
    "In `tweaks` nodes, the `body` setting is meaningful, but should not "
    "refer to a custom body, because the shape of a custom body is always "
    "fully determined by its parent body and its cut. Tweaks are not "
    "applied to custom bodies as such."]
   [[:mcu]
    "MCU is short for ”micro-controller unit”. You need at least one of "
    "these, it’s assumed to be mounted on a PCB, and you typically want some "
    "support for it inside the case.\n\n"
    "The total number of MCUs is governed by more than one setting, roughly "
    "in the following order:\n\n"
    "* If `mcu` → `include` is `false`, there is no MCU.\n"
    "* If `mcu` → `include` is `true` but `main-body` → `reflect` is `false`, "
    "there is one MCU.\n"
    "* If `mcu` → `include` and `main-body` → `reflect` and `mcu` → `position` "
    " → `central` are all `true`, there is (again) one MCU.\n"
    "* Otherwise, there are two MCUs: One in each half of the case, because "
    "of reflection."]
   [[:mcu :include]
    {:default false :parse-fn boolean}
    "If `true`, make space for at least one MCU PCBA."]
   [[:mcu :preview]
    {:default false :parse-fn boolean}
    "If `true`, render a visualization of the MCU PCBA. "
    "For use in development."]
   [[:mcu :body]
    {:default :auto :parse-fn keyword :validate [::valid/body]}
    "A code identifying the [body](configuration.md) that houses the MCU."]
   [[:mcu :type]
    {:default :rpi-pico :parse-fn keyword
     :validate [(partial contains? cots/mcu-facts)]}
    "A code name for a form factor. "
    "The following values are supported, representing a selection of "
    "designs for commercial products from PJRC, SparkFun, the QMK team "
    "and others:\n\n"
    (cots/support-list cots/mcu-facts)]
   [[:mcu :anchoring]
    anch/anchoring-metadata
    "Where to place the middle of the back plate. "
    stock/anchoring-documentation " "
    "The PCBA has its base point in the front and rotates around that point. "
    "By default, the PCBA appears lying flat, with the MCU side up and the "
    "connector end facing nominal north, away from the user."]
   [[:mcu :support]
    "This section offers a couple of different, mutually compatible ways to "
    "hold an MCU PCBA in place. Without such support, the MCU will be "
    "rattling around inside the case.\n\n"
    "Support is especially important if connector(s) on the PCBA will be "
    "exposed to animals, such as people. Take care that the user can plug in "
    "a USB cable, which requires a receptable to be both reachable "
    "through the case *and* held there firmly enough that the force of the "
    "user’s interaction will neither damage nor displace the board.\n\n"
    "Despite the importance of support in most use cases, no MCU support is "
    "included by default. Instead of using the features offered in this "
    "section, consider using `tweaks` anchored to the PCBA instead."]
   [[:mcu :support :preview]
    {:default false :parse-fn boolean}
    "If `true`, render a visualization of the support in place. This applies "
    "only to those parts of the support that are not part of the case model."]
   [[:mcu :support :shelf]
    "The case can include a shelf for the MCU.\n\n"
    "A shelf is the simplest type of MCU support, found on the original "
    "Dactyl-ManuForm. It provides very little mechanical support to hold the "
    "MCU itself in place, so it is not suitable for exposing a connector on "
    "the MCU PCBA through the case. Instead, it’s suitable for use together "
    "with a pigtail cable between the MCU and a secondary USB connector "
    "embedded in the case wall (see `ports`). "
    "It’s especially good with stiff single-strand wiring that will help keep "
    "the MCU in place without a lock or firm grip."]
   [[:mcu :support :shelf :include]
    {:default false :parse-fn boolean}
    "If `true`, include a shelf."]
   [[:mcu :support :shelf :extra-space]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "Modifiers for the size of the PCB, on all three axes, in mm, for the "
    "purpose of determining the size of the shelf.\n\n"
    "For example, the last term, for z, adds extra space between the "
    "component side of the PCBA up to the overhang on each side of the shelf, "
    "if any. The MCU will appear centered inside the available space, so this "
    "parameter can move the plane of the shelf itself."]
   [[:mcu :support :shelf :thickness]
    {:default 1 :parse-fn num :validate [pos?]}
    "The thickness of material in the shelf, below or behind the PCBA, in mm."]
   [[:mcu :support :shelf :bevel]
    {:default {} :parse-fn parse/compass-angle-map
     :validate [::valid/compass-angle-map]}
    "A map of cardinal compass points to angles in radians, whereby "
    "any and all sides of the shelf are turned away from the MCU PCBA. "
    "This feature is intended mainly for manufacturability, to reduce the "
    "need for supports in printing, but it can also add strength or help "
    "connect to other features."]
   [[:mcu :support :shelf :rim]
    "By default, a shelf includes raised edges to hold on to the "
    "PCBA. This is most useful when the shelf is rotated, following the "
    "MCU, out of the xy-plane."]
   [[:mcu :support :shelf :rim :lateral-thickness]
    {:default 1 :parse-fn num :validate [pos?]}
    "The thickness of material to each side of the MCU, in mm."]
   [[:mcu :support :shelf :rim :overhang-thickness]
    {:default 1 :parse-fn num :validate [pos?]}
    "The thickness of material in the outermost part on each side, in mm."]
   [[:mcu :support :shelf :rim :overhang-width]
    {:default 0 :parse-fn num :validate [#(not (neg? %))]}
    "The extent to which each side extends out across the PCBA, in mm."]
   [[:mcu :support :shelf :rim :offsets]
    {:default [0 0]
     :parse-fn (fn [candidate]
                 (if (number? candidate) [candidate candidate] (vec candidate)))
     :validate [::tarmi-core/point-2d]}
    "One or two lengthwise offsets in mm. When these are left at zero, the "
    "sides of the shelf will appear in full. A negative or positive offset "
    "shortens the corresponding side, towards or away from the connector side "
    "of the PCBA."]
   [[:mcu :support :lock]
    "An MCU lock is a support feature made up of three parts:\n\n"
    "* A fixture printed as part of the case. This fixture includes a plate for "
    "the PCB and a socket. The socket holds a USB connector on the PCB in "
    "place.\n"
    "* The bolt of the lock, printed separately.\n"
    "* A threaded fastener, not printed.\n"
    "The fastener connects the bolt to the fixture as the lock closes over "
    "the PCB. Confusingly, the fastener constitutes a bolt, in a different "
    "sense of that word.\n\n"
    "A lock is most appropriate when the PCB aligns with a long, flat wall; "
    "typically the wall of a rear housing. It has the advantage that it can "
    "hug the connector on the PCB tightly from four sides, thus preventing "
    "a fragile surface-mounted connector from snapping off."]
   [[:mcu :support :lock :include]
    {:default false :parse-fn boolean}
    "If `true`, include a lock."]
   [[:mcu :support :lock :width-factor]
    {:default 1 :parse-fn num}
    "A multiplier for the width of the PCB. This determines the width of the "
    "parts touching the PCB in a lock: The plate and the base of the bolt."]
   [[:mcu :support :lock :fastener-properties]
    ;; This parameter is named “fastener-properties” instead of the normal
    ;; “bolt-properties” to help distinguish it both from the lock bolt and
    ;; from parameters that add an implicit default length. The nomenclature
    ;; is not ideal.
    stock/explicit-threaded-bolt-metadata
    "Like the various `bolt-properties` parameters elsewhere, this parameter "
    "describes a threaded fastener using the `bolt` function in the "
    "[`scad-klupe.iso`](https://github.com/veikman/scad-klupe) library.\n\n"
    "This particular set of fastener propertes should not include a "
    "`total-length` because the application will interpolate default values "
    "for both `unthreaded-length` and `threaded-length` based on other "
    "properties of the lock. A contradictory `total-length` is an error."]
   [[:mcu :support :lock :plate]
    "In the lock, the MCU PCBA sits on a plate, as part of the fixture. "
    "This plate is named by analogy with a roughly corresponding part in a "
    "door lock. The plate actually looks like a bed for the PCB.\n\n"
    "The plate is typically more narrow than the PCB, its width being "
    "determined by `width-factor`. Its total height is the sum of this "
    "section’s `base-thickness` and `clearance`."]
   [[:mcu :support :lock :plate :alias]
    stock/alias-metadata
    "A name you can use to target the base of the plate for `tweaks`. "
    "This is useful mainly when there isn’t a flat wall behind the lock."]
   [[:mcu :support :lock :plate :base-thickness]
    {:default 1 :parse-fn num}
    "The thickness of the base of the plate, in mm."]
   [[:mcu :support :lock :plate :backing-thickness]
    {:default 0 :parse-fn num}
    "The thickness of whatever is behind the plate, in mm. "
    "The only influence of this parameter is on the length and position of "
    "the fastener, which is extended this far away from the plate to "
    "penetrate its support."]
   [[:mcu :support :lock :plate :clearance]
    {:default 1 :parse-fn num}
    "The distance between the MCU PCB and the base of the plate, in mm.\n\n"
    "Unlike the base of the plate, its clearance displaces the PCB and cannot "
    "be targeted by `tweaks`, but both parts of the plate have the same "
    "length and width.\n\n"
    "The main use for `clearance` is to leave room between a wall supporting "
    "the lock and the PCB’s through-holes, so its height should be roughly "
    "matched to the length of wire overshoot through the PCB, with a safety "
    "margin for air."]
   [[:mcu :support :lock :socket]
    "A housing around the USB connector on the MCU PCBA."]
   [[:mcu :support :lock :socket :thickness]
    {:default 1 :parse-fn num}
    "The wall thickness of the socket."]
   [[:mcu :support :lock :bolt]
    "The bolt of the MCU lock, named by analogy with a regular door lock, is "
    "not to be confused with the threaded fastener holding it in place. "
    "The properties of the threaded fastener are set using "
    "`fastener-properties` above while the properties of the lock bolt are set "
    "here."]
   [[:mcu :support :lock :bolt :clearance]
    {:default 1 :parse-fn num}
    "The distance of the bolt from the populated side of the PCB. "
    "This distance should be slightly greater than the height of the tallest "
    "component on the PCB."]
   [[:mcu :support :lock :bolt :overshoot]
    {:default 1 :parse-fn num}
    "The distance across which the bolt will touch the PCB at the mount end. "
    "Take care that this distance is free of components on the PCB."]
   [[:mcu :support :lock :bolt :mount-length]
    {:default 1 :parse-fn num}
    "The length of the base containing a threaded channel used to secure the "
    "bolt over the MCU. This is in addition to `overshoot` and "
    "goes in the opposite direction, away from the PCB."]
   [[:mcu :support :lock :bolt :mount-thickness]
    {:default 1 :parse-fn num}
    "The thickness of the mount. This should have some rough correspondence "
    "to the threaded portion of your fastener, which should not have a shank."]
   [[:ports]
    {:freely-keyed true
     :default {}
     :parse-fn (parse/map-of keyword (base/parser-with-defaults port/raws))
     :validate [(spec/map-of
                  ::valid/alias
                  (base/delegated-validation port/raws))]}
    "This section describes ports, including sockets in the case walls to "
    "contain electronic receptacles for signalling connections and other "
    "interfaces. Each port gets its own subsection. "
    "Ports are documented in detail [here](options-ports.md)."]
   [[:resolution]
    "Settings for the amount of detail on curved surfaces. More specific "
    "resolution parameters are available in other sections."]
   [[:resolution :include]
    {:default false :parse-fn boolean}
    "If `true`, apply resolution parameters found throughout the "
    "configuration. Otherwise, use defaults built into this application, "
    "its libraries and OpenSCAD. The defaults are generally conservative, "
    "providing quick renders for previews."]
   [[:resolution :exclude-all-threading]
    {:default false :parse-fn boolean}
    "If `true`, override all `include-threading` settings to exclude "
    "the detail of threading in all bolt holes etc."]
   [[:resolution :minimum-facet-size]
    {:default 1, :parse-fn num, :validate [::appschema/minimum-facet-size]}
    "File-wide OpenSCAD minimum face size in mm."]
   [[:dfm]
    "Settings for design for manufacturability (DFM)."]
   [[:dfm :error-general]
    {:default 0 :parse-fn num}
    "A measurement in mm of errors introduced to negative space in the xy "
    "plane by slicer software and the printer you will use.\n"
    "\n"
    "The default value is zero. An appropriate value for a typical slicer "
    "and FDM printer with a 0.4 mm nozzle would be about -0.4 mm.\n"
    "\n"
    "This application will try to compensate for the error, though only for "
    "certain sensitive inserts, not for the case as a whole."]
   [[:dfm :keycaps]
    "Measurements of error, in mm, for parts of keycap models. "
    "This is separate from `error-general` because it’s especially important "
    "to have a tight fit between switch sliders and cap stems, and the "
    "size of these details is usually comparable to an FDM printer nozzle.\n"
    "\n"
    "If you will not be printing caps, ignore this section."]
   [[:dfm :keycaps :error-stem-positive]
    {:default (:error-stem-positive capdata/option-defaults) :parse-fn num}
    "Error on the positive components of stems on keycaps, such as the "
    "entire stem on an ALPS-compatible cap."]
   [[:dfm :keycaps :error-stem-negative]
    {:default (:error-stem-negative capdata/option-defaults) :parse-fn num}
    "Error on the negative components of stems on keycaps, such as the "
    "cross on an MX-compatible cap."]
   [[:mask]
    "A box limits the entire shape, cutting off any projecting by-products of "
    "the algorithms. By resizing and moving this box, you can select a "
    "subsection for printing. You might want this while you are printing "
    "prototypes for a new style of switch, MCU support etc."]
   [[:mask :size]
    {:default 1000 :parse-fn parse/pad-to-3-tuple
     :validate [::tarmi-core/point-3d]}
    "The size of the mask in mm. By default, a cube of 1 m³."]
   [[:dfm :bottom-plate]
    "DFM for bottom plates."]
   [[:dfm :bottom-plate :fastener-plate-offset]
    {:default 0 :parse-fn num}
    "A vertical offset in mm for the placement of screw holes in bottom "
    "plates. Without a slight negative offset, slicers will tend to make the "
    "holes too wide for screw heads to grip the plate securely.\n"
    "\n"
    "Notice this will not affect how screw holes are cut into the case."]
   [[:mask :center]
    {:default [0 0 500] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "The position of the center point of the mask. By default, `[0, 0, 500]`, "
    "which is supposed to mask out everything below ground level. If you "
    "include bottom plates, their thickness will automatically affect the "
    "placement of the mask beyond what you specify here."]])
