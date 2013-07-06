(ns query-manager.utils
  (:use-macros [dommy.macros :only [sel1 sel node]])
  (:require [dommy.core :refer [listen! add-class! remove-class!]]))

(defn listen-all!
  [elems event f]
  (doseq [e elems]
    (listen! e event f)))

(defn unflash!
  [elem class]
  (js/setTimeout (fn [] (when elem (remove-class! elem class))) 50))

(defn flash!
  [elem class]
  (unflash! elem class)
  (add-class! elem class))

(defn spawn-after!
  [millis f]
  (js/setTimeout f millis))
