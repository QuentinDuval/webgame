(ns webgame.spaceship.core 
  (:require
    [cljs.core.async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [monet.geometry :as geom]
    [reagent.core :as reagent :refer [atom]]
    [reagi.core :as frp])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
  ))

(enable-console-print!)

;; https://github.com/rm-hull/monet
;; https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components

;; ---------------------------------------------------

(def WIDTH 500)
(def HEIGHT 500)

(def SHIP-W 12)
(def SHIP-H 22)
(def MAX-W (- WIDTH SHIP-W))
(def MIN-W SHIP-W)
(def MAX-H SHIP-H)
(def MIN-H 500)

(def DOWN 40)
(def RIGHT 39)
(def UP 38)
(def LEFT 37)
(def SPACE 32)
(def ESCAPE 27)


;; ---------------------------------------------------
;; EVENTS HANDLING
;; ---------------------------------------------------

(def init-state
  {:ship
   {:x (/ WIDTH 2)
    :y (/ (+ MAX-H MIN-H) 2)}
   :bullets []
   })

(defonce game-state
  (atom init-state)) 

;; ---------------------------------------------------

(defn command-move!
  [[dx dy]]
  (swap! game-state update-in [:ship]
    (fn [ship]
      (-> ship
        (update :x #(+ % dx))
        (update :y #(+ % dy))
        ))
    ))

(defn box-position!
  []
  (swap! game-state update-in [:ship]
    (fn [ship]
      (-> ship
        (update :x #(min % MAX-W))
        (update :x #(max % MIN-W))
        (update :y #(min % MIN-H))
        (update :y #(max % MAX-H))))
    ))

(defn move-ship!
  "Update the ship based on the commands pushed"
  [keys]
  (prn keys) 
  (when (keys UP) (command-move! [0 -1]))
  (when (keys DOWN) (command-move! [0 1]))
  (when (keys LEFT) (command-move! [-1 0]))
  (when (keys RIGHT) (command-move! [1 0]))
  (box-position!)
  )

;; ---------------------------------------------------

(defn keep-bullet
  [{:keys [x y] :as bullet}]
  (and
    (< 0 x) (< x WIDTH)
    (< 0 y) (< x HEIGHT)
    ))

(def move-bullets
  (comp
    (map #(update % :y - 2))
    (filter keep-bullet)))

(defn move-bullets!
  []
  (swap! game-state update-in [:bullets]
    #(into [] move-bullets %)
    ))

(defn create-bullet!
  [{:keys [x y] :as ship}]
  (swap! game-state update-in [:bullets]
    #(conj % {:x x :y y})
    ))


;; ---------------------------------------------------
;; EVENT STREAMS 
;; ---------------------------------------------------

(def event-stream
  (let [events (frp/events)]
    (set! (.-onkeydown js/document) #(frp/deliver events [::down (.-which %)]))
    (set! (.-onkeyup js/document) #(frp/deliver events [::up (.-which %)]))
    events))

(defn event-listener
  "Listener for key board events, and output the result in the provided ref"
  []
  (let [keys (frp/reduce
               (fn [keys [msg k]]
                 (case msg
                   ::down (conj keys k)
                   ::up (disj keys k)))
               #{} event-stream)
        fire (frp/filter #(contains? % SPACE) keys)]
    
    (frp/map
      (fn [keys]
        (move-ship! keys)
        (move-bullets!)
        (when (keys SPACE)
          (create-bullet! (:ship @game-state))
          ))
      (frp/sample 8 keys))
    
    #_(frp/map
       #(create-bullet! (:ship @game-state))
       fire)
    ))


;; ---------------------------------------------------
;; DRAWING LOOP
;; ---------------------------------------------------

(defn draw-ship
  "Draw the ship"
  [ctx ship]
  (-> ctx
    (canvas/save)
    (canvas/translate (:x ship) (:y ship))
    (canvas/begin-path)
    (canvas/move-to (- SHIP-W) 0)
    (canvas/line-to SHIP-W 0)
    (canvas/line-to 0 (- SHIP-H))
    (canvas/fill)
    (canvas/restore)
    ))

(defn draw-bullet
  [ctx bullet]
  (-> ctx
    (canvas/save)
    (canvas/translate (:x bullet) (:y bullet))
    (canvas/fill-style "red")
    (canvas/fill-rect {:x -3 :y (- (+ SHIP-H 3)) :w 6 :h 6})
    (canvas/restore)
    ))

(defn main-game-entity
  "Create a display ship entity for the provided ship atom"
  []
  (canvas/entity
    @game-state
    (fn [_] @game-state)
    (fn [ctx state]
      (draw-ship ctx (:ship state))
      (doseq [b (:bullets state)]
        (draw-bullet ctx b))
      )
    ))

;; ---------------------------------------------------

(defn space-ship-game
  "Render the space ship game"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :ship-entity (main-game-entity))
         (canvas/draw-loop ship-canvas)
         (event-listener)
         ))
     :reagent-render
     (fn render []
       [:div
        [:h1 "Space ship"]
        [:canvas#board {:width WIDTH :height HEIGHT}]
       ])
     }))

;; ---------------------------------------------------

(reagent/render [space-ship-game]
  (js/document.getElementById "app"))
