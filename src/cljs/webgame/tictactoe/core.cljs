(ns webgame.tictactoe.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

(defonce app-state
  (atom {:text "Hello Chestnut!"}))

(defn greeting []
  [:h1 (:text @app-state)])

(reagent/render [greeting]
  (js/document.getElementById "app"))
