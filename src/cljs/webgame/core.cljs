(ns webgame.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defn show-games
  []
  [:div
   [:h1 "List of all games"]
   [:ul
    [:li
     [:a {:href "/tictactoe"} "Tic tac toe"]]
   ]])

(reagent/render [show-games]
  (js/document.getElementById "app"))
