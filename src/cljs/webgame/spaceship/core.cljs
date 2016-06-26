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
   :asteroids []
   :paused false
   :score 0
   })

(defonce game-state
  (atom init-state)) 

;; ---------------------------------------------------

(defn command-move
  [entity [dx dy]]
  (-> entity
    (update :x #(+ % dx))
    (update :y #(+ % dy))
    ))

(defn force-in-board
  [ship]
  (-> ship
    (update :x #(max (min % MAX-W) MIN-W))
    (update :y #(max (min % MIN-H) MAX-H))
    ))

(defn inside-board?
  [entity]
  (geom/contained?
    {:x 0 :y 0 :w WIDTH :h HEIGHT}
    entity))

;; ---------------------------------------------------

(defn keys->commands
  [keys]
  (let [mapping [[UP #(command-move % [0 -1])]
                 [DOWN #(command-move % [0 1])]
                 [LEFT #(command-move % [-1 0])]
                 [RIGHT #(command-move % [1 0])]]]
    (map second (filter #(keys (first %)) mapping))
    ))

(defn move-ship!
  "Update the ship based on the commands pushed"
  [keys]
  (swap! game-state update-in [:ship]
    (fn [ship]
      (->>
        (keys->commands keys)
        (reduce #(%2 %1) ship)
        (force-in-board)
        ))
    ))

;; ---------------------------------------------------

(defn move-entity
  [dy]
  (comp
    (map #(command-move % [0 dy]))
    (filter inside-board?)))

(defn move-entities!
  []
  (swap! game-state
    (fn [state]
      (-> state
        (update-in [:bullets] #(into [] (move-entity -2) %))
        (update-in [:asteroids] #(into [] (move-entity 2) %))
        ))
    ))

(defn create-bullet!
  [{:keys [x y] :as ship}]
  (swap! game-state update-in [:bullets]
    #(conj % {:x x :y y})
    ))

(defn create-asteroid!
  []
  (swap! game-state update-in [:asteroids]
    #(conj % {:x (rand-int WIDTH) :y 0})
    ))

;; ---------------------------------------------------

(defn switch-pause!
  []
  (swap! game-state update-in [:paused] not))

(defn collide? ;; TODO - bad approximation here
  [lhs rhs]
  (> 10 (:dist (geom/distance lhs rhs))))

(defn collisions-with
  [state obj-type obstacles]
  (update-in state [obj-type]
    (fn [asteroids]
      (filter
        (fn [a] (not-any? #(collide? a %) obstacles))
        asteroids))
    ))

(defn handle-collisions!
  []
  (swap! game-state
    (fn [state]
      (let [next-state
            (-> state
              (collisions-with :asteroids (:bullets state))
              (collisions-with :bullets (:asteroids state)))
            points (- (count (:asteroids state)) (count (:asteroids next-state)))]
        (update-in next-state [:score] + points)
        ))
    ))

;; ---------------------------------------------------
;; EVENT STREAMS 
;; ---------------------------------------------------

;; TODO - When managing collision, take into account the fact that the asteroids are sorted by Y

(defonce game-loop
  (let [input-chan (chan)]
    (go-loop []
      (let [[evt params] (<! input-chan)]
        (if (= (:paused @game-state) false)
          (case evt
            ::init (reset! game-state init-state)
            ::move (do (move-ship! params) (move-entities!) (handle-collisions!))
            ::fire (create-bullet! (:ship @game-state))
            ::pop-asteroid (create-asteroid!)
            ::pause (switch-pause!))
          (case evt
            ::pause (switch-pause!)
            (prn "ignored")))
        (recur)))
    input-chan
    ))

(defonce key-stream
  (let [events (frp/events)]
    (set! (.-onkeydown js/document) #(frp/deliver events [::down (.-which %)]))
    (set! (.-onkeyup js/document) #(frp/deliver events [::up (.-which %)]))
    events))

(defn filter-key
  [key stream]
  (frp/filter #(= % [::up key]) stream))

(defonce event-listener
  (let [moves (frp/reduce
                (fn [keys [msg k]]
                  (case msg
                    ::down (conj keys k)
                    ::up (disj keys k)))
                #{} key-stream)]
    
    (frp/subscribe
      (frp/map (fn [keys] [::move keys]) (frp/sample 8 moves))
      game-loop)
    
    (frp/subscribe
      (frp/constantly [::fire] (filter-key SPACE key-stream))
      game-loop)
    
    (frp/subscribe
      (frp/constantly [::pause] (filter-key ESCAPE key-stream))
      game-loop)
    
    (frp/subscribe
      (frp/constantly [::pop-asteroid] (frp/sample 500 moves))
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

(defn draw-asteroid
  [ctx asteroid]
  (-> ctx
    (canvas/save)
    (canvas/translate (:x asteroid) (:y asteroid))
    (canvas/fill-style "green")
    (canvas/fill-rect {:x -10 :y -10 :w 20 :h 20})
    (canvas/restore)
    )) 

(defn draw-score
  [ctx score]
  (-> ctx
    (canvas/font-style "30px Arial")
    (canvas/text {:text (str score) :x (- WIDTH 80) :y 40})
    ))

(defn main-game-entity
  "Create a display ship entity for the provided ship atom"
  []
  (canvas/entity
    @game-state
    (fn [_] @game-state)
    (fn [ctx state]
      (draw-ship ctx (:ship state))
      (draw-score ctx (:score state)) 
      (doseq [b (:bullets state)]
        (draw-bullet ctx b))
      (doseq [a (:asteroids state)]
        (draw-asteroid ctx a)) 
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
         (canvas/add-entity ship-canvas :game-entity (main-game-entity))
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
