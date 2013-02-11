;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns lamina.trace.router
  (:use
    [potemkin]
    [lamina.core]
    [clojure.walk :only (postwalk)]
    [lamina.trace.probe :only (select-probes)])
  (:require
    [lamina.time :as time]
    [lamina.cache :as cache]
    [lamina.trace.router
     [core :as c]
     [parse :as p]
     [operators :as o]]))

(defn- stringify-keys [x]
  (postwalk
    (fn [x]
      (if (map? x)
        (zipmap
          (map #(if (keyword? %) (name %) (str %)) (keys x))
          (vals x))
        x))
    x))

(defn parse-descriptor [x & {:as options}]
  (let [descriptor (if (string? x)
                     (stringify-keys (p/parse-stream x))
                     x)
        descriptor (if-let [period (:period options)]
                     (assoc-in descriptor ["__implicit" "period"] period)
                     descriptor)]
    descriptor))

(defn query-stream
  "something goes here"
  [transform-descriptor ch &
   {:keys [task-queue]
    :or {task-queue (time/task-queue)}
    :as options}]
  (time/with-task-queue task-queue
    (c/transform-trace-stream
      (apply parse-descriptor transform-descriptor (apply concat options))
      ch)))

(defn query-seq
  "something goes here"
  [transform-descriptor s &
   {:keys [timestamp
           payload
           period]
    :or {payload identity}
    :as options}]
  (assert timestamp)
  (let [start (timestamp (first s))
        q (time/non-realtime-task-queue start false)
        ch (channel)
        ch* (apply query-stream transform-descriptor ch
              (apply concat
                (assoc options :task-queue q)))]
    (future

      (loop [s s, current-time start]
        (if (empty? s)
          (when period
            (time/advance-until q (+ current-time (* 2 period))))
          (let [t (first s)]
            (time/advance-until q (timestamp t))
            (enqueue ch (payload t))
            (recur (rest s) (timestamp t)))))

      (close ch)
      (close ch*))

    (->> ch*
      (map* #(hash-map :timestamp (time/now q) :value %))
      channel->lazy-seq)))

;;;

(import-macro c/def-trace-operator)

(defn trace-router
  "something goes here"
  [{:keys [generator
           topic->id
           on-subscribe
           on-unsubscribe
           timestamp
           payload
           task-queue]
    :or {payload identity
         task-queue (time/task-queue)}
    :as options}]

  (let [generator (if timestamp
                    #(->> %
                       generator
                       (map* identity)
                       (defer-onto-queue task-queue timestamp)
                       (map* payload))
                    generator)

        topic-fn (fn [this {:strs [operators name] :as topic}]
                   
                   ;; is it a chain of operators?
                   (when (and (not name) (not (empty? operators)))
                     {:cache this
                      :topic (update-in topic ["operators"] butlast)
                      :transform #(c/transform-trace-stream
                                    (update-in topic ["operators"] (comp list last)) %)}))
        router (cache/router
                 (assoc options
                   :generator generator
                   :cache+topic->topic-descriptor topic-fn))]

    (reify cache/IRouter
      (inner-cache [_]
        (cache/inner-cache router))
      (subscribe- [this topic args]
        (let [options (apply hash-map args)
              period (:period options)]
          (time/with-task-queue task-queue
            (binding [c/*stream-generator* (or c/*stream-generator* #(cache/subscribe this %))]
              (cache/subscribe router
                (apply parse-descriptor topic args)))))))))

(def
  ^{:doc "something goes here"}
  local-trace-router
  (trace-router
    {:generator (fn [{:strs [pattern]}]
                  (select-probes pattern))}))

(defn aggregating-trace-router
  "something goes here"
  [endpoint-router]
  (let [router (trace-router
                 {:generator
                  (fn [{:strs [endpoint]}]
                    (cache/subscribe endpoint-router endpoint))})]
    (reify cache/IRouter
      (inner-cache [_]
        (cache/inner-cache router))
      (subscribe- [this topic args]
        (let [options (apply hash-map args)
              {:strs [operators] :as topic} (apply parse-descriptor topic args)
              period (options :period)
              distributable (assoc topic
                              "operators" (c/distributable-chain operators))
              non-distributable (assoc topic
                                  "endpoint" distributable
                                  "operators" (c/non-distributable-chain operators))]
          (binding [c/*stream-generator* (or c/*stream-generator* #(cache/subscribe this %))]
            (cache/subscribe router non-distributable)))))))

