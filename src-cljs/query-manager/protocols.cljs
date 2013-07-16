(ns query-manager.protocols)

(defprotocol IView
  (dom [this]))

(defprotocol ISubscriber
  (recv! [this bus message]))

(defprotocol IBus
  (unsubscribe! [this topic subscriber])
  (subscribe! [this topic subscriber])
  (publish! [this topic message]))
