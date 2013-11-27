(ns query-manager.utils
  ;;
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]))

(defn listen-all!
  [elems event f]
  (doseq [e elems]
    (dom/listen! e event f)))

(defn unflash!
  [elem class]
  (js/setTimeout (fn [] (when elem (dom/remove-class! elem class))) 50))

(defn flash!
  [elem class]
  (unflash! elem class)
  (dom/add-class! elem class))

(defn spawn-after!
  [millis f]
  (js/setTimeout f millis))

(defn das
  [part date]
  (let [num (case part
              :month (inc (.getMonth date))
              :day (.getDate date)
              :hour (.getHours date)
              :minute (.getMinutes date)
              :second (.getSeconds date)
              :year (.getFullYear date)
              0)]
    (if (< num 10)
      (str "0" num)
      (str num))))

(defn subscriber-ch
  [& topics]
  (let [ch (async/chan)
        topics (set topics)]
    (async/filter> (fn [value] (contains? topics (first value))) ch)))
