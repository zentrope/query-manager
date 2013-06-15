(ns queryizer.core
  (:gen-class )
  (:require [queryizer.view :as views]
            [clojure.tools.logging :refer [info]]
            [ring.adapter.jetty :as jetty]))

(defn- port
  []
  (Integer/parseInt (get (System/getenv) "PORT" "8081")))

(defn -main
  [& args]
  (info "Running queryizer.app on port" (port))
  (jetty/run-jetty #'views/app {:port (port) :join? true}))
