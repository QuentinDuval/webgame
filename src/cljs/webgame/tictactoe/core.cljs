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
          :x x
          :y y
          :fill "green"
          :on-click
          (fn rect-click []
            (swap! app-state update-in [:board x y] inc))
          }])

(defn filled-cell
  [x y]
  [:circle {:r 0.45
            :cx (+ x 0.45)
            :cy (+ y 0.45)
            :fill "yellow"
            }])

(defn tic-tac-toe
  []
  [:div
   [:h1 "Tic Tac Toe"]
   (into 
     [:svg
      {:view-box (str "0 0 " SIZE " " SIZE)
       :width 500
       :height 500}]
     (for [x (range SIZE)
           y (range SIZE)]
       ^{:key [x y]}
       (case (get-in @app-state [:board x y])
         0 (empty-cell x y)
         1 (filled-cell x y))
       ))
   ])

(reagent/render [tic-tac-toe]
  (js/document.getElementById "app"))
