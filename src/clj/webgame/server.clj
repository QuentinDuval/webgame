(ns webgame.server
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

;; -----------------------------------------------------

(defn html-content
  [file-path]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource file-path))})

(defroutes routes
  (GET "/" _ (html-content "public/index.html"))
  (GET "/life" _ (html-content "public/life.html"))
  (GET "/spaceship" _ (html-content "public/spaceship.html"))
  (GET "/tictactoe" _ (html-content "public/tictactoe.html"))
  (resources "/"))

;; -----------------------------------------------------

(def http-handler
  (-> routes
      (wrap-defaults api-defaults)
      wrap-with-logger
      wrap-gzip))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 10555))]
    (run-server http-handler {:port port :join? false})))
