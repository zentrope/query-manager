(ns query-manager.event
  (:use [query-manager.protocols :only [IBus ISubscriber recv!]]))

(defrecord Subscriber [topic handler]
  ISubscriber
  (recv! [this mbus message]
    (handler mbus message)))

(defrecord EventBus [subscriptions]
  IBus
  (unsubscribe! [this topic handler]
    (swap! subscriptions (fn [subs]
                           (let [orig (or (topic subs) #{})]
                             (assoc subs topic (disj orig handler))))))
  (subscribe! [this topic handler]
    (let [subscriber (Subscriber. topic handler)]
      (swap! subscriptions (fn [subs]
                             (let [clients (conj (or (topic subs) #{}) subscriber)]
                               (assoc subs topic clients))))))
  (publish! [this topic message]
    (let [subs (topic @subscriptions)]
      (doseq [sub subs]
        (recv! sub this message)))))

(defn mk-event-bus
  []
  (EventBus. (atom {})))
