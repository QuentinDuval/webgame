(ns webgame.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defn show-games
  []
  [:div
   [:h1 "List of all games"]
   [:ul
    [:li "Tic Tac Toe"]
   ]])

(reagent/render [show-games]
  (js/document.getElementById "app"))
