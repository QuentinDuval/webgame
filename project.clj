(defproject webgame "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.clojure/clojurescript "1.8.51" :scope "provided"]
    [org.clojure/core.async "0.2.374"]
    
    [bk/ring-gzip "0.1.1"]
    [compojure "1.5.0"]
    [environ "1.0.3"]
    [http-kit "2.1.19"]
    [reagent "0.5.1"]
    [reagi "0.10.1"]
    [ring "1.4.0"]
    [ring/ring-defaults "0.2.0"]
    [ring.middleware.logger "0.5.0"]
    [rm-hull/monet "0.3.0"]]

  :plugins [
    [lein-cljsbuild "1.1.3"]
    [lein-environ "1.0.3"]]

  :min-lein-version "2.6.1"
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :uberjar-name "webgame.jar"

  ;; Use `lein run` if you just want to start a HTTP server, without figwheel
  :main webgame.server
  :repl-options {:init-ns user}

  :cljsbuild
  {:builds {
            
            :app
            {:source-paths ["src/cljs"]
             :figwheel true
             :compiler {:main webgame.core
                        :asset-path "js/compiled/out/app"
                        :output-to "resources/public/js/compiled/webgame.js"
                        :output-dir "resources/public/js/compiled/out/app"
                        :source-map-timestamp true}}
            
            :tictactoe
            {:source-paths ["src/cljs"]
             :figwheel true
             :compiler {:main webgame.tictactoe.core
                        :asset-path "js/compiled/out/tictactoe"
                        :output-to "resources/public/js/compiled/tictactoe.js"
                        :output-dir "resources/public/js/compiled/out/tictactoe"
                        :source-map-timestamp true}}
            
            :spaceship
            {:source-paths ["src/cljs"]
             :figwheel true
             :compiler {:main webgame.spaceship.core
                        :asset-path "js/compiled/out/spaceship"
                        :output-to "resources/public/js/compiled/spaceship.js"
                        :output-dir "resources/public/js/compiled/out/spaceship"
                        :source-map-timestamp true}}
            
            :game-of-life
            {:source-paths ["src/cljs"]
             :figwheel true
             :compiler {:main webgame.life.core
                        :asset-path "js/compiled/out/life"
                        :output-to "resources/public/js/compiled/life.js"
                        :output-dir "resources/public/js/compiled/out/life"
                        :source-map-timestamp true}}
            }}

  :figwheel {:server-port 3449
             :server-ip "127.0.0.1"
             :css-dirs ["resources/public/css"]
             :ring-handler user/http-handler
             :server-logfile "log/figwheel.log"}

  :doo {:build "test"}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.3-2"]
                             [figwheel-sidecar "0.5.3-2"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.12"]]

              :plugins [[lein-figwheel "0.5.3-2"]
                        [lein-doo "0.1.6"]]

              :source-paths ["dev"]

              :cljsbuild {:builds
                          {:test
                           {:source-paths ["src/cljs" "test/cljs"]
                            :compiler
                            {:output-to "resources/public/js/compiled/testable.js"
                             :main webgame.test-runner
                             :optimizations :none}}}}}

             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :hooks [leiningen.cljsbuild]
              :omit-source true
              :aot :all
              :cljsbuild
              {:builds
               {
                :app
                {:source-paths ^:replace ["src/cljs"]
                 :compiler
                 {:optimizations :advanced
                  :pretty-print false}}
                
                :tictactoe
                {:source-paths ^:replace ["src/cljs"]
                 :compiler
                 {:optimizations :advanced
                  :pretty-print false}}
                
                :spaceship
                {:source-paths ^:replace ["src/cljs"]
                 :compiler
                 {:optimizations :advanced
                  :pretty-print false}}
                
                :game-of-life
                {:source-paths ^:replace ["src/cljs"]
                 :compiler
                 {:optimizations :advanced
                  :pretty-print false}}
                
               }}}})
