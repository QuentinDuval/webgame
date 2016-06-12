(ns webgame.spaceship.core 
  (:require
    [cljs.core.async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [monet.geometry :as geom]
    [reagent.core :as reagent :refer [atom]])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
  ))

(enable-console-print!)

;; https://github.com/rm-hull/monet
;; https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components

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

(defn event-loop
  []
  (let [input-chan (chan)]
    (go-loop [keys #{}]
     (let [[msg key] (<! input-chan)
           new-keys (case msg
                      ::down (conj keys key)
                      ::up (disj keys key))]
       (prn new-keys)
       (recur new-keys))
     )
    input-chan))

(defn space-ship
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (prn "initialize")
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :ship-entity (make-ship-entity ship))
         (canvas/draw-loop ship-canvas)
         
         (let [send-chan (event-loop)]
           (set! (.-onkeydown js/document) #(put! send-chan [::down (.-keycode %)]))
           (set! (.-onkeyup js/document) #(put! send-chan [::up (.-keycode %)])))
         ))
     
     :reagent-render
     (fn render []
       [:div
        [:h1 "Space ship"]
        [:button
         {:on-click (fn [] (swap! ship update-in [:x] dec))}
         "Left"]
        [:canvas#board {:width 500 :height 500}]
       ])
     }))


(reagent/render [space-ship]
  (js/document.getElementById "app"))
