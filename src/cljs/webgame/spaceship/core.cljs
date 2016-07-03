(ns webgame.spaceship.core 
  (:require
    [cljs.core.async :as async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [monet.geometry :as geom]
    [reagent.core :as reagent :refer [atom]]
    [reagi.core :as frp])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
  ))

(enable-console-print!)

;; ---------------------------------------------------

(def ^:const WIDTH 500)
(def ^:const HEIGHT 500)
(def ^:const POP-INTERVAL 500)

(def ^:const SHIP-W 12)
(def ^:const SHIP-H 22)
(def ^:const MAX-W (- WIDTH SHIP-W))
(def ^:const MIN-W SHIP-W)
(def ^:const MAX-H SHIP-H)
(def ^:const MIN-H 500)

(def ^:const DOWN 40)
(def ^:const RIGHT 39)
(def ^:const UP 38)
(def ^:const LEFT 37)
(def ^:const SPACE 32)
(def ^:const ESCAPE 27)


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
  "Move an entity along the vector [dx dy]" 
  [entity [dx dy]]
  (-> entity
    (update :x #(+ % dx))
    (update :y #(+ % dy))
    ))

(defn force-in-board
  "Force the entity to stay on the screen" 
  [entity]
  (-> entity
    (update :x #(max (min % MAX-W) MIN-W))
    (update :y #(max (min % MIN-H) MAX-H))
    ))

(defn inside-board?
  "Checks whether an entity is still on the screen" 
  [entity]
  (geom/contained? {:x 0 :y 0 :w WIDTH :h HEIGHT} entity))

;; ---------------------------------------------------

(def keys->directions
  (keep #(get {UP [0 -1]
               DOWN [0 1]
               LEFT [-1 0]
               RIGHT [1 0]} %)))

(defn move-ship
  "Update the ship based on the commands pushed"
  [ship keys]
  (force-in-board
    (transduce keys->directions command-move ship keys)))

(defn move-entity
  "Move an non-ship entity along the y axis"
  [dy]
  (comp
    (map #(command-move % [0 dy]))
    (filter inside-board?)))

;; ---------------------------------------------------

(defn collide?
  "Simple collision system between a bullet and an asteroid" ;; TODO - Rework
  [lhs rhs]
  (geom/in-radius? lhs rhs 10))

(defn not-any-collide?
  "Indicates whether the entity collides with any of the obstacles"
  [entity obstacles]
  (not-any? #(collide? entity %) obstacles))

(defn collisions-with
  "Handle the collision of a set of elements with a list of obstacles" 
  [elements obstacles]
  (filter #(not-any-collide? % obstacles) elements))

(defn handle-collisions
  "Handle all collisions in the game, removing elements destroyed"
  [state]
  (let [next-state (-> state
                     (update :asteroids collisions-with (:bullets state))
                     (update :bullets collisions-with (:asteroids state)))
        points (- (count (:asteroids state)) (count (:asteroids next-state)))]
    (update next-state :score + points)))

(defn handle-end-game
  "Check whether the ship is destroyed by an asteroid"
  [{:keys [ship asteroids] :as state}]
  (if (not-any-collide? ship asteroids) state init-state))

(defn handle-tick
  "Handle a tick in the game: player move, entity moves, collisions"
  [state keys]
  (-> state
    (update :ship move-ship keys)
    (update :bullets #(into [] (move-entity -2) %))
    (update :asteroids #(into [] (move-entity 2) %))
    (handle-collisions)
    (handle-end-game)))

;; ---------------------------------------------------

(defn create-bullet!
  "Add a new bullet to the game, at the ship's position" 
  [{:keys [x y] :as ship}]
  (swap! game-state update :bullets
    #(conj % {:x x :y y})))

(defn create-asteroid!
  "Create a new asteroid into the game" ;; TODO - Make them appear with strange directions 
  []
  (swap! game-state update :asteroids
    #(conj % {:x (rand-int WIDTH) :y 0})))


;; ---------------------------------------------------
;; EVENT STREAMS 
;; ---------------------------------------------------

(def move-filter
  (filter #(-> % second #{DOWN RIGHT UP LEFT})))

(def to-action-xf
  (keep #(get {[::up SPACE] [::fire]
               [::up ESCAPE] [::pause]} %)))

(defonce key-chan
  (let [events (chan)
        mult (async/mult events)]
    (set! (.-onkeydown js/document) #(put! events [::down (.-which %)]))
    (set! (.-onkeyup js/document) #(put! events [::up (.-which %)]))
    {:moves (async/tap mult (chan 1 move-filter))
     :actions (async/tap mult (chan 1 to-action-xf))
    }))

(defonce moves-stream
  (let [stream (frp/events)]
    (async/pipe (:moves key-chan) (frp/port stream))
    (frp/reduce
      (fn [keys [msg k]]
        (case msg
          ::down (conj keys k)
          ::up (disj keys k)))
      #{} stream)))


;; ---------------------------------------------------
;; CONNECTION TO GAME LOOP
;; ---------------------------------------------------

(defonce game-loop
  (let [input-chan (chan)]
    (go-loop []
      (let [[evt params] (<! input-chan)]
        (if (= evt ::pause)
          (swap! game-state update :paused not)
          (when (= (:paused @game-state) false)
            (case evt
              ::init (reset! game-state init-state)
              ::move (swap! game-state handle-tick params)
              ::fire (create-bullet! (:ship @game-state))
              ::pop-asteroid (create-asteroid!))
            ))
        (recur)))
    input-chan))

(frp/subscribe
  (frp/map (fn [keys] [::move keys]) (frp/sample 8 moves-stream))
  game-loop)

(async/pipe (:actions key-chan) game-loop)

(go-loop []
  (<! (async/timeout POP-INTERVAL))
  (>! game-loop [::pop-asteroid])
  (recur))


;; ---------------------------------------------------
;; DRAWING LOOP
;; ---------------------------------------------------

(defn draw-ship
  "Draw the ship"
  [ctx ship]
  (-> ctx
    (canvas/save)
    (canvas/fill-style "black")
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
    (canvas/fill-style "red")
    (canvas/fill-rect
      {:x (- (:x bullet) 3)
       :y (- (:y bullet) (+ SHIP-H 3))
       :w 6 :h 6})))

(defn draw-asteroid
  [ctx asteroid]
  (-> ctx
    (canvas/fill-style "green")
    (canvas/fill-rect
      {:x (- (:x asteroid) 10)
       :y (- (:y asteroid) 10)
       :w 20 :h 20}))) 

(defn draw-score
  [ctx score]
  (-> ctx
    (canvas/fill-style "black")
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
      )))

;; ---------------------------------------------------

(defn space-ship-game
  "Render the space ship game"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :game-entity (main-game-entity))
         (canvas/draw-loop ship-canvas)))
     
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
