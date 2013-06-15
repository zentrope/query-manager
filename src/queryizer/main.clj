(ns queryizer.main
  (:gen-class)
  (:require [queryizer.view :refer [app]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [clojure.tools.logging :refer [info]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn- evar
  [name default-value]
  (Integer/parseInt (get (System/getenv) name default-value)))

(defn- start-httpd
  []
  (let [port (evar "PORT" "8081")]
    (info "Running http on port" port)
    (run-jetty #'app {:port port :join? false})))

(defn- start-repl
  []
  (let [port (evar "NREPL_PORT" "4001")]
    (info "Running repl on port" port)
    (start-server :port port)))

(defn -main
  [& args]
  (start-httpd)
  (start-repl)
  (deref (promise)))
