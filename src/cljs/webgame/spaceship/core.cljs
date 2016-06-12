(ns webgame.spaceship.core 
  (:require
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

(defn space-ship
  []
  [:div.app
   [:h1 "TODO"]
  ])

(reagent/render [space-ship]
  (js/document.getElementById "app"))
