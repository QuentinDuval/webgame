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

(def init-ship
  {:x (/ WIDTH 2)
   :y (/ (+ MAX-H MIN-H) 2)
  })

(def init-state
  {:ship init-ship
   :bullets []
   })

(def DOWN 40)
(def RIGHT 39)
(def UP 38)
(def LEFT 37)
(def SPACE 32)
(def ESCAPE 27)

(def commands (atom #{}))

;; ---------------------------------------------------

(defn draw-ship
  "Draw the ship"
  [ctx ship]
  (-> ctx
    (canvas/save)
    (canvas/translate (:x ship) (:y ship))
    (canvas/rotate (:angle ship))
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

(defn main-game-entity
  "Create a display ship entity for the provided ship atom"
  []
  (canvas/entity
    init-ship
    update-ship
    draw-ship
    ))

(defn event-listener
  "Listener for key board events, and output the result in the provided ref"
  [output-ref]
  (let [input-chan (chan)]
    (go-loop [keys #{}]
      (reset! output-ref keys)
      (let [[msg k] (<! input-chan)
            new-keys (case msg
                       ::down (conj keys k)
                       ::up (disj keys k))]
        (recur new-keys)))
    
    
    input-chan))

(defn space-ship-game
  "Render the space ship game"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :ship-entity (main-game-entity))
         (canvas/draw-loop ship-canvas)
         (let [send-chan (event-listener commands)]
           (set! (.-onkeydown js/document) #(put! send-chan [::down (.-which %)]))
           (set! (.-onkeyup js/document) #(put! send-chan [::up (.-which %)])))
         ))
     :reagent-render
     (fn render []
       [:div
        [:h1 "Space ship"]
        [:canvas#board {:width WIDTH :height HEIGHT}]
       ])
     }))


(reagent/render [space-ship-game]
  (js/document.getElementById "app"))
