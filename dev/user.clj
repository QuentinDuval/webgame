(ns user
  (:require
    [figwheel-sidecar.repl-api :as figwheel]
    [ring.middleware.reload :refer [wrap-reload]]
    [webgame.server]
  ))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def http-handler (wrap-reload #'webgame.server/http-handler))

(defn run []
  (figwheel/start-figwheel!)
  (figwheel/start-autobuild "tictactoe"))

(defn stop [] (figwheel/stop-figwheel!))

(def browser-repl figwheel/cljs-repl)
