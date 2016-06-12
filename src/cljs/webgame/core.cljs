(ns webgame.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

(defn game-item
  [link name]
  [:li.game-list
   [:a {:href link} name]])

(defn show-games
  []
  [:div.app
   [:h1 "Available games"]
   [:ul
    [game-item "/spaceship" "Space Ship"]
    [game-item "/tictactoe" "Tic Tac Toe"]
   ]])

(reagent/render [show-games]
  (js/document.getElementById "app"))
