(ns clojure.network.l3.ip.IPAddress
  (:require [clojure.math.numeric-tower :as math])
  (:import [java.math.BigInteger]
           [java.net InetAddress Inet4Address Inet6Address])
  (:gen-class
    :extends java.math.BigInteger
    :init init
    :exposes-methods {toByteArray ptoByteArray}
    :prefix ip-
    :state state
    :methods  [[version [] String]
               [hostname [] String]]
    :constructors {[String] ["[B"]
                   ["[B"] ["[B"]}))


(defn ip-toString [this]
  (.getHostAddress (.state this)))

(defmulti ip-toByteArray (fn [this] (class (.state this))))

(defn normalize-ip-bytes [#^"[B" ba #^Integer required-size]
  (if (= (count ba) required-size) ba
    (let [new-ba (byte-array required-size)
          ba (if (< required-size (count ba))
               (to-array (drop (- (count ba) required-size) ba))
               ba)
          distance (- required-size (count ba))
          set-bytes (range (min (count ba) required-size))]
      (doseq [x set-bytes]
        (aset new-ba (+ x distance) (get ba x)))
      new-ba)))

(defmethod ip-toByteArray java.net.Inet4Address [this]
  (normalize-ip-bytes (.ptoByteArray this) 4))

(defmethod ip-toByteArray java.net.Inet6Address [this]
  (normalize-ip-bytes (.ptoByteArray this) 16))

(defn ip-version [this]
  (cond
    (instance? Inet4Address (.state this)) "v4"
    (instance? Inet6Address (.state this)) "v6"))

(defn ip-hostname [this]
  (.. this state getHostName))

(defprotocol IPConstructor
  (ip-init [this]))

(extend-type String
  IPConstructor
  (ip-init [this]
    (let [ip (InetAddress/getByName this)]
      [[(.getAddress ip)] ip])))

(extend-type (Class/forName "[B")
  IPConstructor
  (ip-init [this]
    [[this] (InetAddress/getByAddress this)]))
