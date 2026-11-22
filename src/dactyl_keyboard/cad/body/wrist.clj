;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Wrist Rest                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; A body for supporting the user’s wrists.

;;; This body is not generally expected to have any keys or tweaks, but can be
;;; joined to the main body by tweaks, more commonly by adjustable screws. It
;;; is distinguished by the option of multiple materials: A hard base and a
;;; soft pad, typically cast in silicone.

(ns dactyl-keyboard.cad.body.wrist
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :as tarmi :refer [sin cos π]]
            [scad-tarmi.maybe :as maybe]
            [scad-klupe.iso :as klupe :refer [bolt-length]]
            [thi.ng.geom.core :as geom :refer [tessellate vertices]]
            [thi.ng.geom.polygon :refer [polygon2 inset-polygon]]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.body :refer [wrist-plate-hull]]
            [dactyl-keyboard.cad.place :as place]
            [dactyl-keyboard.cad.poly :as poly]
            [dactyl-keyboard.misc :refer [colours]]
            [dactyl-keyboard.param.access :refer [compensator]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plinth and Pad Polyhedrons ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def first-elevation 0.0)

(defn- from-base-outline
  "Vertices based on the user-requested spline."
  [getopt inset]
  (poly/from-outline (getopt :wrist-rest :derived :outline :base) inset))

(defn- get-resolution
  "Get the effective spline resolution. Default to 1, i.e. no interpolation."
  [getopt & keys]
  (if (getopt :resolution :include)
    (apply (partial getopt :wrist-rest :shape) keys)
    1))

(defn- edge-inset
  "Compute a pad surface edge inset in mm."
  [getopt θ]
  (let [maximum (getopt :wrist-rest :shape :pad :surface :edge :inset)]
    (* maximum (- 1 (cos θ)))))

(defn- edge-elevation
  "Compute pad surface elevation."
  [getopt θ]
  (let [maximum (getopt :wrist-rest :shape :pad :height :surface-range)]
    (* maximum (sin θ))))

(defn- polygon-step
  "Vertices of one level of the pad surface edge."
  [getopt θ]
  (from-base-outline getopt (edge-inset getopt θ)))

(defn- polygon-faces
  "A list of faces by index number, for one level of a polyhedron.
  Each face has four vertices, numbered in clockwise order."
  ;; From the OpenSCAD manual:
  ;; “All faces must have points ordered in the same direction.
  ;; OpenSCAD prefers clockwise when looking at each face from outside
  ;; inwards.”
  ;; A diagram of how this is implemented here:
  ;;      |    up    |
  ;; -----3----------0-----
  ;;   CW |          | CCW along the polyhedron wall
  ;; -----2----------1-----
  ;;      |   down   |
  [n-prior n-outline vertical-index]
  (for [i (map #(+ n-prior %) (range n-outline))]
    [i
     (- i n-outline)  ; The vertex below i.
     (+ (* (dec vertical-index) n-outline)
        (mod (- (inc i) n-outline) n-outline))  ; Vertex below subsequent.
     (+ (* vertical-index n-outline) (mod (inc i) n-outline))]))  ; Subsequent.

(defn- to-3d [triangles z] (mapv (fn [p] (map #(conj % z) p)) triangles))

(defn- pad-walls
  "Points in a stepped polyhedron and faces that constitute its walls."
  [getopt]
  (let [outline (getopt :wrist-rest :derived :outline :base)
        angles (poly/subdivide-right-angle
                 (getopt :wrist-rest :derived :resolution :pad))]
    (reduce
      (fn [[points faces] index]
        (let [θ (nth angles index)
              elevation (edge-elevation getopt θ)]
          [(concat points (map #(conj % elevation) (polygon-step getopt θ)))
           (concat faces (polygon-faces (count points) (count outline) index))]))
      [(map #(conj % first-elevation) outline) []]
      (map inc (range (get-resolution getopt :pad :surface :edge :resolution))))))

(defn- transition-triangles
  "thi.ng triangles at the plinth-to-pad transition."
  [getopt]
  (let [base-polygon (getopt :wrist-rest :derived :base-polygon)
        triangles (tessellate base-polygon)]
    (if (nil? triangles)
      (throw (ex-info
               "Unable to tessellate wrist rest base. Check main points."
               {:base-polygon base-polygon})))
    triangles))

(defn- pad-surface-polyhedron
  "A scad-clj polyhedron for the basic surface shape of a rubber pad."
  [getopt]
  (let [base-polygon (getopt :wrist-rest :derived :base-polygon)
        last-elevation (getopt :wrist-rest :derived :pad-surface-height)
        floor-triangles (transition-triangles getopt)
        ceiling-triangles
          (tessellate (polygon2 (polygon-step getopt (/ π 2))))
        [points wall-faces] (pad-walls getopt)]
    (if (nil? ceiling-triangles)
      (throw (ex-info
               (str "Unable to tessellate the top of the wrist rest. "
                    "The outline of the wrist rest is probably too narrow for "
                    "the maximum inset and/or the spline resolution is too "
                    "high.")
               {:base-polygon base-polygon,
                :inset (getopt :wrist-rest :shape :pad :surface :edge :inset)})))
    (model/polyhedron
      points
      (concat wall-faces
              ;; Additional faces for the floor.
              (poly/coords-to-indices points
                (to-3d floor-triangles first-elevation))
              ;; Additional faces for the ceiling (reversed to go clockwise).
              (poly/coords-to-indices points
                (mapv reverse (to-3d ceiling-triangles last-elevation))))
      :convexity 2)))

(defn- rubber-surface-heightmap
  "A heightmap horizontally centered on the world origin."
  [getopt]
  (model/resize (conj
                  (getopt :wrist-rest :derived :spline :bounds :size)
                  (getopt :wrist-rest :derived :pad-surface-height))
    (model/surface
      (getopt :wrist-rest :shape :pad :surface :heightmap :filepath)
      :convexity 3)))

(defn- rubber-bottom
  "Parts of the rubber pad below its upper surface."
  [getopt]
  (let [prop (partial getopt :wrist-rest :derived)
        intermediate (getopt :wrist-rest :shape :pad :height :lip-to-surface)
        depth (getopt :wrist-rest :shape :pad :height :below-lip)
        width (getopt :wrist-rest :shape :lip :width)]
    (model/union
      (model/translate [0 0 (prop :z2)]
        (model/extrude-linear {:height intermediate, :center false}
          (model/polygon (prop :outline :base))))
      (model/translate [0 0 (- (prop :z2) depth)]
        (model/extrude-linear {:height depth, :center false}
          (model/polygon (inset-polygon (prop :outline :base) width)))))))

(defn- rubber-body
  [getopt]
  (model/union
    (rubber-bottom getopt)
    (model/translate [0 0 (getopt :wrist-rest :derived :z3)]
      (maybe/intersection
        (pad-surface-polyhedron getopt)
        (when (getopt :wrist-rest :shape :pad :surface :heightmap :include)
          (rubber-surface-heightmap getopt))))))

(defn- temporary-polywalls
  "Points and faces as if for an OpenSCAD polygon, but the points are
  meant to be moved around later."
  [getopt indices]
  (let [outline (getopt :wrist-rest :derived :outline :base)]
    (reduce
      (fn [[points-3d faces] index]
        [(concat points-3d (map #(conj % index) outline))
         (if (zero? index)  ; Don’t build faces from below the first index.
           faces
           (concat faces (polygon-faces (count points-3d) (count outline) index)))])
      [[] []]
      indices)))

(defn- temporary-polygon
  "Temporary points and permanent faces for an OpenSCAD polygon."
  [getopt indices]
  (let [[tmp-points wall-faces] (temporary-polywalls getopt indices)
        triangles (transition-triangles getopt)]
    [tmp-points
     (concat
       wall-faces
       ;; Additional faces for the floor.
       (poly/coords-to-indices tmp-points
         (to-3d triangles (first indices)))
       ;; Additional faces for the ceiling (reversed to go clockwise).
       (poly/coords-to-indices tmp-points
         (mapv reverse (to-3d triangles (last indices)))))]))

(defn- move-points [base mapping] (mapv #(get mapping % %) base))

(defn- plinth-mapping
  "Take a list of 3D points. Map their z coordinate to a segment ID. Return a
  hash map translating the list into the shape of a wrist-rest plinth body."
  [getopt tmp-points segments]
  (let [index-to-seg (apply merge (map-indexed hash-map segments))]
    (reduce
      (fn [hmap p]
        (let [xy (vec (take 2 p))
              segment (get index-to-seg (int (last p)))]
          (when (nil? segment)
            (throw (ex-info "Error indexing plinth segments"
                            {:map index-to-seg, :point p})))
          (assoc hmap p (place/wrist-segment-coord getopt xy segment))))
      {}
      tmp-points)))

(defn- plinth-polyhedron
  [getopt]
  (let [segments [3 1 0]
        [points faces] (temporary-polygon getopt (vec (range (count segments))))]
    (model/polyhedron
      (move-points points (plinth-mapping getopt points segments))
      faces
      :convexity 2)))

(defn- mould-mapping
  "Like plinth-mapping but for the exterior of the mould."
  ;; This is not 100% safe. The larger the mould thickness, the larger the
  ;; probability that remapping the standard transition triangles will not be
  ;; legal.
  [getopt tmp-points]
  (let [prop (partial getopt :wrist-rest :derived)
        base (prop :outline :base)
        mould (prop :outline :mould)]
    (reduce
      (fn [hmap [ox oy oz]]
        (let [[nx ny] (nth mould (.indexOf base [ox oy]))
              nz (prop (if (zero? oz) :z1 :z5))]
          (assoc hmap [ox oy oz] (place/wrist-place getopt [nx ny nz]))))
      {}
      tmp-points)))


;;;;;;;;;;;;;;;;;;;
;; Miscellaneous ;;
;;;;;;;;;;;;;;;;;;;


(defn sprues
  "Place the sprue module according to the user configuration."
  [getopt]
  (apply maybe/union
    (map (fn [anchoring]
           (place/at-named getopt
             (merge anchoring {:outline-key :sprue
                               :preserve-orientation true  ; Override!
                               ::place/n-dimensions 2})
             (model/call-module "sprue_negative")))
         (getopt :wrist-rest :sprues :positions))))

(defn sprue-negative
  "A model of a sprue. This assumes that wrist-rest rotation is modest."
  [getopt]
  (let [height (getopt :wrist-rest :derived :z5)]
    (model/translate [0 0 (/ height 2)]
      (model/cylinder (/ (getopt :wrist-rest :sprues :diameter) 2) height))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration Interface ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn derive-properties
  "Derive certain properties from the base configuration.
  z-level properties herein do not take rotation of the rest into account."
  [getopt]
  ;; In order to compute a bounding rectangle adequate for a height map, find a
  ;; temporary spline using the original user-supplied coordinates and the
  ;; final resolution.
  (let [spline-base (mapv :position
                          (getopt :wrist-rest :shape :spline :main-points))
        spline-res (get-resolution getopt :spline :resolution)
        spline-bounds (geom/bounds (polygon2 (poly/spline spline-base spline-res)))
        {bound-sw-high :p, bound-size :size} spline-bounds  ; :p is the southwest corner.
        ;; Any intrinsic rotation of the wrist rest should be made around its
        ;; center, not around its origin, so find the center.
        bound-center (mapv #(/ % 2) bound-size)
        to-around-origin (fn [point] (mapv - point bound-sw-high bound-center))
        spline-center (mapv to-around-origin spline-base)
        poly-centered (poly/spline spline-center spline-res)
        ;; Prepare to compute an offset from the centered, rotatable spline to
        ;; the anchor. This uses resolution 1 like the user input.
        bound-sw (fn [sp] (->> (poly/spline sp 1) polygon2 geom/bounds :p))
        ;; Make further preparations for derived splines used in the model.
        inset (fn [n] (poly/from-outline (vec poly-centered) n))
        lip-inset (getopt :wrist-rest :shape :lip :inset)
        z2 (getopt :wrist-rest :plinth-height)
        z1 (- z2 (getopt :wrist-rest :shape :lip :height))
        getpad (partial getopt :wrist-rest :shape :pad :height)
        pad-middle (getpad :lip-to-surface)
        z3 (+ z2 pad-middle)
        ;; The height of the pad surface is not the nominal :surface-range,
        ;; but that number adjusted by an imprecise multiplication with sin π/2.
        ;; This is because the functions that build polyhedron faces must be
        ;; able to precisely identify elevation figures like this one.
        pad-above (edge-elevation getopt (/ π 2))
        z4 (+ z3 pad-above)
        z5 (+ z4 (getopt :wrist-rest :mould-thickness))]
   {:resolution {:spline spline-res
                 :pad (get-resolution getopt :pad :surface :edge :resolution)}
    :spline {:poly spline-base
             :bounds spline-bounds}
    :base-polygon (polygon2 poly-centered)
    :outline
      {:base (inset 0)
       :lip (inset lip-inset)
       :mould (inset (+ lip-inset (- (getopt :wrist-rest :mould-thickness))))
       :sprue (inset (getopt :wrist-rest :sprues :inset))}
    :center-2d (mapv +
                 (place/at-named getopt
                   (merge (getopt :wrist-rest :anchoring)
                          {:preserve-orientation true, ::place/n-dimensions 2}))
                 (mapv - (bound-sw spline-base) (bound-sw spline-center)))
    :pad-surface-height pad-above
    :z5 z5     ; Base of the mould (as positioned for printing, not use).
    :z4 z4     ; Peak of the entire rest. Top of silicone pad.
    :z3 z3     ; Silicone-to-silicone transition at base of heightmap.
    :z2 z2     ; Top of lip. Plastic-to-silicone material transition.
    :z1 z1}))  ; Bottom of lip. All plastic.

(defn derive-mount-properties
  "Derive properties for one connection to the wrist rest."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)
        authority (prop :authority)
        heights (sort (prop :fasteners :heights))
        width (prop :blocks :width)
        depth (fn [block-key] (prop :blocks block-key :depth))
        height (fn [position] (* 2 (last position)))
        z (+ (/ (last heights) 2) (/ width 4))
        nuts (fn [base] (mapv #(conj (subvec base 0 2) %) heights))
        ofa #(place/at-named getopt (merge (prop :blocks % :anchoring)
                                           {:preserve-orientation true
                                            ::place/n-dimensions 2}))
        partner-side (assoc (ofa :partner-side) 2 z)
        wrist-side
          ;; Find the position of the wrist-side block.
          (case authority
            :mutual (assoc (ofa :wrist-side) 2 z)
            :partner-side
              (let [θ (prop :angle)
                    d (+ (/ (prop :blocks :partner-side :depth) 2)
                         (prop :blocks :distance)
                         (/ (prop :blocks :wrist-side :depth) 2))]
                (mapv + partner-side [(* d (sin θ)), (* -1 d (cos θ)), 0])))
        angle
          (case authority
            :partner-side (prop :angle)  ; The fixed angle supplied by the user.
            :mutual  ; Compute the angle from the position of the blocks.
              (- (Math/atan (apply / (map - (take 2 wrist-side)
                                            (take 2 partner-side))))))]
    {:angle angle
     :block->size {:partner-side [width (depth :partner-side) (height partner-side)]
                   :wrist-side [width (depth :wrist-side) (height wrist-side)]}
     :block->position {:partner-side partner-side
                       :wrist-side wrist-side}
     :block->nut->position {:partner-side (nuts partner-side)
                            :wrist-side (nuts wrist-side)}
     ;; [x y z] coordinates of the middle of the uppermost threaded rod:
     :midpoint (mapv #(/ % 2) (map + partner-side wrist-side))}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Threaded Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn threaded-rod
  "An unthreaded model of a threaded cylindrical rod connecting the keyboard
  and wrist rest."
  [getopt mount-index fastener-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)]
    (->>
      (prop :fasteners :bolt-properties)
      (bolt-length)
      (model/cylinder (/ (prop :fasteners :bolt-properties :m-diameter) 2))
      (model/rotate [(/ π 2) 0 (prop :derived :angle)])
      (model/translate (conj (subvec (prop :derived :midpoint) 0 2)
                             (prop :fasteners :heights fastener-index))))))

(defn nut
  "A model of a nut for use on or in wrist-rest mounts. Not in place,
  but rotated to match the threaded rod it fastens."
  [getopt mount-index block-key fastener-index]
  (let [bolt (getopt :wrist-rest :mounts mount-index :fasteners :bolt-properties)]
    (->> (klupe/nut (merge {:negative true} bolt))
      (model/rotate [(/ π 2) 0 0])
      ((compensator getopt) (:m-diameter bolt) {}))))

(defn- mount-fasteners
  "One mount’s set of connecting threaded rods."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)]
    (apply maybe/union
      (for [fastener-index (range (count (prop :fasteners :heights)))]
        (maybe/union
          (threaded-rod getopt mount-index fastener-index))))))

(defn- all-mounts
  [getopt model-fn]
  (apply maybe/union
    (for [i (range (count (getopt :wrist-rest :mounts)))]
      (model-fn getopt i))))

(defn block-model
  "A model of a mounting block. A cuboid with edges bevelled by 0.5 mm.
  The block is modelled in such a way that it will be pierced by the
  topmost threaded rod of its mount at the nominal position of the block,
  once placed in its final position (using block-in-place)."
  ;; The reason for the squarish profile is forward compatibility with
  ;; square-profile nuts in future, as well as ergonomy.
  [getopt mount-index block-key]
  (let [prop (partial getopt :wrist-rest :mounts mount-index :derived)]
    (wrist-plate-hull getopt
      (misc/bevelled-cuboid (prop :block->size block-key) 0.5))))

(defn block-in-place
  "Use the placement module without side, segment or offset."
  [getopt mount-index block-key]
  (place/wrist-block-place getopt mount-index block-key nil nil
    (block-model getopt mount-index block-key)))

(defn- partner-side-block
  "A plate on the case side for a threaded rod to the keyboard case."
  [getopt mount-index]
  (block-in-place getopt mount-index :partner-side))

(defn- wrist-side-block
  "A plate on the plinth side for a threaded rod to the keyboard case."
  [getopt mount-index]
  (block-in-place getopt mount-index :wrist-side))


;;;;;;;;;;;;;
;; Outputs ;;
;;;;;;;;;;;;;


(defn all-partner-side-blocks
  [getopt]
  (all-mounts getopt partner-side-block))

(defn hulled-block-pairs
  "Convex hulls of pairs of mount blocks.
  For use in generating a unifying bottom plate."
  [getopt]
  (apply maybe/union
    (map (fn [mount-index]
           (apply model/hull
             (model/cut (partner-side-block getopt mount-index))
             (model/cut (wrist-side-block getopt mount-index))))
         (range (count (getopt :wrist-rest :mounts))))))

(defn all-fasteners
  [getopt]
  (all-mounts getopt mount-fasteners))

(defn plinth-positive
  "A maquette of the plastic, rigid portion of a wrist rest."
  [getopt]
  (maybe/union
    (model/difference
      (plinth-polyhedron getopt)
      (place/wrist-place getopt (rubber-bottom getopt)))
    (when (= (getopt :wrist-rest :style) :threaded)
      (all-mounts getopt wrist-side-block))))

(defn plinth-plastic
  "The lower portion of a wrist rest, to be printed in a rigid material.
  This is complete except for masking and holes for a bottom plate."
  [getopt]
  (maybe/difference
    (plinth-positive getopt)
    (when (= (getopt :wrist-rest :style) :threaded)
      (model/union (all-fasteners getopt)))
    (when (getopt :wrist-rest :sprues :include)
      (sprues getopt))))

(defn rubber-insert-positive
  "The upper portion of a wrist rest, to be cast or printed in a soft material.
  This model is not fully featured."
  [getopt]
  (maybe/difference
    (model/color (:rubber colours)
      (place/wrist-place getopt
        (rubber-body getopt)))
    (all-mounts getopt wrist-side-block)))

(defn projection-maquette
  "A merged view of a wrist rest. This might be printed in hard plastic for a
  prototype but is not suitable for long-term use: It would typically be too
  hard for ergonomy and does not have all the details. It is intended purely
  for shaping a bottom plate."
  [getopt]
  (model/union
    (rubber-insert-positive getopt)
    (plinth-positive getopt)))

(defn mould-polyhedron
  "An OpenSCAD polyhedron describing the exterior of the casting mould.
  This could be simplified to an OpenSCAD extrusion of an OpenSCAD polygon,
  since it is presently just one level with straight edges."
  [getopt]
  (let [[points faces] (temporary-polygon getopt [0 1])]
    (model/polyhedron
      (move-points points (mould-mapping getopt points))
      faces
      :convexity 2)))
