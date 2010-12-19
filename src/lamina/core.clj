;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns
  ^{:author "Zachary Tellman"}
  lamina.core
  (:use
    [potemkin :only (import-fn)])
  (:require
    [lamina.core.pipeline :as pipeline]
    [lamina.core.channel :as channel]
    [lamina.core.seq :as seq])
  (:import))


;;;; CHANNELS

;; core channel functions
(import-fn channel/receive)
(import-fn channel/cancel-callback)
(import-fn channel/enqueue)
(import-fn channel/enqueue-and-close)
(import-fn channel/close)
(import-fn channel/on-close)
(import-fn channel/on-sealed)
(import-fn channel/sealed?)
(import-fn channel/closed?)
(import-fn channel/channel?)
(import-fn seq/receive-all)
(import-fn channel/poll)

;; channel variants
(import-fn channel/splice)
(import-fn channel/channel)
(import-fn channel/channel-pair)
(import-fn channel/constant-channel)
(import-fn channel/sealed-channel)

(def nil-channel channel/nil-channel)

;; channel utility functions

(import-fn seq/siphon)
(import-fn seq/fork)
(import-fn seq/map*)
(import-fn seq/filter*)
(import-fn seq/receive-in-order)
(import-fn seq/reduce*)
(import-fn seq/reductions*)
;;(import-fn channel/take*)
;;(import-fn channel/take-while*)

;; named channels
;;(import-fn channel/named-channel)
;;(import-fn channel/release-named-channel)

;; synchronous channel functions
(import-fn seq/lazy-channel-seq)
(import-fn seq/channel-seq)
(import-fn seq/wait-for-message)


;;;; PIPELINES

;; core pipeline functions
(import-fn pipeline/result-channel)
(import-fn pipeline/pipeline)
(import-fn pipeline/run-pipeline)

;; pipeline stage helpers
(import-fn pipeline/read-channel)
(import-fn pipeline/read-merge)
(import-fn pipeline/blocking)

;; redirect signals
(import-fn pipeline/redirect)
(import-fn pipeline/restart)
(import-fn pipeline/complete)

;; pipeline result hooks
(import-fn pipeline/wait-for-result)

;;;