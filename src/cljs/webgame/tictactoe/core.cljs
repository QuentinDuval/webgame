(ns webgame.tictactoe.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))


(enable-console-print!)

;; Based on: https://www.youtube.com/watch?v=pIiOgTwjbes

(def SIZE 3)
(def BLANK 0)
(def PLAYER 1)
(def AI 2)

(defn new-board [] ;TODO - Trigger this at applications start
  (let [col (vec (repeat SIZE BLANK))]
    (vec (repeat SIZE col))))

(def app-state
  (atom {:board (new-board)}))

(defn all-empty-cells
  [board]
  (let [blank? #(= BLANK (get-in board [%1 %2]))]
    (for [x (range SIZE)
          y (range SIZE)
          :when (blank? x y)]
      [x y])))

(defn full-board?
  [board]
  (empty? (all-empty-cells board)))

(defn computer-move
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
  [x y]
  (swap! app-state assoc-in [:board x y] PLAYER)
  (when (= (game-status (:board @app-state)) :in-progress)
    (swap! app-state update-in [:board] computer-move)))

(defn empty-cell
  [x y]
  [:rect {:width 0.9
          :height 0.9
          :x (+ 0.05 x)
          :y (+ 0.05 y)
          :fill "grey"
          :on-click
          (fn on-rect-click []
            (when (= (game-status (:board @app-state)) :in-progress)
              (on-player-move x y)))
          }])

(defn circle-cell
  [x y]
  [:circle {:r 0.45
            :cx (+ x 0.5)
            :cy (+ y 0.5)
            :fill "green"
            }])

(defn cross-cell
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

(defn new-game
  []
  (swap! app-state assoc-in [:board] (new-board)))

(defn tic-tac-toe
  []
  [:div
   [:h1 "Tic Tac Toe"]
   [:p
    [:button {:on-click new-game} "New game"]]
   (into 
     [:svg
      {:view-box (str "0 0 " SIZE " " SIZE)
       :width 500
       :height 500}]
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
