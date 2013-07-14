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

(defn- rem-sub!
  [event-type handler]
  (swap! subscribers (fn [subs]
                       (let [orig (or (event-type subs) #{})]
                         (assoc subs event-type (disj orig handler))))))

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

(defn unsubscribe!
  [handler event-type]
  (rem-sub! event-type handler))

(defn broadcast
  [event]
  (let [topic (first event)
        data (assoc (second event) :timestamp (now))]
    (when-let [subscribers (topic @subscribers)]
      (doseq [s subscribers]
        (s broadcast [topic data])))))
