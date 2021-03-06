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
  ([app-title port]
     (log/info "Starting query manager application.")
     ;;
     (let [event-manager (events/make)
           request-q (events/put-event-q event-manager)
           response-q (events/get-event-q event-manager)
           web-app (web/make app-title port request-q response-q)]
       (start-svc! +evt-mgr+ event-manager events/start!)
       (start-svc! +web-app+ web-app web/start!)))
  ([]
     (start! "Query Manager" (evar "PORT" "8081"))))

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
  [db-props]
  (if db-props
    (state/load-database! db-props)
    (state/load-database!))
  (state/load-queries-from-disk!)
  (state/load-jobs-from-disk!))

(defn start-embedded!
  ([app-title port properties]
     (load-db! properties)
     (on-jvm-shutdown (fn [] (stop!)))
     (start! app-title port))
  ([app-title port]
     (start-embedded! app-title port nil))
  ([port]
     (start-embedded! "Query Manager" port)))

(defn stop-embedded!
  []
  (stop!))

(defn -main
  [& args]
  (let [lock (promise)]
    (load-db! nil)
    (on-jvm-shutdown (fn [] (stop!)))
    (on-jvm-shutdown (fn [] (release-lock lock)))
    (start!)
    (deref lock)
    (System/exit 0)))
