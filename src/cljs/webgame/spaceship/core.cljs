(ns webgame.spaceship.core 
  (:require
    [cljs.core.async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [monet.geometry :as geom]
    [reagent.core :as reagent :refer [atom]])
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

(def commands (atom #{}))


;; ---------------------------------------------------
;; SHIP
;; ---------------------------------------------------

(defn draw-ship
  "Draw the ship"
  [ctx ship]
  (-> ctx
    (canvas/save)
    (canvas/translate (:x ship) (:y ship))
    ;(canvas/rotate (:angle ship))
    (canvas/begin-path)
    (canvas/move-to (- SHIP-W) 0)
    (canvas/line-to SHIP-W 0)
    (canvas/line-to 0 (- SHIP-H))
    (canvas/fill)
    (canvas/restore)
    ))

(defn command-move!
  [{:keys [x y] :as ship} key [dx dy]]
  (if (@commands key)
    (-> ship
      (update :x #(+ % dx))
      (update :y #(+ % dy)))
    ship))

(defn box-position
  [{:keys [x y] :as ship}]
  (-> ship
    (update :x #(min % MAX-W))
    (update :x #(max % MIN-W))
    (update :y #(min % MIN-H))
    (update :y #(max % MAX-H))
    ))

(defn update-ship
  "Update the ship based on the commands pushed"
  [ship]
  (-> ship
    (command-move! UP [0 -1])
    (command-move! DOWN [0 1])
    (command-move! LEFT [-1 0])
    (command-move! RIGHT [1 0])
    box-position
    ))


;; ---------------------------------------------------
;; BULLET
;; ---------------------------------------------------

(defn draw-bullet
  [ctx bullet]
  (-> ctx
    (canvas/save)
    (canvas/translate (:x bullet) (:y bullet))
    (canvas/fill-style "red")
    ;(canvas/circle {:x 0 :y 0 :r 15}) ; Does not work
    (canvas/fill-rect {:x -3 :y (- SHIP-H) :w 6 :h 6})
    (canvas/restore)
    ))

(defn update-bullet
  [bullet]
  bullet)

(defn create-bullet
  [bullets {:keys [x y] :as ship}]
  (if (@commands SPACE) ; TODO - This results in far too many bullets - Just count the keydown
    (conj bullets {:x x :y y})
    bullets
    ))

;; ---------------------------------------------------

(def init-state
  {:ship
   {:x (/ WIDTH 2)
    :y (/ (+ MAX-H MIN-H) 2)}
   :bullets []
   })

(defn main-game-entity
  "Create a display ship entity for the provided ship atom"
  []
  (canvas/entity
    init-state
    (fn [state]
      (-> state
        (update :bullets #(mapv update-bullet %))
        (update :bullets create-bullet (:ship state))
        (update :ship update-ship)
        ))
    (fn [ctx state]
      (draw-ship ctx (:ship state))
      (doseq [b (:bullets state)]
        (draw-bullet ctx b))
      )
    ))

;; ---------------------------------------------------

(defn event-listener
  "Listener for key board events, and output the result in the provided ref"
  [output-ref]
  (let [input-chan (chan)]
    (reset! output-ref #{})
    (go-loop []
      (let [[msg k] (<! input-chan)]
        (case msg
            ::down (swap! output-ref conj k)
            ::up (swap! output-ref disj k))
        (recur)))
    (set! (.-onkeydown js/document) #(put! input-chan [::down (.-which %)]))
    (set! (.-onkeyup js/document) #(put! input-chan [::up (.-which %)]))
    ))

(defn space-ship-game
  "Render the space ship game"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :ship-entity (main-game-entity))
         (canvas/draw-loop ship-canvas)
         (event-listener commands)
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
