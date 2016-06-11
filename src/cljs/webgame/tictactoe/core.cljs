(ns webgame.tictactoe.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

(def app-state
  (atom
    {:text "Hopefully tic tac toe soon"}
  ))

(defn tic-tac-toe
  []
  [:h1 (:text @app-state)])

(reagent/render [tic-tac-toe]
  (js/document.getElementById "app"))
