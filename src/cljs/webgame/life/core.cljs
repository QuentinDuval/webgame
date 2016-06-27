(ns webgame.life.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

(defn game-of-life
  []
  [:div
   [:h1 "Game of life"]
   [:h1 "TODO"]])

(reagent/render [game-of-life]
  (js/document.getElementById "app"))
