;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Flanges                                                             ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Bolts, their inserts and their bosses for attaching bottom plates and other
;;; loose parts of a keyboard.

(ns dactyl-keyboard.cad.flange
  (:require [clojure.set :refer [intersection]]
            [scad-clj.model :as model]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.util :refer [loft]]
            [dactyl-keyboard.cad.body :refer [has-bottom-plate?]]
            [dactyl-keyboard.cad.misc :refer [merge-bolt flip-x]]
            [dactyl-keyboard.cad.place :as place]
            [dactyl-keyboard.misc :refer [key-to-scadstr]]
            [dactyl-keyboard.param.proc.anch :as anch]))

(defn name-module
  "Name an OpenSCAD module for the negative of a flange fastener."
  [flange]
  (str "flange_" (key-to-scadstr flange) "_negative"))

(defn- item-body
  "The resolved body of a specific flange screw."
  [getopt flange position-index]
  (anch/resolve-body getopt
                     (getopt :flanges flange :body)
                     (getopt :flanges flange :positions position-index
                             :anchoring :anchor)))

(defn build-negative
  "The shape of a screw and optional insert. Threading is disabled with
  inserts, which start just above any bottom plate.
  Written for use in defining an OpenSCAD module."
  [getopt flange]
  (let [prop (partial getopt :flanges flange)
        ins (partial prop :inserts)
        base (if (prop :bottom) (getopt :bottom-plates :thickness) 0)
        gap (max 0 (- (ins :height) base))
        ins-rt (/ (ins :diameter :top) 2)
        ins-rb (/ (ins :diameter :bottom) 2)]
    (maybe/union
      (when (prop :bolts :include)
        (merge-bolt getopt (prop :bolts :bolt-properties)
                           (when (ins :include) {:include-threading false})))
      (when (ins :include)
        (maybe/union
          (model/translate [0 0 (- (+ (ins :height) (/ (ins :length) 2)))]
            (model/union
              (model/cylinder [ins-rb ins-rt] (ins :length)))
              ;; render cone above insert as DFM
              (model/translate [0 0 (- (/ (+ (ins :length) ins-rt) 2))]
                (model/cylinder [0 ins-rt] ins-rt)))
          (when-not (zero? gap)
            ;; Add a channel along the screw for placing the insert.
            ;; Raise it above the bottom plate, for bottom flanges.
            ;; TODO: Parameterize this to allow placing the insert from an
            ;; arbitrary direction and distance.
            ;; TODO: Styles of inserts, including scad-klupe’s square nuts.
            (maybe/translate [0 0 (- (+ base (/ gap 2)))]
              (model/cylinder ins-rb gap))))))))

(defn segment-model
  "Take a boss segment configuration. Return OpenSCAD scaffolding."
  ;; TODO: Expand this style & size subsystem and apply it to other things,
  ;; like key-mount wall segments, secondary anchors and tweaks.
  [{:keys [style size]}]
  (let [[x y z] size]
    (case style
      ;; TODO: Selectively apply scaling to a unit sphere etc. where the stated
      ;; size is not uniform.
      :cube (model/cube x y z)
      :cylinder (model/cylinder (/ x 2) z)
      :sphere (model/sphere (/ x 2)))))

(defn- segment-from-zero
  [getopt flange position-index segment]
  (maybe/translate
    (place/flange-segment-offset getopt flange position-index segment)
    (segment-model (getopt :flanges flange :bosses :segments segment))))

(defn boss-model
  "Take a boss segment configuration and range. Return OpenSCAD scaffolding."
  [getopt flange position-index]
  (let [prop (partial getopt :flanges flange)]
    (loft (map (partial segment-from-zero getopt flange position-index)
               (range 0 (inc (apply max (keys (prop :bosses :segments)))))))))

(defn- flatten-flanges
  "Generate a list of all unique flange names and positions therein."
  [getopt]
  (remove nil?
    (for [flange (keys (getopt :flanges))
          n (range (count (getopt :flanges flange :positions)))]
      (when (getopt :flanges flange :include)
        [flange n]))))

(defn- add-body-fn [body] (fn [bodies] (conj (or bodies #{}) body)))

(defn- map-flanges-to-bodies
  [getopt]
  (reduce (fn [coll [flange position-index]]
            (let [body (item-body getopt flange position-index)]
              (update coll flange (add-body-fn body))))
          {}
          (flatten-flanges getopt)))

(defn relevant-modules
  "Name all flange OpenSCAD modules relevant to any of named bodies.
  This does not take e.g. bottom-plate inclusion into account."
  [getopt & bodies]
  (->> (remove
         (fn [[_ relevant-set]]
           (empty? (intersection (set bodies) relevant-set)))
         (map-flanges-to-bodies getopt))
    (map first)  ; Take flange keywords.
    (set)  ; Deduplicate.
    (sort)  ; Deterministic.
    (mapv name-module)))

(defn- item-in-place
  "One model of a bolt, insert and/or boss, in place.
  If the item should be reflected, do so at the upper level.
  If the item should be reflected and is also negative space, assume it is
  chiral and flip it in place at the lower well as well. This second flip
  should counteract the local effects of reflection at the upper level, thus
  preserving screw threads in both copies of the item.
  Target segment nil for placement so as not apply any segment-specific
  offsets."
  [getopt positive negative reflect [flange position-index]]
  {:pre [(boolean? positive) (boolean? reflect)]}
  (let [pose (partial place/flange-place getopt flange position-index nil)
        shape (maybe/union
                 (when positive
                   (boss-model getopt flange position-index))
                 (when negative
                   (model/call-module (name-module flange))))]
    (maybe/translate
      ;; Conditionally raise bottom flanges as DFM.
      [0 0 (if (and negative (getopt :flanges flange :bottom))
             (getopt :dfm :bottom-plate :fastener-plate-offset)
             0)]
      (maybe/union
        (pose shape)
        (when (or reflect  ; Reflection forced by caller.
                  (getopt :flanges flange :reflect)
                  (getopt :flanges flange :positions position-index :reflect))
          (flip-x (pose (if positive shape (flip-x shape)))))))))

(defn select  ; Exposed for unit testing only.
  "Select flange positions."
  [getopt {:keys [bodies
                  include-positive include-negative
                  include-bottom include-top]}]
  {:pre [(set? bodies) (every? keyword? bodies)]}
  (filter
    (every-pred
      (let [allowed? (set bodies)]
        (fn [[flange position-index]]
          (allowed? (item-body getopt flange position-index))))
      (fn [[flange position-index]]
        (if (getopt :flanges flange :bottom)
          (or include-bottom
              (and include-top
                   (has-bottom-plate? getopt
                     (item-body getopt flange position-index))))
          include-top))
      (fn [[flange _]]
        (or (and (getopt :flanges flange :bosses :include)
                 include-positive)
            (and (or (getopt :flanges flange :inserts :include)
                     (getopt :flanges flange :bolts :include))
                 include-negative))))
    (flatten-flanges getopt)))

(defn union
  "Shapes in place for filtered flanges."
  ;; The interface of this function is patterned after the union-3d function
  ;; for tweaks, mainly for API compatibility rather than shared semantics.
  [getopt {:keys [reflect
                  ;; Remaining keyword arguments are a subset of search
                  ;; criteria.
                  include-positive include-negative]
           :as criteria}]
  (apply maybe/union
    (map (partial item-in-place getopt
                  (boolean include-positive)
                  (boolean include-negative)
                  (boolean reflect))
         (select getopt criteria))))
