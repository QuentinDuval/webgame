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
;; GAME MECHANICS
;; - Edit / Observe mode (pause)
;; - Add elements by user click
;; - Configurable speed (ticks)
;; ------------------------------------------------------

(defonce game-state
  (atom #{[5 5] [5 6] [6 5]}))




;; ------------------------------------------------------
;; DRAWING
;; ------------------------------------------------------

(defn draw-cell
  [color x y]
  [:rect {:width 1 :height 1 :x x :y y :fill color}])

(def empty-cell (partial draw-cell "white"))
(def filled-cell (partial draw-cell "black"))

(defn draw-board
  [board]
  (into 
    [:svg#board {:view-box (str "0 0 " WIDTH " " HEIGHT)}]
    (for [x (range WIDTH)
          y (range HEIGHT)]
      ^{:key [x y]}
      (if (board [x y])
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
   [draw-board @game-state]])

(reagent/render [game-of-life]
  (js/document.getElementById "app"))
