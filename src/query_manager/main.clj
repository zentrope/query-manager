(ns query-manager.main
  (:gen-class)
  (:require [query-manager.http         :refer [mk-web-app]]
            [query-manager.job          :refer [mk-jobs]]
            [clojure.tools.logging      :refer [info]]
            [org.httpkit.server         :refer [run-server]]))

(defn- on-jvm-shutdown
  [f]
  (doto (Runtime/getRuntime)
    (.addShutdownHook (Thread. f))))

(defn- evar
  [name default-value]
  (Integer/parseInt (get (System/getenv) name default-value)))

(defrecord SystemState [http])

(defonce ^:private system-state (atom (SystemState. nil)))

(defn- start-http
  []
  (let [port (evar "PORT" "8081")
        job-state (mk-jobs 100)
        http (run-server (mk-web-app job-state) {:port port})]
    (info "Running http on port" port)
    (swap! system-state assoc :http http)))

(defn- stop-http
  []
  (when-let [http (:http @system-state)]
    (info "Stopping http.")
    (http)
    (swap! system-state :http nil)))

(defn- release-lock
  [lock]
  (Thread/sleep 2000)
  (deliver lock :done))

(defn -main
  [& args]
  (let [lock (promise)]
    (on-jvm-shutdown (fn [] (stop-http)))
    (on-jvm-shutdown (fn [] (release-lock lock)))
    (start-http)
    (deref lock)
    (System/exit 0)))
