;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Placement Utilities                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This module consolidates functions on the basis that some features can be
;;; positioned in relation to multiple other types of features, creating the
;;; need for a a high-level, delegating placement utility that builds on the
;;; rest.

(ns dactyl-keyboard.cad.place
  (:require [clojure.spec.alpha :as spec]
            [clojure.core.matrix :refer [mmul]]
            [thi.ng.geom.vector :refer [vec3]]
            [thi.ng.geom.core :as geom]
            [thi.ng.math.core :as math]
            [scad-tarmi.core :refer [abs π] :as tarmi-core]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.flex :as flex]
            [dmote-keycap.data :as capdata]
            [dmote-keycap.measure :as measure]
            [dactyl-keyboard.cots :as cots]
            [dactyl-keyboard.compass :as compass]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.key.switch :refer [mount-thickness
                                                    resting-clearance]]
            [dactyl-keyboard.param.access
             :refer [most-specific resolve-anchor key-properties
                     salient-anchoring compensator]]
            [dactyl-keyboard.param.proc.anch :as anch]))


;;;;;;;;;;;;;;;
;; Functions ;;
;;;;;;;;;;;;;;;

(declare at-named)  ;; The most general placement function.

;; Key mounts.

(defn- adaptive-corner-offset
  "Locate the corner of a switch mount based on its key style."
  [getopt cluster coord side key-style [dx dy]]
  (let [{:keys [unit-size]} (getopt :keys :derived key-style)
        [subject-x subject-y] (map measure/key-length (or unit-size [1 1]))]
    [(* 1/2 dx subject-x)
     (* 1/2 dy subject-y)
     (/ (mount-thickness getopt cluster coord) -2)]))

(defn- custom-corner-offset
  "Locate the corner of a switch mount based on explicit plate properties."
  [most [dx dy]]
  (let [[sx sy sz] (most :plate :size)]
    (mapv + [(* 1/2 dx sx) (* 1/2 dy sy) (/ sz -2)] (most :plate :position))))

(defn- mount-corner-offset
  "Produce a mm coordinate offset for a corner of a switch mount.
  This is not to be confused with offsets for walls, which build out from mount
  corners."
  [getopt cluster coord side]
  {:pre [(or (nil? side) (side compass/all))]}
  (let [most #(most-specific getopt %& cluster coord
                             (or side :dactyl-keyboard.cad.key/any))
        factors (misc/grid-factors side)]
    (if (most :plate :use-key-style)
      (adaptive-corner-offset getopt cluster coord side (most :key-style) factors)
      (custom-corner-offset most factors))))

(defn- curver
  "Given an angle for progressive curvature, apply it. Else lay keys out flat."
  [subject dimension-n rotate-type delta-fn orthographic
   rot-ax-fn getopt cluster coord obj]
  (let [index (nth coord dimension-n)
        most #(most-specific getopt %& cluster coord)
        angle-factor (most :layout rotate-type :progressive)
        neutral (most :layout :matrix :neutral subject)
        separation (most :layout :matrix :separation subject)
        space (+ capdata/mount-1u separation)
        delta-f (delta-fn index neutral)
        delta-r (delta-fn neutral index)
        angle-product (* angle-factor delta-f)
        flat-distance (* space (- index neutral))
        radius (+ (if (most :layout :clearance :use-key-style)
                    (resting-clearance getopt cluster coord)
                    (most :layout :clearance :nominal))
                  (/ (/ space 2) (Math/sin (/ angle-factor 2))))
        ortho-x (- (* delta-r (+ -1 (- (* radius (Math/sin angle-factor))))))
        ortho-z (* radius (- 1 (Math/cos angle-product)))]
   (if (zero? angle-factor)
     (flex/translate (assoc [0 0 0] dimension-n flat-distance) obj)
     (if orthographic
       (->> obj
            (rot-ax-fn angle-product)
            (flex/translate [ortho-x 0 ortho-z]))
       (misc/swing-callables flex/translate radius
                             (partial rot-ax-fn angle-product) obj)))))

(defn- put-in-column
  "Place a key in relation to its column."
  [rot-ax-fn getopt cluster coord obj]
  (curver :row 1 :pitch #(- %1 %2) false
          rot-ax-fn getopt cluster coord obj))

(defn- put-in-row
  "Place a key in relation to its row."
  [rot-ax-fn getopt cluster coord obj]
  (let [style (getopt :key-clusters :derived :by-cluster cluster :style)]
   (curver :column 0 :roll #(- %2 %1) (= style :orthographic)
           rot-ax-fn getopt cluster coord obj)))

(defn cluster-place
  "Place and tilt passed ‘subject’ as if into a key cluster.
  This uses flex, so the ‘subject’ argument can be a
  single point in 3-dimensional space, typically an offset in mm from the
  middle of the indicated key, or a scad-clj object."
  [getopt cluster coord subject]
  (let [most #(most-specific getopt (concat [:layout] %&) cluster coord)
        center (most :matrix :neutral :row)]
    (->> subject
         (flex/translate (most :translation :early))
         (flex/rotate [(most :pitch :intrinsic)
                       (most :roll :intrinsic)
                       (most :yaw :intrinsic)])
         (put-in-column #(flex/rotate [%1 0 0] %2) getopt cluster coord)
         (put-in-row #(flex/rotate [0 %1 0] %2) getopt cluster coord)
         (flex/translate (most :translation :mid))
         (flex/rotate [(most :pitch :base)
                       (most :roll :base)
                       (most :yaw :base)])
         (flex/translate [0 (* capdata/mount-1u center) 0])
         (flex/translate (most :translation :late))
         (at-named getopt (getopt :key-clusters cluster :anchoring)))))


;; Case walls extending from key mounts.

(defn wall-segment-offset  ; Exposed for unit testing purposes only.
  "Compute a 3D offset from one side of a switch mount to a part of its wall."
  [getopt cluster coord side segment]
  {:pre [(integer? segment) (not (neg? segment))]
   :post [(spec/valid? ::tarmi-core/point-3d %)]}
  ;; First, find an additive offset that assumes a positive xy orientation
  ;; appropriate for the compass metaphor’s NNE side.
  ;; Sides here have a special meaning. The NNE side, for example, is north
  ;; from the nominal northeast corner of the key mount, not 22½° from the
  ;; centre of the key mount.
  ;; Second, modify the x coordinate accordingly, negating it for e.g. NNW and
  ;; nullifying it for the cardinal direction, treating all directions as
  ;; northerly for this purpose.
  ;; Third, rotate the modified coordinates by matrix multiplication based on
  ;; the closest cardinal direction, flipping x and y accordingly so that a
  ;; user can supply a single offset that is equally useful in all directions,
  ;; and/or fully detailed exceptions for any particular side and segment.
  (let [most #(most-specific getopt [:wall :segments % :intrinsic-offset]
                             cluster coord side)
        [dx _] (misc/grid-factors (compass/northern-modulus side))
        rotation (compass/matrices (compass/convert-to-cardinal side))
        rotate (fn [[x y z]] (conj (mmul rotation [x y]) z))]
    (-> (apply mapv + (map most (range 0 (inc segment))))
      (update 0 (partial * dx))  ; dy would be redundant with northern-modulus.
      (rotate))))

(defn wall-corner-offset
  "Combined [x y z] offset from the center of a switch mount.
  This can go to one corner of the hem of the mount’s skirt of
  walling and therefore finds the base of full walls."
  [getopt cluster coordinates
   {:keys [side segment vertex] :or {segment 0, vertex false} :as keyopts}]
  {:pre [(or (nil? side) (compass/all side))]}
  (mapv +
    (mount-corner-offset getopt cluster coordinates side)
    (wall-segment-offset getopt cluster coordinates side (or segment 0))
    (if (and side vertex)
      ;; Compute a 3D offset from the center of a web post to a vertex on it.
      (matrix/cube-vertex-offset
        side
        (map #(/ % 2)
             (most-specific getopt [:wall :segments segment :size]
                            cluster coordinates side))
        keyopts)
      [0 0 0])))

(defn wall-corner-place
  "Absolute position of the lower wall around a key mount."
  ([getopt cluster coordinates]
   (wall-corner-place getopt cluster coordinates {}))
  ([getopt cluster coordinates keyopts]
   (wall-corner-place getopt cluster coordinates keyopts [0 0 0]))
  ([getopt cluster coordinates keyopts subject]
   (cluster-place getopt cluster coordinates
     (flex/translate
       (wall-corner-offset getopt cluster coordinates keyopts)
       subject))))

(defn wall-slab-center-offset
  "Combined [x y z] offset to the center of a vertical wall.
  Computed as the arithmetic average of its two corners."
  [getopt cluster coordinates direction]
  (let [c (fn [turning-fn]
            (wall-corner-offset getopt cluster coordinates
              {:side (compass/tuple-to-intermediate
                         [direction (turning-fn direction)])}))
        pair (map + (c compass/sharp-left) (c compass/sharp-right))]
    (vec (map / (vec pair) [2 2 2]))))


;; Central housing.

(defn- chousing-place
  "Place passed shape in relation to a vertex of the central housing.
  Pick the most useful precomputed 3D vertex, favouring actual vertices on
  the body of the central housing over more ethereal vertices that are not
  part of the body but correspond to its outer shell."
  [getopt index part side segment subject]
  {:pre [(nat-int? index), (keyword? part), (integer? segment),
         (#{:gabel :adapter} part)]}
  (when-not (#{0 1} segment)
    (throw (ex-info "Invalid segment ID specified for central housing."
              {:configured-segment segment
               :available-segments #{0 1}})))
  (let [points (getopt :central-housing :derived :interface index :points)
        coord (or  ; Pick the first of a number of candidates.
               (get-in points [:above-ground part side segment])  ; Gabel.
               (get-in points [:above-ground part segment])  ; Adapter.
               (get-in points [:ethereal part]))]  ; Fallback even for at-ground.
    (flex/translate coord subject)))

(defn- next-chousing-point
  "Find the properties of a central-housing interface point.
  Apply an arbitrary filter to the interface, remapping indices."
  [getopt pred base-index index-offset]
  {:pre [(integer? base-index) (integer? index-offset)]}
  (let [raw-interface (getopt :central-housing :derived :interface)
        base-item (get raw-interface base-index)
        cooked-interface (filterv pred raw-interface)
        is-base #(when (= %2 base-item) %1)
        cooked-index (->> cooked-interface
                       (map-indexed is-base) (filter some?) first)
        next-index (misc/shallow-wrap cooked-interface
                                      (+ cooked-index index-offset))]
    (get cooked-interface next-index)))

(defn- chousing-fastener-landmark
  "Find a 3-tuple of coordinates for a fastener element for the central
  housing adapter."
  [getopt name {:keys [index]} distance]
  {:pre [(or (keyword? name) (nil? name))]
   :post [(spec/valid? ::tarmi-core/point-3d %)]}
  (if name
    (at-named getopt {:anchor name})
    ;; Else find the next point of the interface above ground.
    (let [point (next-chousing-point
                  getopt :above-ground index (math/sign distance))]
      (mapv + [(/ (getopt :central-housing :shape :width) 2) 0 0]
              (get-in point [:base :offset])))))

(defn chousing-fastener
  "Placement function for an arbitrary object in relation to the site of a
  fastener connecting the central housing to the main body on one side."
  ;; This assumes the wall is planar, and will therefore work poorly with
  ;; complex central-housing adapters and wall tweaks. Custom offsets and
  ;; angles may need to be added to the parameter set.
  [getopt {:keys [starting-point direction-point axial-offset radial-offset]} subject]
  (let [pred (fn [{::anch/keys [type]}] (= type ::anch/central-gabel))
        anchor (resolve-anchor getopt starting-point pred)
        starting-coord (vec3 (at-named getopt {:anchor starting-point}))
        target-coord (chousing-fastener-landmark
                       getopt direction-point anchor radial-offset)
        nonlocal (math/- (vec3 target-coord) starting-coord)
        ;; There’s likely a simpler way to scale a thi.ng vector by a scalar.
        multiplier (* (math/sign radial-offset) (/ radial-offset (math/mag nonlocal)))
        displacement (geom/scale (vec3 (repeat 3 multiplier)) nonlocal)
        angle (- (geom/heading-yz nonlocal) (if (neg? radial-offset) π 0))]
    (flex/translate (mapv + starting-coord displacement [axial-offset 0 0])
      (flex/rotate [angle 0 0]
        subject))))


;; Rear housing.

(defn rhousing-place
  "Place in relation to the exterior of the rear housing of the main body."
  [getopt layer side segment obj]
  {:pre [(#{:interior :exterior} layer)
         (or (nil? side) (side compass/all))]}
  (let [prop (partial getopt :main-body :rear-housing)]
    (flex/translate
      (mapv +
        (misc/bevelled-corner-xyz side segment
          (prop :derived :size layer)
          (prop :bevel layer))
        (prop :derived :position layer))
      obj)))


;; Ports.

(defn port-hole-size
  "Compute the size of a port hole."
  [getopt id]
  {:pre [(= (getopt :derived :anchors id ::anch/type) ::anch/port-hole)]}
  (let [type (getopt :ports id :type)
        [xₛ yₛ zₛ] (if (type #{:custom-cuboid :custom-cylindroid})
                     (getopt :ports id :size)
                     (misc/map-to-3d-vec (type cots/port-facts)))
        [xᵢ yᵢ] (map (compensator getopt) [xₛ yₛ])]
    [[xₛ xᵢ] [yₛ yᵢ] zₛ]))

(defn port-holder-size
  "Compute the size of an open-topped port holder.
  Take the ID of the port, not the holder."
  [getopt id]
  {:pre [(= (getopt :derived :anchors id ::anch/type) ::anch/port-hole)]}
  (let [[[x _] [y _] z] (port-hole-size getopt id)
        type (getopt :ports id :holder :type)
        t (getopt :ports id :holder :thickness)]
    [(+ x t t) (+ y t t) (+ z (if (= type :through-hole) 0 t))]))

(defn port-hole-offset
  "Shift an offset for one part of a port hole.
  This is designed to hit a corner of the negative space."
  [getopt {:keys [anchor side segment] :or {segment 1}}]
  (when-not (#{0 1 2} segment)
    (throw (ex-info "Invalid segment ID specified for port hole."
              {:configured-segment segment
               :available-segments #{0 1 2}})))
  (let [[[_ x] [_ y] z] (port-hole-size getopt anchor)]
    (misc/walled-corner-xyz side segment [x y z] 0)))

(defn port-holder-offset
  "Shift an offset for one part of a port holder.
  This is designed on the assumption that what is being placed is a
  tweak post that is the size (in every direction) of the thickness of the port
  holder’s wall."
  [getopt {:keys [anchor side segment] :or {segment 1}}]
  {:pre [(keyword? anchor)
         (= (getopt :derived :anchors anchor ::anch/type) ::anch/port-hole)]}
  (when-not (#{0 1 2} segment)
    (throw (ex-info "Invalid segment ID specified for port holder."
              {:configured-segment segment
               :available-segments #{0 1 2}})))
  (let [type (getopt :ports anchor :holder :type)
        t (getopt :ports anchor :holder :thickness)]
    (mapv + (misc/walled-corner-xyz side segment (port-holder-size getopt anchor) t)
            [0 0 (if (= type :through-hole) 0 (/ t -2))])))

(defn- port-alignment-offset
  "Return a vector moving the centre of one port away from its anchor."
  [getopt id]
  (mapv - (port-hole-offset getopt
            {:anchor id
             :side (getopt :ports id :alignment :side)
             :segment (getopt :ports id :alignment :segment)})))

(defn port-place
  "Place passed object in relation to the indicated port."
  [getopt id subject]
  {:pre [(keyword? id)
         (= (getopt :derived :anchors id ::anch/type) ::anch/port-hole)]}
  (at-named getopt (getopt :ports id :anchoring)
    (flex/translate (port-alignment-offset getopt id) subject)))


;; Wrist rests.

(defn wrist-place
  "Place passed object like the plinth of the wrist rest."
  [getopt obj]
  (->>
    obj
    (flex/rotate [(getopt :wrist-rest :rotation :pitch)
                  (getopt :wrist-rest :rotation :roll)
                  0])
    (flex/translate (conj (getopt :wrist-rest :derived :center-2d)
                          (getopt :wrist-rest :plinth-height)))))

(defn wrist-undo
  "Reverse the rotation aspect of wrist-placement by repeating it in the negative.
  This is intended solely as a convenience to avoid having to rebalance models
  in the slicer."
  [getopt obj]
  (maybe/rotate [(- (getopt :wrist-rest :rotation :pitch))
                 (- (getopt :wrist-rest :rotation :roll))
                 0]
    obj))

(defn- remap-outline
  [getopt base-xy outline-key]
  (let [index (.indexOf (getopt :wrist-rest :derived :outline :base) base-xy)]
    (nth (getopt :wrist-rest :derived :outline outline-key) index)))

(defn- wrist-lip-coord
  [getopt xy outline-key]
  {:post [(spec/valid? ::tarmi-core/point-3d %)]}
  (let [nxy (remap-outline getopt xy outline-key)]
    (wrist-place getopt (conj nxy (getopt :wrist-rest :derived :z1)))))

(defn wrist-segment-coord
  "Take an xy coordinate pair as in the 2D wrist-rest spline outline and a
  segment ID number as for a case wall.
  Return vertex coordinates for the corresponding point on the plastic plinth
  of a wrist rest, in its final position.
  Segments extend outward and downward. Specifically, segment 0 is at
  the top of the lip, segment 1 is at the base of the lip, segment 2 is at
  global floor level, and all other segments are well below floor level to
  ensure that they fall below segment 1 even on a low and tilted rest."
  [getopt xy segment]
  {:pre [(vector? xy), (integer? segment)]
   :post [(spec/valid? ::tarmi-core/point-3d %)]}
  (case segment
    0 (wrist-place getopt (conj xy (getopt :wrist-rest :derived :z2)))
    1 (wrist-lip-coord getopt xy :lip)
    ; By default, recurse and override the z coordinate of segment 1.
    (assoc (wrist-segment-coord getopt xy 1) 2 (if (= segment 2) 0.0 -100.0))))

(defn- relative-to-wrist-base
  "Offset passed position relative to the base of the wrist rest."
  [getopt point]
  {:pre [(spec/valid? ::tarmi-core/point-2d point)]}
  (let [{:keys [p size]} (getopt :wrist-rest :derived :spline :bounds)]
    (mapv - point p (mapv #(/ % 2) size))))

(defn- wrist-segment-naive
  "Support outline keys as an alternative to segment IDs. With no outline key,
  use wrist-segment-coord with a layer of translation from the naïve/relative
  coordinates initially supplied by the user to the derived base."
  [getopt naive-xy outline-key segment]
  (let [aware-xy (relative-to-wrist-base getopt naive-xy)]
    (if (some? outline-key)
      (wrist-lip-coord getopt aware-xy outline-key)
      (wrist-segment-coord getopt aware-xy segment))))

(defn wrist-block-place
  "Place something for a wrist-rest mount.
  Where a side or segment is given, find a vertex on the mounting block,
  using a hardcoded 0.5 bevel."
  [getopt mount-index block-key side segment obj]
  {:pre [(integer? mount-index)
         (keyword? block-key)]}
  (let [prop (partial getopt :wrist-rest :mounts mount-index :derived)
        size (prop :block->size block-key)]
    (->> obj
      (flex/translate (misc/bevelled-corner-xyz side segment size 0.5))
      (flex/rotate [0 0 (prop :angle)])
      (flex/translate (prop :block->position block-key)))))

(defn wrist-nut-place
  "Place a nut for a wrist-rest mounting block."
  [getopt mount-index block-key fastener-index obj]
  {:pre [(integer? mount-index)
         (keyword? block-key)
         (integer? fastener-index)]}
  (let [prop (partial getopt :wrist-rest :mounts mount-index :derived)]
    (->> obj
      (flex/rotate [0 0 (prop :angle)])
      (flex/translate (prop :block->nut->position block-key fastener-index)))))

;; Flanges.

(defn flange-segment-offset
  "Compute the cumulative offset for part of a screw boss."
  [getopt flange position-index segment]
  (let [prop (partial getopt :flanges flange :bosses :segments)]
    (apply mapv + (map #(prop % :intrinsic-offset)
                       (range 0 (inc segment))))))

(defn flange-place
  "Place a flange bolt, insert and/or boss.
  For bottom flanges, force the preservation of orientation and use only the
  xy-plane for positioning relative to the anchor."
  [getopt flange position-index segment subject]
  (let [bottom (getopt :flanges flange :bottom)
        anchoring (merge (when bottom {:preserve-orientation true, ::n-dimensions 2})
                         (salient-anchoring
                           (getopt :flanges flange :positions position-index :anchoring)))]
    (cond->> subject
      segment (flex/translate (flange-segment-offset getopt flange position-index segment))
      bottom (flex/rotate [π 0 0])
      true (at-named getopt anchoring))))

;; Polymorphic treatment of the properties of aliases.

(defmulti by-type
  "The by-type multimethod dispatches placement of features in relation to
  other features, on the basis of properties associated with each anchor,
  starting with its type."
  (fn [_ properties] (::anch/type properties)))

(defmethod by-type ::anch/origin
  [_ {:keys [subject]}]
  subject)

(defmethod by-type ::anch/central-gabel
  [getopt {:keys [index subject side segment] :or {segment 0}}]
  (chousing-place getopt index :gabel side segment subject))

(defmethod by-type ::anch/central-adapter
  [getopt {:keys [index subject side segment] :or {segment 0}}]
  (chousing-place getopt index :adapter side segment subject))

(defmethod by-type ::anch/rear-housing
  [getopt {:keys [side segment subject] ::anch/keys [layer] :or {segment 3}}]
  {:pre [(some? side)]}
  (rhousing-place getopt layer side segment subject))

(defmethod by-type ::anch/wr-perimeter
  [getopt {:keys [coordinates outline-key segment subject] :or {segment 3}}]
  (flex/translate
    (wrist-segment-naive getopt coordinates outline-key segment)
    subject))

(defmethod by-type ::anch/wr-block
  [getopt {:keys [mount-index block-key side segment subject]}]
  (wrist-block-place getopt mount-index block-key side segment subject))

(defmethod by-type ::anch/wr-nut
  [getopt {:keys [mount-index block-key fastener-index subject]}]
  (wrist-nut-place getopt mount-index block-key fastener-index subject))

(defmethod by-type ::anch/key-mount
  [getopt {:keys [cluster coordinates side segment subject] :as opts}]
  {:pre [(or (nil? side) (compass/all side))]}
  (cluster-place getopt cluster coordinates
    (if (some? side)
      ;; Corner named. By default, the target feature is the outermost wall.
      (flex/translate
        (wall-corner-offset getopt cluster coordinates
          (merge opts {:side side} (when segment (:segment segment))))
        subject)
      ;; Else no corner named.
      ;; The target feature is the middle of the key mounting plate.
      subject)))

(defmethod by-type ::anch/mcu-pcba
  [getopt {:keys [side segment subject]}]
  (let [size (misc/map-to-3d-vec (getopt :mcu :derived :pcb))]
    (at-named getopt (getopt :mcu :anchoring)
      ;; MCU anchoring parameters pertain to the connector end of the PCBA.
      ;; For meaningful treatment of sides, pull the subject back from there.
      (flex/translate (mapv - (misc/walled-corner-xyz side (or segment 1) size 0)
                              [0 (/ (getopt :mcu :derived :pcb :length) 2) 0])
                      subject))))

(defmethod by-type ::anch/mcu-lock-plate
  [getopt {:keys [side segment subject]}]
  {:pre [(or (nil? side) (compass/noncardinals side))]}
  (let [size (misc/map-to-3d-vec (getopt :mcu :derived :plate))]
    (at-named getopt {:anchor :mcu-pcba}
      (flex/translate (mapv + (misc/walled-corner-xyz side (or segment 1) size 0)
                              [0
                               (/ (- (getopt :mcu :derived :plate :length)
                                     (getopt :mcu :derived :pcb :length))
                                  -2)
                               (- (getopt :mcu :derived :plate :transition)
                                  (/ (getopt :mcu :derived :plate :thickness) 2))])
                      subject))))

(defmethod by-type ::anch/mcu-grip
  [getopt {:keys [side subject]}]
  {:pre [(compass/noncardinals side)]}
  (at-named getopt {:anchor :mcu-pcba}
    (flex/translate
      (getopt :mcu :derived :pcb (compass/convert-to-intercardinal side))
      subject)))

(defmethod by-type ::anch/port-hole
  [getopt {:keys [anchor subject] :as opts}]
  (port-place getopt anchor
    (flex/translate (port-hole-offset getopt opts)
      subject)))

(defmethod by-type ::anch/port-holder
  [getopt {:keys [subject] ::anch/keys [primary] :as opts}]
  (port-place getopt primary
    (flex/translate (port-holder-offset getopt (assoc opts :anchor primary))
      subject)))

(defmethod by-type ::anch/flange-boss
  [getopt {:keys [flange position-index segment subject]}]
  (flange-place getopt flange position-index (or segment 0) subject))

(defmethod by-type ::anch/secondary
  [getopt {:keys [subject] ::anch/keys [primary]}]
  {:pre [(map? primary)]}
  ;; Apply the override by walking across the primary anchor’s position,
  ;; picking coordinates from the override where not nil.
  (flex/translate
    (map-indexed (fn [i coord] (or (get (:override primary) i) coord))
                 (at-named getopt (:anchoring primary)))
    subject))

;; Generalizations.

(defn- transformation-sequence
  [rotation translation subject]
  (->> subject (flex/rotate rotation) (flex/translate translation)))

(defn- intrinsics
  "Apply intrinsic tuning to an anchored feature.
  This is not to be confused with intrinsic rotation in the alternative sense
  that each step is performed on a coordinate system resulting from previous
  operations."
  [{:keys [intrinsic-offset intrinsic-rotation]
    :or {intrinsic-offset [0 0 0], intrinsic-rotation [0 0 0]}}
   subject]
  (transformation-sequence intrinsic-rotation intrinsic-offset subject))

(defn- extrinsics
  [{:keys [extrinsic-offset extrinsic-rotation]
    :or {extrinsic-offset [0 0 0], extrinsic-rotation [0 0 0]}}
   subject]
  (transformation-sequence extrinsic-rotation extrinsic-offset subject))

(defn- dissoc-generics
  [options & extras]
  (apply dissoc options :intrinsic-offset :intrinsic-rotation
                        :extrinsic-offset :extrinsic-rotation
                        extras))

(defn- limit-dimensions
  [{::keys [n-dimensions] :or {n-dimensions 3}} coordinates]
  (misc/limit-d n-dimensions coordinates))

(defn at-named
  "Find a position corresponding to a specific named feature.
  Differents parts of a feature can be targeted with keyword parameters.
  Return a scad-clj node or, by default, a vector of three numbers.
  General (not anchor-type-specific) parameters passed to this function will be
  applied before and after treatment specific to the named anchor, and will be
  stripped from the input to by-type so as to prevent them being applied twice
  in any subordinate call to at-named.
  Where they collide in the map of passed options, override incoming options
  with the prepared properties of the named feature as a registered anchor."
  ([getopt {:keys [anchor subject preserve-orientation]
            :or {subject [0 0 0]} :as opts}]
   {:pre [(keyword? anchor)]}
   (extrinsics opts
     (if preserve-orientation
       ;; Implicit rotation of the model with the target has been countermanded.
       ;; Recurse to get coordinates only, then translate the true subject.
       (flex/translate
         (limit-dimensions opts
           (at-named getopt
             (dissoc-generics opts :subject :preserve-orientation)))
         (intrinsics opts subject))
       ;; Else allow rotation of the subject itself (even a shape) along with the
       ;; target feature, by passing it to by-type for target-specific treatment.
       (as-> opts o
         ;; Drop neutral values and cumbersome nils.
         (salient-anchoring o)
         ;; Prevent repetition of transformations being applied here.
         (dissoc-generics o)
         ;; Add registered anchor properties, including type.
         (merge o (resolve-anchor getopt anchor))
         ;; Add initial subject.
         (assoc o :subject (intrinsics opts subject))
         (by-type getopt o)))))
  ([getopt opts subject]  ; Convenience resembling scad-clj operations.
   (at-named getopt (assoc opts :subject subject))))
