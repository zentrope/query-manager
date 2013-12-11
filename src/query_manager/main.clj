(ns query-manager.main
  (:gen-class)
  (:require [query-manager.web :as web]
            [query-manager.events :as events]
            [query-manager.state :as state]
            [clojure.tools.logging :as log]))

;;-----------------------------------------------------------------------------
;; JVM Host
;;-----------------------------------------------------------------------------

(defn- on-jvm-shutdown
  [f]
  (doto (Runtime/getRuntime)
    (.addShutdownHook (Thread. f))))

(defn- evar
  [name default-value]
  (Integer/parseInt (get (System/getenv) name default-value)))

(defn- release-lock
  [lock]
  (Thread/sleep 2000)
  (deliver lock :done))

;;-----------------------------------------------------------------------------
;; Service Harnass (for now)
;;-----------------------------------------------------------------------------

(def ^:private +web-app+ (atom nil))
(def ^:private +evt-mgr+ (atom nil))

(defn- start-svc!
  [svc instance start-fn]
  (reset! svc instance)
  (start-fn instance))

(defn- stop-svc!
  [svc stop-fn]
  (when-let [instance @svc]
    (stop-fn instance)
    (reset! svc nil)))

;;-----------------------------------------------------------------------------
;; Application
;;-----------------------------------------------------------------------------

(defn- start!
  []
  (log/info "Starting query manager application.")
  ;;
  (let [port (evar "PORT" "8081")
        event-manager (events/make)
        request-q (events/put-event-q event-manager)
        response-q (events/get-event-q event-manager)
        web-app (web/make port request-q response-q)]
    (start-svc! +evt-mgr+ event-manager events/start!)
    (start-svc! +web-app+ web-app web/start!)))

(defn- stop!
  []
  (log/info "Stopping query manager application.")
  ;;
  (stop-svc! +web-app+ web/stop!)
  (stop-svc! +evt-mgr+ events/stop!))

;;-----------------------------------------------------------------------------
;; Main entry point
;;-----------------------------------------------------------------------------

(defn- load-db!
  []
  ;;
  ;; Temporary, until I work out proper state management for this
  ;; whole thing.
  ;;
  (when-let [db (query-manager.state/load-database!)]
    (state/put-db! db)))

(defn -main
  [& args]
  (let [lock (promise)]
    (load-db!)
    (on-jvm-shutdown (fn [] (stop!)))
    (on-jvm-shutdown (fn [] (release-lock lock)))
    (start!)
    (deref lock)
    (System/exit 0)))
