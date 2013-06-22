(ns query-manager.main
  (:gen-class)
  (:require [query-manager.http         :refer [app]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [clojure.tools.logging      :refer [info]]
            [org.httpkit.server         :refer [run-server]]))

(defn- evar
  [name default-value]
  (Integer/parseInt (get (System/getenv) name default-value)))

(defn- start-http
  []
  (let [port (evar "PORT" "8081")]
    (info "Running http on port" port)
    (run-server #'app {:port port})))

(defn- start-repl
  []
  (let [port (evar "NREPL_PORT" "4001")]
    (info "Running repl on port" port)
    (start-server :port port)))

(defn -main
  [& args]
  (start-http)
  (start-repl)
  (deref (promise)))
