(ns webgame.life.core
  (:require
    [cljs.core.async :as async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [reagent.core :as reagent :refer [atom]])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
    ))


(enable-console-print!)

;; ------------------------------------------------------
;; PARAMETERS
;; ------------------------------------------------------

(def WIDTH 120)
(def HEIGHT 120)
(def SCALE 5)
(def INTERVAL 80)

;; TODO - Add configurable speed

;; ------------------------------------------------------
;; CREATING STRUCTURES
;; ------------------------------------------------------

(defn glider
  [x y]
  (for [[dx dy] [[0 0] [1 1] [1 2] [0 2] [-1 2]]]
    [(+ x dx) (+ y dy)]))

(defn square
  [x y]
  (for [dx [0 1]
        dy [0 1]]
    [(+ x dx) (+ y dy)]))

(defn star
  [x y]
  (for [dx [-1 0 1]]
    [(+ x dx) y]))

(defn toad
  [x y]
  (for [dx [0 1 2]
        ds [0 -1]]
    [(+ x dx ds) (+ y ds)]))

(def structure-mapping
  {:glider glider
   :square square
   :star star
   :toad toad})

(defn new-structure
  [structure-id x y]
  ((structure-mapping structure-id) x y))


;; ------------------------------------------------------
;; GAME STATE
;; ------------------------------------------------------

(defonce game-state
  (atom
    {:board (into #{} (new-structure :glider 10 10))
     :structure :glider}))

(def board (reagent/cursor game-state [:board]))
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
  [board [cell n]]
  (or (= n 3) (and (= n 2) (board cell))))

(defn fast-frequencies
  [coll]
  (into {} ;; TODO - Use transduce - or js->clj
    (reduce
      #_(fn [counts x]
        (let [c (or (aget counts x) 0)]
          (aset counts x (inc c))
          ))
      (fn [counts x]
        (let [c (get counts x 0)]
          (assoc counts x (inc c))
          ))
      ;(clj->js {})
      {}
      coll)
    ))

(defn next-turn
  "Compute the next board state based on the previous"
  [board]
  (into #{}
    (comp
      (filter (partial stay-alive? board))
      (map first)
      (filter in-board?))
    ;(frequencies (mapcat neighbors board))
    (fast-frequencies (mapcat neighbors board))
    ))


;; ------------------------------------------------------
;; EVENT STREAMS
;; ------------------------------------------------------

(defonce start-ticks
  (go-loop []
    (<! (async/timeout INTERVAL))
    (time (swap! game-state update :board next-turn))
    (recur)))


;; ------------------------------------------------------
;; DRAWING
;; ------------------------------------------------------

(defn draw-cell
  [ctx [x y :as cell]]
  (-> ctx
    (canvas/save)
    (canvas/translate (* SCALE x) (* SCALE y))
    (canvas/fill-style "white")
    (canvas/fill-rect {:x 0 :y 0 :w SCALE :h SCALE})
    (canvas/restore)))

(defn draw-board
  "Create a display ship entity for the provided ship atom"
  []
  (canvas/entity
    (:board @game-state)
    (fn [_] (:board @game-state))
    (fn [ctx board]
      (doseq [c board] (draw-cell ctx c)) 
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
    (for [[k _] structure-mapping]
      [:option (name k)])]
   ])


;; ------------------------------------------------------
;; ENTRY POINT
;; ------------------------------------------------------

(defn with-mouse-pos
  [handler]
  (fn [e]
    (let [rect (-> "board" js/document.getElementById .getBoundingClientRect)]
      (handler
        (quot (- (.-pageX e) (.-left rect)) SCALE)
        (quot (- (.-pageY e) (.-top rect)) SCALE))
      )))

(defn render-board
  []
  (let [on-add #(swap! board into (new-structure @structure %1 %2))]
    (fn []
      [:div
       [:h1 "Game of life"]
       [structures @structure #(reset! structure %)]
       [:canvas#board
        {:width (* SCALE WIDTH)
         :height (* SCALE HEIGHT)
         :on-click (with-mouse-pos on-add)}
        ]])
    ))

(def game-of-life
  (with-meta render-board
    {:component-did-mount
     (fn []
       (let [board-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity board-canvas :board (draw-board))
         (canvas/draw-loop board-canvas)))
     }))

(reagent/render [game-of-life]
  (js/document.getElementById "app"))
