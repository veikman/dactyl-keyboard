;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Auxiliary Features                                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.auxf
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :refer [π]]
            [scad-tarmi.maybe :as maybe]
            [scad-klupe.iso :as iso :refer [nut]]
            [dactyl-keyboard.cad.misc :as misc :refer [merge-bolt]]
            [dactyl-keyboard.cad.place :as place]
            [dactyl-keyboard.param.proc.anch :as anch]))


;;;;;;;;;;;;;;;;
;; Back Plate ;;
;;;;;;;;;;;;;;;;

;; Plate for a connecting beam, rod etc.

(defn backplate-place
  [getopt shape]
  (->>
    shape
    (place/at-named getopt (getopt :main-body :back-plate :anchoring))
    (model/translate [0 0 (/ (getopt :main-body :back-plate :beam-height) -2)])))

(defn backplate-shape
  "A mounting plate for a connecting bar/rod/beam."
  [getopt]
  (let [height (getopt :main-body :back-plate :beam-height)
        width (+ (getopt :main-body :back-plate :fasteners :distance) height)
        depth 3
        interior-protrusion 8
        exterior-bevel 1
        interior-bevel 7]
   (model/hull
     (model/translate [0 (- interior-protrusion) 0]
       (model/cube (- width interior-bevel) depth (- height interior-bevel)))
     (model/cube width depth height)
     (model/translate [0 exterior-bevel 0]
       (model/cube (dec width) depth (dec height))))))

(defn backplate-fastener-holes
  "Two holes for screws through the back plate."
  [getopt]
  (let [d (getopt :main-body :back-plate :fasteners :bolt-properties :m-diameter)
        D (getopt :main-body :back-plate :fasteners :distance)
        hole (fn [x-offset]
               (->>
                 (model/union
                   (model/cylinder (/ d 2) 25)
                   (if (getopt :main-body :back-plate :fasteners :bosses)
                     (model/translate [0 0 10]
                       (nut {:m-diameter d :height 10 :negative true}))))
                 (model/rotate [(/ π 2) 0 0])
                 (model/translate [x-offset 0 0])
                 (backplate-place getopt)))]
   (model/union
     (hole (/ D 2))
     (hole (/ D -2)))))

(defn backplate-block [getopt]
  (misc/bottom-hull (backplate-place getopt (backplate-shape getopt))))


;;;;;;;;;;;;;;;
;; LED Strip ;;
;;;;;;;;;;;;;;;

(defn- west-wall-west-points [getopt]
  (let [cluster (getopt :main-body :leds :position :cluster)
        column 0
        rows (getopt :key-clusters :derived :by-cluster cluster
               :row-indices-by-column column)]
    (for [row rows, side [:WSW :WNW]]
      (take 2 (place/wall-corner-place getopt cluster
                                       [column row] {:side side, :segment 2})))))

(defn- west-wall-east-points [getopt]
  (map (fn [[x y]] [(+ x 10) y]) (west-wall-west-points getopt)))

(defn- west-wall-led-channel [getopt]
  (let [west-points (west-wall-west-points getopt)
        east-points (west-wall-east-points getopt)]
    (model/extrude-linear {:height 50}
      (model/polygon (concat west-points (reverse east-points))))))

(defn- led-hole-position [getopt ordinal]
  (let [cluster (getopt :main-body :leds :position :cluster)
        column 0
        rows (getopt :key-clusters :derived :by-cluster cluster
                 :row-indices-by-column column)
        row (first rows)
        [x0 y0 _] (place/wall-corner-place
                    getopt cluster [column row] {:side :W, :segment 2})
        h (+ 5 (/ (getopt :main-body :leds :housing-size) 2))]
   [x0 (+ y0 (* (getopt :main-body :leds :interval) ordinal)) h]))

(defn- led-emitter-channel [getopt ordinal]
  (->> (model/cylinder (/ (getopt :main-body :leds :emitter-diameter) 2) 20)
       (model/rotate [0 (/ π 2) 0])
       (model/translate (led-hole-position getopt ordinal))))

(defn- lhousing-channel [getopt ordinal]
  (let [h (getopt :main-body :leds :housing-size)]
   (->> (model/cube 50 h h)
        (model/translate (led-hole-position getopt ordinal)))))

(defn led-holes [getopt]
  (let [holes (range (getopt :main-body :leds :amount))
        group (fn [function]
                (apply model/union (map (partial function getopt) holes)))]
    (model/union
      (model/intersection
        (west-wall-led-channel getopt)
        (group lhousing-channel))
      (group led-emitter-channel))))


;;;;;;;;;;;
;; Ports ;;
;;;;;;;;;;;

(defn- port-hole-base-shape
  [getopt id [x y z]]
  (if (= (getopt :ports id :type) :custom-cylindroid)
    (model/resize [x y z] (model/cylinder (max x y) z))
    (model/cube x y z)))

(defn port-hole-base
  "Negative space for one port."
  [getopt id]
  (let [[[_ x] [_ y] z] (place/port-hole-size getopt id)]
    (port-hole-base-shape getopt id [x y z])))

(defn- port-hole-flared
  "A negative shape that is flared on top. This version is suitable for use as
  a port model, for easier entry in case of imperfect alignment, but it is
  not suitable for use in a tweak because convex hull would widen the hole."
  [getopt id]
  (let [[[_ x] [_ y] z] (place/port-hole-size getopt id)
        size (fn [increment] [(+ x increment) (+ y increment) misc/wafer])]
    (model/union
      (port-hole-base getopt id)
      (model/translate [0 0 (/ z 2)]
        (model/hull
          (port-hole-base-shape getopt id (size 0))
          (model/translate [0 0 1]
            (port-hole-base-shape getopt id (size 1))))))))

(defn port-holder
  "Positive space for one port. Take the ID of the port, not the holder."
  [getopt id]
  {:pre [(keyword? id)
         (= (getopt :derived :anchors id ::anch/type) ::anch/port-hole)]}
  (let [[x y z] (place/port-holder-size getopt id)]
    (maybe/translate
      (place/port-holder-offset getopt {:anchor id})
      (port-hole-base-shape getopt id [x y z]))))

(defn port-tweak-post
  "A cube the thickness of the wall around a specific holder."
  [getopt id]
  {:pre [(keyword? id)]}
  (apply model/cube (repeat 3 (getopt :ports id :holder :thickness))))

(defn- port-set
  "The positive or negative space for all ports."
  [getopt bodies positive]
  (apply maybe/union
    (map (fn [id]
           (let [port (partial getopt :ports id)]
             (when (and (port :include)
                        (or (not positive) (port :holder :include))
                        ((anch/resolve-body getopt
                           (port :body)
                           (port :anchoring :anchor))
                         bodies))
               (let [subject-fn (if positive port-holder port-hole-flared)
                     subject (subject-fn getopt id)]
                 (place/at-named getopt {:anchor id} subject)))))
         (keys (getopt :ports)))))

;; Unions of the positive and negative spaces for holding all ports, in place.
(defn ports-positive [getopt bodies] (port-set getopt bodies true))
(defn ports-negative [getopt bodies] (port-set getopt bodies false))
