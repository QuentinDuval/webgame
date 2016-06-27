(ns webgame.life.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))


;; ------------------------------------------------------
;; PARAMETERS
;; ------------------------------------------------------

(def WIDTH 50)
(def HEIGHT 50)


;; ------------------------------------------------------
;; DRAWING
;; ------------------------------------------------------

(defn draw-cell
  [color x y]
  [:rect {:width 1 :height 1 :x x :y y :fill color}])

(def empty-cell (partial draw-cell "white"))
(def filled-cell (partial draw-cell "black"))

(defn draw-board
  []
  (into 
    [:svg#board {:view-box (str "0 0 " WIDTH " " HEIGHT)}]
    (for [x (range WIDTH)
          y (range HEIGHT)]
      ^{:key [x y]}
      (if (odd? (+ x y))
        [empty-cell x y]
        [filled-cell x y])
      )))


;; ------------------------------------------------------
;; ENTRY POINT
;; ------------------------------------------------------

(defn game-of-life
  []
  [:div
   [:h1 "Game of life"]
   [draw-board]])

(reagent/render [game-of-life]
  (js/document.getElementById "app"))
