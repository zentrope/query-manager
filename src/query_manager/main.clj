(ns query-manager.main
  (:gen-class)
  (:require [query-manager.http :as web]
            [query-manager.job :as job]
            [query-manager.db :as db]
            [query-manager.repo :as repo]
            [clojure.tools.logging :as log]))

(defn- on-jvm-shutdown
  [f]
  (doto (Runtime/getRuntime)
    (.addShutdownHook (Thread. f))))

(defn- evar
  [name default-value]
  (Integer/parseInt (get (System/getenv) name default-value)))

(defonce ^:private system-state (atom {}))

(defn- start!
  []
  (let [port (evar "PORT" "8081")
        job-state (job/mk-jobs 100)
        file-repo (repo/instance)
        web-app (web/instance port job-state)]

    (reset! system-state {:web-app web-app :file-repo file-repo})

    (log/info "Starting application.")
    (repo/start! file-repo)
    (web/start! web-app)))

(defn- stop!
  []
  (log/info "Stopping application.")
  (when-let [file-repo (:file-repo @system-state)]
    (repo/stop! file-repo))
  (when-let [web-app (:web-app @system-state)]
    (web/stop! web-app))
  (reset! system-state {}))

(defn- release-lock
  [lock]
  (Thread/sleep 2000)
  (deliver lock :done))

(defn -main
  [& args]
  (db/load)
  (let [lock (promise)]
    (on-jvm-shutdown (fn [] (db/save)))
    (on-jvm-shutdown (fn [] (stop!)))
    (on-jvm-shutdown (fn [] (release-lock lock)))
    (start!)
    (deref lock)
    (System/exit 0)))
