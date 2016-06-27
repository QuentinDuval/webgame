(ns webgame.life.core
  (:require
    [cljs.core.async :as async :refer [put! chan <!]]
    [reagent.core :as reagent :refer [atom]])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
    ))


;; ------------------------------------------------------
;; PARAMETERS
;; ------------------------------------------------------

(def WIDTH 50)
(def HEIGHT 50)
(def INTERVAL 25)


;; ------------------------------------------------------
;; CREATING STRUCTURES
;; ------------------------------------------------------

(defmulti new-structure
  (fn [structure-id x y] structure-id))

(defmethod new-structure :square
  [_ x y]
  (for [dx [0 1]
        dy [0 1]]
    [(+ x dx) (+ y dy)]))

(defmethod new-structure :glider
  [_ x y]
  (for [[dx dy] [[0 0] [1 1] [1 2] [0 2] [-1 2]]]
    [(+ x dx) (+ y dy)]))

(defmethod new-structure :star
  [_ x y]
  (for [dx [-1 0 1]]
    [(+ x dx) y]))


;; ------------------------------------------------------
;; GAME MECHANICS
;; - Edit / Observe mode (pause)
;; - Add elements by user click
;; - Configurable speed (ticks)
;; ------------------------------------------------------

(defonce game-state
  (atom
    {:board (into #{} (new-structure :glider 10 10))
     :structure :glider
     }))

(defn in-board?
  "Checks whether a cell is in the board"
  [[x y]]
  (and (<= 0 x) (<= 0 y) (< x WIDTH) (< y HEIGHT)))

(defn neighbors
  "Compute the neighbors of a cell"
  [[x y :as old]]
  (for [dx [0 1 -1]
        dy [0 1 -1]
        :let [[x' y' :as new] [(+ x dx) (+ y dy)]]
        :when (and (not= old new) (in-board? new))]
    new))

(defn next-turn
  "Compute the next board state based on the previous"
  [board]
  (set
    (for [[pos n] (frequencies (mapcat neighbors board))
          :when (or (= n 3) (and (= n 2) (board pos)))]
      pos)))


;; ------------------------------------------------------
;; EVENT STREAMS
;; ------------------------------------------------------

(defonce start-ticks
  (go-loop []
    (<! (async/timeout INTERVAL))
    (swap! game-state update :board #'next-turn)
    (recur)))


;; ------------------------------------------------------
;; DRAWING
;; ------------------------------------------------------

(defn draw-cell
  [color on-click x y]
  [:rect {:width 1 :height 1
          :x x :y y :fill color
          :on-click #(on-click x y)}])

(def empty-cell (partial draw-cell "white"))
(def filled-cell (partial draw-cell "black"))

(defn draw-board
  [board on-click]
  (into 
    [:svg#board {:view-box (str "0 0 " WIDTH " " HEIGHT)}]
    (for [x (range WIDTH)
          y (range HEIGHT)]
      ^{:key [x y]}
      (if (board [x y])
        [empty-cell on-click x y]
        [filled-cell on-click x y])
      )))


;; ------------------------------------------------------
;; STRUCTURE SELECTION
;; ------------------------------------------------------

(defn structures
  [selected on-select]
  [:div#structure
   [:span "Select structure"]
   [:select#selector
    {:value (name selected)
     :on-change #(on-select (-> % .-target .-value keyword))}
    [:option "glider"]
    [:option "square"]
    [:option "star"]]
   ])


;; ------------------------------------------------------
;; ENTRY POINT
;; ------------------------------------------------------

(defn game-of-life
  []
  (let [board (reagent/cursor game-state [:board])
        structure (reagent/cursor game-state [:structure])
        on-select #(reset! structure %)
        on-add #(swap! board into (new-structure @structure %1 %2))]
    (fn []
      [:div
       [:h1 "Game of life"]
       [structures @structure on-select]
       [draw-board @board on-add]])
    ))

(reagent/render [game-of-life]
  (js/document.getElementById "app"))
