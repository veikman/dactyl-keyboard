;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Commercial Off-the-Shelf Parts (COTS) Specifications                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This module collects data about the form factor of third-party designs to
;;; support their use as keyboard parts.

(ns dactyl-keyboard.cots
  (:require [clojure.string :refer [join]]))

(def port-facts
  "Form factors of the main bodies of supported common connectors.

  All described connectors here are receptacles (a.k.a. “female”).
  Connections occur depthwise in the local nomenclature.

  The data is based on common specs. Individual designs may vary, particularly
  for USB B which normally has large housings even for single connectors, and
  most especially for height between versions intended for mounting in
  different orientations.

  Dimensions given here generally describe the main body of a surface- or
  through-hole-mounted connector, excluding all pins, grippers, flared faces
  and other such details, but including plastic “rails” for isolation from a
  PCB, since the height added by such rails tends to be included in the metal
  housing of alternate versions standing on end."
  {:usb-full-a {:width 10 :depth 6.5 :height 13.6
                :description "full-size USB A"}
   :usb-full-2b {:width 12.1 :depth 11 :height 16.1
                 :description "full-size USB 2 B"}
   :usb-full-3b {:width 12 :depth 12.9 :height 18.3
                 :description "full-size USB 3 B"}
   :usb-mini-b {:width 7.56 :depth 4 :height 9.27
                :description "USB mini B"}
   :usb-micro-2b {:width 7.5 :depth 2.55 :height 5.9
                  :description "USB micro 2 B"}
   :usb-c {:width 9.2 :depth 3.28 :height 10.5
           :description "USB C"}
   :modular-4p4c-616e {:width 10 :depth 17.7 :height 11
                       :description (str "modular connector 4P4C, socket "
                                         "616E, minus the vertical stripe")}})

(def mcu-facts
  "Form factors etc. of printed circuit board assemblies (PCBAs) for
  common microcontroller units (MCUs)."
  ;; Starting with global default dimensions.
  {::default {:width 17.78 :thickness 1.57}
   ;; PJRC (Paul J Stoffregen and Robin C Coon) products:
   :teensy-s {:length 30  ; Rough guess.
              :port-type :usb-mini-b
              :port-overshoot 0.5  ; Rough guess.
              :description "Teensy 2.0"}
   :teensy-m {:length 35.56
              :port-type :usb-micro-2b
              :port-overshoot 0.7
              :description "Medium-size Teensy, 3.2 or LC"}
   :teensy-l {:length 53  ; Rough guess.
              :port-type :usb-mini-b
              :port-overshoot 0.5  ; Rough guess.
              :description "Teensy++ 2.0"}
   :teensy-xl {:length 61  ; Rough guess.
               :port-type :usb-micro-2b
               :port-overshoot 1.3  ; Rough guess.
               :description "Extra large Teensy, 3.5 or 3.6"}
   ;; SparkFun products:
   :promicro {:length 33
              :port-type :usb-micro-2b
              :port-overshoot 1.9
              :description "Pro Micro"}
   ;; That-Canadian products:
   :elite-c {:length 33
             :thickness 1
             :port-type :usb-c
             :port-overshoot 1.9  ; Rough guess.
             :description "Elite-C"}
   ;; QMK products:
   :proton-c {:length 52.9  ; About 34.6 mm snapped off.
              :port-type :usb-c
              :port-overshoot 0.75
              :description "Proton C"}
   ;; Raspberry Pi Foundation products:
   :rpi-pico {:width 21
              :length 51
              :port-type :usb-micro-2b
              :port-overshoot 1.3
              :description "Raspberry Pi Pico"}})


(def switch-facts
  "Form factors of switches for the purpose of cutting holes into key mounting
  plates. The dmote-keycap library has data on the upper bodies of some of
  these switches for the contrasting purpose of modelling caps."
  {:alps         {:hole {:x 15.55,  :y 12.6}
                  :foot {:x 17.25, :y 14.25}
                  :height {:above-plate 7.6, :into-plate 4.5}
                  :description "ALPS-style, including Matias"}
   :kailh-pg1511 {:description
                  (str "Cherry MX style except that there are no recesses in "
                       "the lower body of the switch; "
                       "this is true of Kailh PG1511 series switches, "
                       "including KT and KS sub-series")}
   :mx           {:hole {:x 14,    :y 14}
                  :foot {:x 15.5,  :y 15.5}
                  :height {:above-plate 5, :into-plate 5}
                  :description
                  (str "Full Cherry MX style with lateral recesses in the lower "
                       "body of the switch")}})

(defn support-list
  [coll]
  "Return a Markdown string describing passed collection,
  which must be like one of those in this module."
  (join "\n"
    (map (fn [[k {d :description}]] (format "* `%s`: %s." (name k) d))
         (sort (dissoc coll ::default)))))
