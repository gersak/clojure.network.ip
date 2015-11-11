(ns clojure.network.l3.ip.Network
  (:require [clojure.network.l3.ip.IPAddress :refer [normalize-ip-bytes]])
  (:import [java.net InetAddress Inet4Address Inet6Address]
           [clojure.network.l3.ip IPAddress]
           [java.lang UnsupportedOperationException IndexOutOfBoundsException])
  (:gen-class :init init
              :prefix network-
              :implements [clojure.lang.ISeq clojure.lang.Counted clojure.lang.IPersistentSet]
              :state state
              :methods [[ipAddress [] clojure.network.l3.ip.IPAddress]]
              :constructors {[String] []
                             [String String] []
                             [String Number] []}))

(defn- get-mark [#^bytes ip-address ^Integer subnet]
  (if (<= (count ip-address) 4) (- 32 subnet) (- 128 subnet)))

(defn- subnet-count [x]
  "Returns set bits in byte"
  (let [testers (range 7 -1 -1)
        test! (map #(bit-test x %) testers)
        subnet-bits (count (take-while true? test!))
        all-bits (count (filter true? test!))]
    (if (not= subnet-bits all-bits)
      (throw (Exception. (str "Subnet part: " x " is not valid subnet!")))
      subnet-bits)))

(defmulti get-network-address (fn [ip subnet] (class (.state ip))))

(defmethod get-network-address java.net.Inet4Address [ip-address subnet]
  (let [mark (get-mark (.toByteArray ip-address) subnet)
        network-address-bytes (->
                                (.shiftRight ip-address mark)
                                (.shiftLeft mark)
                                (.toByteArray)
                                (normalize-ip-bytes 4))]
    (IPAddress. network-address-bytes)))

(defmethod get-network-address java.net.Inet6Address [ip-address subnet]
  (let [mark (get-mark (.toByteArray ip-address) subnet)
        network-address-bytes (->
                                (.shiftRight ip-address mark)
                                (.shiftLeft mark)
                                (.toByteArray)
                                (normalize-ip-bytes 16))]
    (IPAddress. network-address-bytes)))

(def ^:private IPv4_MAX_ADDRESS
  (IPAddress.
    (normalize-ip-bytes
      (.toByteArray
        (BigInteger/valueOf (dec (clojure.math.numeric-tower/expt 2 32)))) 4)))


(def ^:private IPv6_MAX_ADDRESS
  (IPAddress.
    (normalize-ip-bytes
      (.toByteArray
        (biginteger (dec (clojure.math.numeric-tower/expt 2 128)))) 16)))

(defmulti get-max-address (fn [ip subnet] (class (.state ip))))

(defmethod get-max-address java.net.Inet4Address [ip-address subnet]
  (let [mark (get-mark (.toByteArray ip-address) subnet)
        mask (BigInteger/valueOf (dec (bit-shift-left 1 mark)))
        diff-seq (.toByteArray (.or mask ip-address))]
    (if (= (list (byte -1)) (seq diff-seq))
      IPv4_MAX_ADDRESS
      (IPAddress. (normalize-ip-bytes diff-seq 4)))))

(defmethod get-max-address java.net.Inet6Address [ip-address subnet]
  (let [mark (get-mark (.toByteArray ip-address) subnet)
        mask (BigInteger/valueOf (dec (bit-shift-left 1 mark)))
        diff-seq (.toByteArray (.or mask ip-address))]
    (if (= (list (byte -1)) (seq diff-seq))
      IPv6_MAX_ADDRESS
      (IPAddress. (normalize-ip-bytes diff-seq 16)))))

(defn next-address-iterator [min-address max-address]
  (fn [x]
    (when (and (>= x min-address) (< x max-address))
      (IPAddress. (.toByteArray (biginteger (.add x BigInteger/ONE)))))))

(defn network-init
  ([network]
   (assert (string? network) "Argument not of String type...")
   (let [[ip subnet] (clojure.string/split network #"/")
         subnet (Integer. subnet)
         ip (IPAddress. ip)]
     [[] {:ip ip
          :subnet subnet}]))
  ([ip-address subnet]
   (if (string? subnet)
     (do
       (assert (seq (re-find #"\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" subnet)) (str "Subnet: " subnet " is not valid!"))
       (let [subnet (if (string? subnet)
                      (reduce clojure.core/+ (map subnet-count (map read-string (clojure.string/split subnet #"\."))))
                      subnet)]
         [[] {:ip (IPAddress. ip-address)
              :subnet subnet}]))
     (do
       (let [ip (IPAddress. ip-address)]
         (if (instance? Inet4Address (.state ip))
           (assert (and (<= subnet 32) (> subnet 0)) (str "Subnet: " subnet " is not valid!"))
           (assert (and (<= subnet 128) (> subnet 0)) (str "Subnet: " subnet " is not valid!")))
         [[] {:ip (IPAddress. ip-address)
              :subnet subnet}])))))

(defn network-toString [this]
  (str (.getHostAddress (.state (:ip (.state this)))) "/" ((comp str :subnet ) (.state this))))

(defn address-range [network]
  (let [{ip :ip subnet :subnet} (.state network)]
    [(get-network-address ip subnet)
     (get-max-address ip subnet)]))

;; ISeq
(defn network-seq [this]
  (when-let [[min-address max-address] (address-range this)]
    (lazy-seq (take-while (comp not nil?) (iterate (next-address-iterator min-address max-address) min-address)))))

(defn network-ipAddress [this]
  (:ip (.state this)))

;; IPersistentCollection
(defn network-count [this]
  (when-let [[min-address max-address] (address-range this)]
    (let [r (inc (biginteger (- max-address min-address)))
          r (if (zero? r)
              (case (.version (network-ipAddress this))
                "ipv4" (clojure.math.numeric-tower/expt 2 32)
                "ipv6" (clojure.math.numeric-tower/expt 2 128))
              r)]
      (if (> r Integer/MAX_VALUE)
        (throw (Exception. "Network larger than Integer/MAX_VALUE. count not supported"))))))

(defn network-cons [this o]
  (throw (UnsupportedOperationException. "Network definition complete and persistent. Does not support cons!")))

(defn network-empty [this] '())

(defn network-equiv [this n] (= (address-range this) (address-range n)))

(defn network-first [this] (first (address-range this)))

(defn network-more [this]
  (rest (network-seq this)))

(def network-next network-more)

;; IPersistentSet
(defn network-disjoin [this key] (throw (UnsupportedOperationException. "Addresses cannot be disjoined from network!")))

(defn network-contains [this address]
  (let [address (cond
                  (string? address) (IPAddress. address)
                  (instance? IPAddress address) address)
        [min-address max-address] (address-range this)]
    (and (<= min-address address) (>= max-address address))))

(defn network-get [this #^Integer address-number]
  (let [target-address (IPAddress. (.toByteArray (.add (biginteger (first this)) (biginteger address-number))))]
    (if (network-contains this target-address)
      target-address
      (throw (IndexOutOfBoundsException. (str "IP address: " target-address " is not in network " this))))))
