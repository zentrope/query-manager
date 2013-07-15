(ns query-manager.protocols)

(defprotocol ISubscriber
  (recv! [this bus message]))

(defprotocol IBus
  (subscribe! [this topic subscriber])
  (publish! [this topic message]))
