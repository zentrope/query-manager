(ns query-manager.event)

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private subscribers (atom {}))

(defn- add-sub
  [event-type f]
  (swap! subscribers (fn [s]
                       (let [orig (or (event-type s) #{})]
                         (assoc s event-type (conj orig f))))))

(defn- now
  []
  (.getTime (js/Date.)))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn map-subs
  [f event-types]
  (doseq [e event-types]
    (add-sub e f)))

(defn send-event
  [event]
  (let [topic (first event)
        data (assoc (second event) :timestamp (now))]
    (when-let [subscribers (topic @subscribers)]
      (doseq [s subscribers]
        (s [topic data])))))
