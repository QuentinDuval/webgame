(ns webgame.tictactoe.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))


;; Based on: https://www.youtube.com/watch?v=pIiOgTwjbes

(def SIZE 3)

(defn new-board []
  (let [col (vec (repeat SIZE 0))]
    (vec (repeat SIZE col))))


(def app-state
  (atom
    {:board (new-board)}
  ))

(defn empty-cell
  [x y]
  [:rect {:width 0.9
          :height 0.9
          :x (+ 0.05 x)
          :y (+ 0.05 y)
          :fill "grey"
          :on-click
          (fn rect-click []
            (swap! app-state update-in [:board x y] inc))
          }])

(defn circle-cell
  [x y]
  [:circle {:r 0.45
            :cx (+ x 0.5)
            :cy (+ y 0.5)
            :fill "green"
            :on-click
            (fn rect-click []
              (swap! app-state update-in [:board x y] inc))
            }])

(defn cross-cell
  [x y]
  [:g
   {:stroke "darkred"
    :stroke-width 0.1
    :stroke-linecap "round"
    :transform (str "translate(" (+ 0.5 x) "," (+ 0.5 y) ")"
                    "scale(0.7)")
    }
   [:line {:x1 -0.5 :y1 -0.5
           :x2 0.5 :y2 0.5}]
   [:line {:x1 -0.5 :y1 0.5
           :x2 0.5 :y2 -0.5}]
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
       (case (get-in @app-state [:board x y])
         0 [empty-cell x y]
         1 [circle-cell x y]
         2 [cross-cell x y])
       ))
   ])

(reagent/render [tic-tac-toe]
  (js/document.getElementById "app"))
