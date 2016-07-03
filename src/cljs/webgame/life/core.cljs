(ns webgame.life.core
  (:require
    [cljs.core.async :as async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [reagent.core :as reagent :refer [atom]])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)


;; ------------------------------------------------------
;; PARAMETERS
;; ------------------------------------------------------

(def WIDTH 120)
(def HEIGHT 120)
(def SCALE 5)
(def MIN-INTERVAL 40)
(def MAX-INTERVAL 120)
(def INTERVAL (quot (+ MIN-INTERVAL MAX-INTERVAL) 2))


;; ------------------------------------------------------
;; CREATING STRUCTURES
;; ------------------------------------------------------

(defn glider
  [x y]
  (for [[dx dy] [[0 0] [1 1] [1 2] [0 2] [-1 2]]]
    [(+ x dx) (+ y dy)]))

(defn square
  [x y]
  (for [dx [0 1] dy [0 1]]
    [(+ x dx) (+ y dy)]))

(defn beacon
  [x y]
  (concat
    (square (- x 2) (- y 2))
    (square x y)))

(defn star
  [x y]
  (for [dx [-1 0 1]]
    [(+ x dx) y]))

(defn toad
  [x y]
  (for [dx [0 1 2] ds [0 -1]]
    [(+ x dx ds) (+ y ds)]))

(def structure-mapping
  {:beacon beacon
   :glider glider
   :square square
   :star star
   :toad toad})

(defn new-structure
  [structure-id x y]
  ((structure-mapping structure-id) x y))


;; ------------------------------------------------------
;; GAME STATE
;; ------------------------------------------------------

(def init-board
  (into #{} (new-structure :glider 10 10)))

(defonce game-state
  (atom {:board init-board :interval INTERVAL :structure :glider}))

(def board (reagent/cursor game-state [:board]))
(def interval (reagent/cursor game-state [:interval]))
(def structure (reagent/cursor game-state [:structure]))


;; ------------------------------------------------------
;; GAME MECHANICS
;; ------------------------------------------------------

(defn in-board?
  "Checks whether a cell is in the board"
  [[x y]]
  (and (<= 0 x) (<= 0 y) (< x WIDTH) (< y HEIGHT)))

(def neighbor-matrix
  (rest (for [dx [0 1 -1] dy [0 1 -1]] [dx dy])))

(defn neighbors
  "Compute the neighbors of a cell"
  [[x y]]
  (map (fn [[dx dy]] [(+ x dx) (+ y dy)])
    neighbor-matrix))

(defn stay-alive?
  "Given the board, indicates whether a cell with a given number of neighbor shall live"
  [board cell n]
  (or (= n 3) (and (= n 2) (board cell))))

(defn next-turn
  "Compute the next board state based on the previous"
  [board]
  (into #{}
    (comp
      (partition-by identity)
      (filter #(stay-alive? board (first %) (count %)))
      (map first)
      (filter in-board?))
    (sort (mapcat neighbors board)) ;; Using frequencies is a bit slower
    ))

(defonce start-ticks
  (go-loop []
    (<! (async/timeout @interval))
    (go (swap! game-state update :board next-turn))
    (recur)))


;; ------------------------------------------------------
;; CANVAS
;; ------------------------------------------------------

(defn draw-board
  "Create a display ship entity for the provided ship atom"
  []
  (canvas/entity
    (:board @game-state)
    (fn [_] (:board @game-state))
    (fn [ctx board]
      (canvas/fill-style ctx "white")
      (doseq [[x y] board]
        (canvas/fill-rect ctx {:x (* SCALE x) :y (* SCALE y) :w SCALE :h SCALE})) 
      )))

(defn with-mouse-pos
  [handler e]
  (let [rect (-> "board" js/document.getElementById .getBoundingClientRect)]
    (handler
      (quot (- (.-pageX e) (.-left rect)) SCALE)
      (quot (- (.-pageY e) (.-top rect)) SCALE))
    ))


;; ------------------------------------------------------
;; ENTRY POINT
;; ------------------------------------------------------

(defn structures
  [selected on-select]
  [:div#structure
   [:span "Select structure"]
   [:select#selector
    {:value (name selected)
     :on-change #(-> % .-target .-value keyword on-select)}
    (for [[k _] structure-mapping]
      [:option (name k)])
    ]])

(defn time-interval
  [on-change]
  [:div#structure
   [:span "Time inverval"]
   [:input
    {:type "range"
     :min MIN-INTERVAL
     :max MAX-INTERVAL
     :value @interval
     :on-change #(-> % .-target .-value on-change)}
    ]])

(defn render-board
  []
  (let [on-add #(swap! board into (new-structure @structure %1 %2))
        on-interval-change (fn [e]
                             (println e)
                             (reset! interval e))]
    (fn []
      [:div
       [:h1 "Game of life"]
       [structures @structure #(reset! structure %)]
       [time-interval on-interval-change] 
       [:canvas#board
        {:width (* SCALE WIDTH)
         :height (* SCALE HEIGHT)
         :on-click #(with-mouse-pos on-add %)}
        ]])
    ))

(def game-of-life
  (with-meta render-board
    {:component-did-mount
     #(let [board-canvas (canvas/init (js/document.getElementById "board") "2d")]
        (canvas/add-entity board-canvas :board (draw-board))
        (canvas/draw-loop board-canvas))
     }))

(reagent/render [game-of-life]
  (js/document.getElementById "app"))
