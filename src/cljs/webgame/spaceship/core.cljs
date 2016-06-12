(ns webgame.spaceship.core 
  (:require
    [monet.canvas :as canvas]
    [monet.geometry :as geom] 
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

;; https://github.com/rm-hull/monet

;; ---------------------------------------------------

(def init-ship
  {:x 50
   :y 50
   :angle 0})

(def ship
  (atom init-ship))

(defn make-ship-entity
  [ship]
  (canvas/entity
    {:x (:x @ship)
     :y (:y @ship)
     :angle (:angle @ship)}
    (fn update
      [value]
      (-> value
        (assoc :x (:x @ship))
        (assoc :y (:y @ship))
        (assoc :angle (:angle @ship))
        ))
    (fn draw
      [ctx val]
      (-> ctx
        (canvas/save)
        (canvas/translate (:x val) (:y val))
        (canvas/rotate (:angle val))
        (canvas/begin-path)
        (canvas/move-to 30 0)
        (canvas/line-to 0 -12)
        (canvas/line-to 0 12)
        (canvas/fill)
        (canvas/restore)
        ))
    ))

(defn space-ship
  []
  (reagent/create-class
    {:component-did-mount
     (fn yolo []
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :ship-entity (make-ship-entity ship))
         (canvas/draw-loop ship-canvas)
         ))
     
     :reagent-render
     (fn render []
       [:div
        [:h1 "TODO"]
        [:button
         {:on-click (fn [] (swap! ship update-in [:x] dec))}
         "Left"]
        [:canvas#board
         {:width 500
          :height 500}
         ]]
       )
     }))


(reagent/render [space-ship]
  (js/document.getElementById "app"))
