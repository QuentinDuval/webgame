(ns webgame.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

(defn show-games
  []
  [:div
   [:h1 "List of all games"]
   [:ul
    [:li
     [:a {:href "/tictactoe"} "Tic tac toe"]]
    [:li
     "And more to come (hopefully)"]
   ]])

(reagent/render [show-games]
  (js/document.getElementById "app"))
