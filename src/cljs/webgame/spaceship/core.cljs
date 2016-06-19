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

(defn command-move
  [ship [dx dy]]
  (-> ship
    (update :x #(+ % dx))
    (update :y #(+ % dy))
    ))

(defn box-position
  [ship]
  (-> ship
    (update :x #(min % MAX-W))
    (update :x #(max % MIN-W))
    (update :y #(min % MIN-H))
    (update :y #(max % MAX-H))
    ))

(def commands
  [[UP #(command-move % [0 -1])]
   [DOWN #(command-move % [0 1])]
   [LEFT #(command-move % [-1 0])]
   [RIGHT #(command-move % [1 0])]
  ])

(defn move-ship!
  "Update the ship based on the commands pushed"
  [keys]
  (swap! game-state update-in [:ship]
   (fn [ship]
     (let [to-apply (map second (filter #(keys (first %)) commands))]
       (box-position (reduce #(%2 %1) ship to-apply))
       ))
   ))

;; ---------------------------------------------------

(defn keep-bullet
  [{:keys [x y] :as bullet}]
  (and
    (< 0 x) (< x WIDTH)
    (< 0 y) (< x HEIGHT)
    ))

(def move-bullets-xf
  (comp
    (map #(update % :y - 2))
    (filter keep-bullet)))

(defn move-bullets! []
  (swap! game-state update-in [:bullets]
    #(into [] move-bullets-xf %)
    ))

(defn create-bullet!
  [{:keys [x y] :as ship}]
  (swap! game-state update-in [:bullets]
    #(conj % {:x x :y y})
    ))


;; ---------------------------------------------------
;; EVENT STREAMS 
;; ---------------------------------------------------

(defonce game-loop
  (let [input-chan (chan)]
    (go-loop []
      (let [[evt params] (<! input-chan)]
        (case evt
          ::init (prn "init")
          ::move (do (move-ship! params) (move-bullets!))
          ::fire (create-bullet! (:ship @game-state))
          ::asteroid-tick (prn "pop-asteroid")
          )
        (recur)
        ))
    input-chan
    ))

(defonce event-stream
  (let [events (frp/events)]
    (set! (.-onkeydown js/document) #(frp/deliver events [::down (.-which %)]))
    (set! (.-onkeyup js/document) #(frp/deliver events [::up (.-which %)]))
    events))

(defonce event-listener
  (let [keys (frp/reduce
               (fn [keys [msg k]]
                 (case msg
                   ::down (conj keys k)
                   ::up (disj keys k)))
               #{} event-stream)
        fire (frp/filter #(= % [::up SPACE]) event-stream)]
    
    (frp/subscribe
      (frp/map (fn [keys] [::move keys]) (frp/sample 8 keys))
      game-loop)
    
    (frp/subscribe
      (frp/map (fn [_] [::fire]) fire)
      game-loop)
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
