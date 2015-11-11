(ns clojure.network.ip
  (:refer-clojure :exclude [+ - inc dec first more next cons count])
  #?(:clj
      (:import
        [java.math.BigInteger]
        [clojure.lang ISeq Counted IPersistentSet]
        [java.net InetAddress Inet4Address Inet6Address]
        [java.lang UnsupportedOperationException])))

(defprotocol IPConstructor
  (make-ip-address [this]))

(defprotocol IPInfo
  (ip-address [this])
  (host [this])
  (version [this])
  (numeric-value [this]))

(defrecord IPAddress [value]
  IPInfo
  (ip-address [this]
    (str this))
  (host [this]
    (.getHostName (InetAddress/getByAddress value)))
  (version [this]
    (cond
      (instance? Inet4Address (InetAddress/getByAddress value)) "v4"
      (instance? Inet6Address (InetAddress/getByAddress value)) "v6"))
  (numeric-value [this]
    (BigInteger. value))
  IPConstructor
  (make-ip-address [this] this)
  Object
  (toString [this]
    (.getHostAddress (InetAddress/getByAddress value))))



(extend-type String
  IPConstructor
  (make-ip-address [this]
    (->IPAddress (.getAddress (InetAddress/getByName this)))))

(extend-type (Class/forName "[B")
  IPConstructor
  (make-ip-address [this]
    (->IPAddress this)))

(extend-type BigInteger
  IPConstructor
  (make-ip-address [this]
    (->IPAddress (.toByteArray this))))

(extend-type clojure.lang.BigInt
  IPConstructor
  (make-ip-address [this]
    (->IPAddress (.toByteArray (biginteger this)))))

(defmethod print-method IPAddress [record writter]
  (.write writter
          (str "IP Address: " (str record) \newline
               "Version: " (version record) \newline)))

;(defn get-network-address [{ip :ip-address subnet :mask}]
;  (case  (version ip)
;    "v4" (make-ip-address (reduce (fn [n bit] (.clearBit n bit)) (numeric-value ip) (range (clojure.core/- 32 subnet))))
;    "v6" (make-ip-address (reduce (fn [n bit] (.clearBit n bit)) (numeric-value ip) (range (clojure.core/- 128 subnet))))))
;
;(defn get-broadcast-address [{ip :ip-address subnet :mask}]
;  (case  (version ip)
;    "v4" (make-ip-address (reduce (fn [n bit] (.setBit n bit)) (numeric-value ip) (range (clojure.core/- 32 subnet))))
;    "v6" (make-ip-address (reduce (fn [n bit] (.setBit n bit)) (numeric-value ip) (range (clojure.core/- 128 subnet))))))
;
;
;(defn get-all-addresses [{ip :ip-address subnet :mask :as network}]
;  (let [min-address (numeric-value (get-network-address network))
;        max-address (.add (numeric-value (get-broadcast-address network)) BigInteger/ONE)]
;    (map make-ip-address (range min-address max-address))))

(defn- get-network-address [ip subnet]
  (case  (version ip)
    "v4" (make-ip-address (reduce (fn [n bit] (.clearBit n bit)) (numeric-value ip) (range (clojure.core/- 32 subnet))))
    "v6" (make-ip-address (reduce (fn [n bit] (.clearBit n bit)) (numeric-value ip) (range (clojure.core/- 128 subnet))))))

(defn- get-broadcast-address [ip subnet]
  (case  (version ip)
    "v4" (make-ip-address (reduce (fn [n bit] (.setBit n bit)) (numeric-value ip) (range (clojure.core/- 32 subnet))))
    "v6" (make-ip-address (reduce (fn [n bit] (.setBit n bit)) (numeric-value ip) (range (clojure.core/- 128 subnet))))))


(defn- get-all-addresses [ip subnet]
  (let [min-address (numeric-value (get-network-address ip subnet))
        max-address (.add (numeric-value (get-broadcast-address ip subnet)) BigInteger/ONE)]
    (map make-ip-address (range min-address max-address))))






(def test-ipv6 "2a00:c31:1ffe:fff::9:13")
(def test-ipv4 "192.168.250.111")


(deftype Network [ip-address mask]
  IPInfo
  (ip-address [_] ip-address)
  (host [_] (host ip-address))
  (version [_] (version ip-address))
  (numeric-value [_] (numeric-value ip-address))
  ISeq
  (first [this] (get-network-address ip-address mask))
  (next [this] (next (get-all-addresses ip-address mask)))
  (more [this] (rest (get-all-addresses ip-address mask)))
  (count [this]
    (long (.subtract
            (numeric-value (get-broadcast-address ip-address mask))
            (numeric-value (get-network-address ip-address mask)))))
  clojure.lang.Seqable
  (seq [this] (get-all-addresses ip-address mask))
  clojure.lang.IPersistentSet
  (disjoin [this _] (throw (Exception. "Network can't disjoin IP Addresses.")))
  (contains [this ip-address]
    (let [value (-> ip-address make-ip-address numeric-value)]
      (and (<= value (numeric-value (get-broadcast-address ip-address mask))) (>= value (numeric-value (get-network-address ip-address mask))))))
  (get [this address-number]
    (let [target-address (make-ip-address
                           (.add
                             (numeric-value (get-network-address ip-address mask))
                             (biginteger address-number)))]
      (if (.contains this target-address)
        target-address
        (throw (IndexOutOfBoundsException. (str "IP address: " target-address " is not in network " this))))))
  Object
  (toString [this] (str ip-address "/" mask)))

;(defn- make-network-reify [ip-address subnet]
;  (let [network {:ip-address ip-address :mask subnet}
;        min-address (get-network-address network)
;        max-address (get-broadcast-address network)
;        all-addresses (get-all-addresses network)]
;    (reify
;      IPInfo
;      (ip-address [_] ip-address)
;      (host [_] (host ip-address))
;      (version [_] (version ip-address))
;      (numeric-value [_] (numeric-value ip-address))
;      ISeq
;      (first [this] min-address)
;      (next [this] (next all-addresses))
;      (more [this] (rest all-addresses))
;      (count [this]
;        (long (.subtract (numeric-value max-address) (numeric-value min-address))))
;      clojure.lang.Seqable
;      (seq [this] all-addresses)
;      clojure.lang.IPersistentSet
;      (disjoin [this _] (throw (Exception. "Network can't disjoin IP Addresses.")))
;      (contains [this ip-address]
;        (let [value (-> ip-address make-ip-address numeric-value)]
;          (and (<= value (numeric-value max-address)) (>= value (numeric-value min-address)))))
;      (get [this address-number]
;        (let [target-address (make-ip-address (.add (numeric-value min-address) (biginteger address-number)))]
;          (if (.contains this target-address)
;            target-address
;            (throw (IndexOutOfBoundsException. (str "IP address: " target-address " is not in network " this))))))
;      Object
;      (toString [this] (str ip-address "/" subnet)))))


(defn make-network
  ([^String network] (apply make-network (clojure.string/split network #"/")))
  ([ip-address subnet]
   (let [ip-address (make-ip-address ip-address)]
     (if (= "v4" (version ip-address))
       (cond
         (string? subnet) (if-let [subnet (try (Integer/parseInt subnet) (catch Exception e nil))]
                            (->Network ip-address subnet)
                            (do
                              (assert (seq (re-find #"\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" subnet)) (str "Subnet: " subnet " is not valid!"))
                              (let [subnet-count (fn subnet-count [x]
                                                   (let [testers (range 7 -1 -1)
                                                         test! (map #(bit-test x %) testers)
                                                         subnet-bits (count (take-while true? test!))
                                                         all-bits (count (filter true? test!))]
                                                     (if (not= subnet-bits all-bits)
                                                       (throw (Exception. (str "Subnet part: " x " is not valid subnet!")))
                                                       subnet-bits)))
                                    subnet (reduce clojure.core/+ (map subnet-count (map read-string (clojure.string/split subnet #"\."))))])
                              (->Network ip-address subnet)))
         (number? subnet) (->Network ip-address subnet)
         :else (throw (UnsupportedOperationException. (str "Don't recongize subnet " (str subnet)))))))))



(defmethod print-method Network [record writter]
  (.write writter
          (let [ip (.ip-address record)
                subnet (.mask record)]
            (str "Network: " ip \/  subnet \newline
                 "Network address: " (get-network-address ip subnet) \newline
                 "Broadcast address: " (get-broadcast-address ip subnet) \newline
                 "Address count: " (count record)
                 \newline))))

;; OLD CODE

(defn- num2ip [#^Number x]
  (IPAddress. (.toByteArray (biginteger x))))

(defn +
  ([] nil)
  ([x] x)
  ([x y] (num2ip (.add (biginteger x) (biginteger y))))
  ([x y & more]
   (num2ip (reduce #(.add %1 (biginteger %2)) (.add (biginteger x) (biginteger y)) more))))

(defn inc
  [x]
  (num2ip (.add x BigInteger/ONE)))

(defn dec
  [x]
  (num2ip (.subtract x BigInteger/ONE)))

(defn -
  ([] nil)
  ([x] x)
  ([x y] (num2ip (.subtract (biginteger x) (biginteger y))))
  ([x y & more]
   (num2ip (reduce #(.subtract %1 (biginteger %2)) (.subtract (biginteger x) (biginteger y)) more))))
