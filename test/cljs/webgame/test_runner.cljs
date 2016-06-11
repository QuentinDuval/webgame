(ns webgame.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [webgame.core-test]))

(enable-console-print!)

(doo-tests 'webgame.core-test)
