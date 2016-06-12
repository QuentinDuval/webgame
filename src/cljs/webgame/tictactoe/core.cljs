(ns webgame.tictactoe.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

;; Based on: https://www.youtube.com/watch?v=pIiOgTwjbes

(def SIZE 3) ; Change the size to play enhanced tic-tac-toe
(def BLANK 0)
(def PLAYER 1)
(def AI 2)

(defn new-board
  "Create new empty board"
  []
  (let [col (vec (repeat SIZE BLANK))]
    (vec (repeat SIZE col))))

(def init-state
  {:board (new-board)
   :game-status :in-progress })

(def app-state
  (atom init-state))

(defn all-empty-cells
  "Return the coordinates of all cells that are empty"
  [board]
  (let [blank? #(= BLANK (get-in board [%1 %2]))]
    (for [x (range SIZE)
          y (range SIZE)
          :when (blank? x y)]
      [x y])))

(defn full-board?
  "Indicates if all cells are empty"
  [board]
  (empty? (all-empty-cells board)))

(defn computer-move
  "The (dummy) code for tha AI: plays a random move"
  [board]
  (let [remaining (all-empty-cells board)
        [x y] (rand-nth remaining)]
    (assoc-in board [x y] AI)))

(defn owns-line?
  "Checks if a player has the whole line"
  [player board [x y] [dx dy]]
  (let [line (for [i (range SIZE)]
               [(+ x (* dx i)) (+ y (* dy i))])]
    (every? #(= player %)
      (map (fn [[x y]] (get-in board [x y])) line))
    ))

(defn is-winner?
  "Check the victory condition for the given player on the board"
  [player board]
  (some true?
    (for [x (range SIZE)
          y (range SIZE)
          d [[1 0] [0 1] [1 1] [1 -1]]]
      (owns-line? player board [x y] d)
      )))

(defn game-status
  "Check the on-going game status"
  [board]
  (cond
    (is-winner? PLAYER board) :player-victory
    (is-winner? AI board)     :ai-victory
    (full-board? board)       :draw-game
    :else                     :in-progress
    ))

(defn on-player-move
  "The game logic, that occurs upon player click"
  [x y]
  (swap! app-state assoc-in [:board x y] PLAYER)
  (when (= (game-status (:board @app-state)) :in-progress)
    (swap! app-state update-in [:board] computer-move))
  (swap! app-state assoc-in [:game-status]
    (game-status (:board @app-state)))
  )

(defn empty-cell
  "Draws an empty cell"
  [x y]
  [:rect {:width 0.9
          :height 0.9
          :x (+ 0.05 x)
          :y (+ 0.05 y)
          :fill "grey"
          :on-click
          (fn on-rect-click []
            (when (= (:game-status @app-state) :in-progress)
              (on-player-move x y)))
          }])

(defn circle-cell
  "Draws a cycle"
  [x y]
  [:circle {:r 0.4
            :cx (+ x 0.5)
            :cy (+ y 0.5)
            :stroke "green"
            :stroke-width 0.1
            :fill "none"
            }])

(defn cross-cell
  "Draws a cross"
  [x y]
  [:g
   {:stroke "darkred"
    :stroke-width 0.1
    :stroke-linecap "round"
    :transform (str "translate(" (+ 0.5 x) "," (+ 0.5 y) ")"
                    "scale(0.7)")}
   [:line {:x1 -0.5 :y1 -0.5 :x2 0.5 :y2 0.5}]
   [:line {:x1 -0.5 :y1 0.5 :x2 0.5 :y2 -0.5}]
  ])

(defn tic-tac-toe
  "Main renderer for the tic-tac-toe"
  []
  [:div.app
   [:h1
    (case (:game-status @app-state)
     :player-victory "You won!"
     :ai-victory "AI won!"
     :draw-game "Draw game"
     "Tic Tac Toe")]
   [:button#new-game
    {:on-click #(reset! app-state init-state)}
    "New game"]
   (into 
     [:svg#board
      {:view-box (str "0 0 " SIZE " " SIZE)}]
     (for [x (range SIZE)
           y (range SIZE)]
       ^{:key [x y]}
       (condp = (get-in @app-state [:board x y])
         BLANK  [empty-cell x y]
         PLAYER [circle-cell x y]
         AI     [cross-cell x y])
       ))
   ])

(reagent/render [tic-tac-toe]
  (js/document.getElementById "app"))
