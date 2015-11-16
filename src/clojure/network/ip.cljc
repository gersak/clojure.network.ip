(ns clojure.network.ip
  #?(:clj
      (:refer-clojure :exclude [first more cons]))
  #?(:clj
      (:import
        [java.math.BigInteger]
        [clojure.lang ISeq Counted IPersistentSet]
        [java.net InetAddress Inet4Address Inet6Address]
        [java.lang UnsupportedOperationException])
    :cljs
     (:require [goog.net.IpAddress :as ip]
               [goog.math.Integer :as i])))

(defprotocol IPConstructor
  (make-ip-address [this]))

(defprotocol IPInfo
  (ip-address [this])
  (version [this])
  (numeric-value [this]))

(defprotocol NetworkInfo
  (network-mask [this]))


#?(:cljs
    (defn- bits->ip [bits]
      (if (= 1 (count bits))
        (goog.net.Ipv4Address. (goog.math.Integer. bits 0))
        (goog.net.Ipv6Address. (goog.math.Integer. bits 0)))))

(deftype IPAddress [value]
  IPInfo
  (ip-address [this]
    (str this))
  (version [this]
    #?(:clj
        (cond
          (instance? Inet4Address (InetAddress/getByAddress value)) 4
          (instance? Inet6Address (InetAddress/getByAddress value)) 6)
       :cljs
        (.getVersion (bits->ip value))))
  (numeric-value [this]
    #?(:clj (BigInteger. value)
       :cljs (.toNumber (goog.math.Integer. value 0))))
  IPConstructor
  (make-ip-address [this] this)
  #?@(:clj [clojure.lang.IHashEq
            hasheq [this] (numeric-value this)]
     :cljs [IEquiv
            (-equiv [this other]
                    (= (numeric-value this) (numeric-value other)))])
  Object
  (equals [this other] (= (numeric-value this) (numeric-value other)))
  (hashCode [this] (numeric-value this))
  (toString [this]
    #?(:clj (.getHostAddress (InetAddress/getByAddress value))
       :cljs (.toString (bits->ip value)))))



#?(:clj
    (extend-type String
      IPConstructor
      (make-ip-address [this]
        (->IPAddress
          (.getAddress (InetAddress/getByName this)))))
  :cljs
    (extend-type string
      IPConstructor
      (make-ip-address [this]
        (->IPAddress (.-bits_ (.toInteger (ip/fromString this)))))))

#?(:clj
    (extend-type (Class/forName "[B")
      IPConstructor
      (make-ip-address [this]
        (->IPAddress this))))

#?(:clj
    (extend-type BigInteger
        IPConstructor
        (make-ip-address [this]
          (->IPAddress (.toByteArray this))))
   :cljs
    (extend-type goog.math.Integer
      IPConstructor
      (make-ip-address [this]
        (->IPAddress (.-bits_ this)))))

#?(:clj
    (extend-type clojure.lang.BigInt
      IPConstructor
      (make-ip-address [this]
        (->IPAddress (.toByteArray (biginteger this)))))
   :cljs
    (extend-type number
      IPConstructor
      (make-ip-address [this]
        (->IPAddress (.-bits_ (i/fromNumber this))))))

#?(:clj
    (defmethod print-method IPAddress [record writter]
        (.write writter
                (str "IP Address: " (str record) \newline
                     "Version: " (version record) \newline))))


(defn- get-network-address [ip subnet]
  (let [ip (make-ip-address ip)]
    (make-ip-address
      (reduce
        #?(:clj (fn [n bit] (.clearBit n bit))
                :cljs (fn [n bit] (bit-clear n bit)))
        (numeric-value ip)
        (case (version ip)
          4 (range (clojure.core/- 32 subnet))
          6 (range (clojure.core/- 128 subnet)))))))

(defn- get-broadcast-address [ip subnet]
  (let [ip (make-ip-address ip)]
    (make-ip-address
      (reduce
        #?(:clj (fn [n bit] (.setBit n bit))
                :cljs (fn [n bit] (bit-set n bit)))
        (numeric-value ip)
        (case  (version ip)
          4 (range (clojure.core/- 32 subnet))
          6 (range (clojure.core/- 128 subnet)))))))


(defn- get-all-addresses [ip subnet]
  (let [min-address (numeric-value (get-network-address ip subnet))
        max-address #?(:clj
                        (.add (numeric-value (get-broadcast-address ip subnet)) BigInteger/ONE)
                       :cljs (inc (numeric-value (get-broadcast-address ip subnet))))]
    (map make-ip-address (range min-address max-address))))


(def test-ipv6 "2a00:c31:1ffe:fff::9:13")
(def test-ipv4 "192.168.250.111")

(deftype Network [ip mask]
  IPInfo
  (ip-address [_] ip)
  (version [_] (version ip))
  (numeric-value [_] (numeric-value ip))
  NetworkInfo
  (network-mask [_] mask)
  #?@(:clj
       [ISeq
        (first [this] (get-network-address ip mask))
        (next [this] (next (get-all-addresses ip mask)))
        (more [this] (rest (get-all-addresses ip mask)))
        (count [this]
               (let [min-value (numeric-value (get-network-address ip mask))
                     max-value (numeric-value (get-broadcast-address ip mask))]
                 ;; TODO this function is not workin as meant to.
                 ;; There is problem with returning value for whom
                 ;; count coerces return value to long.
                 ;; Point is no more thant 2^32 can be counted
                 ;; BIG FAIL for... clojure
                 #?(:clj (inc (.subtract max-value min-value)))))
        clojure.lang.Seqable
        (seq [this] (get-all-addresses ip mask))
        clojure.lang.IPersistentSet
        (disjoin [this _] #?(:clj (throw (Exception. "Network can't disjoin IP Addresses."))))
        (contains [this ip]
                  (let [value (-> ip make-ip-address numeric-value)]
                    (and (<= value (numeric-value (get-broadcast-address ip mask))) (>= value (numeric-value (get-network-address ip mask))))))
        (get [this address-number]
             #?(:clj
                 (let [target-address (make-ip-address
                                        (.add
                                          (numeric-value (get-network-address ip mask))
                                          (biginteger address-number)))]
                   (if (.contains this target-address)
                     target-address
                     (throw (IndexOutOfBoundsException. (str "IP address: " target-address " is not in network " this)))))))
        clojure.lang.Indexed
        (nth [this seq-number] (nth this seq-number nil))
        (nth [this seq-number not-found]
             (let [ip (make-ip-address ip)
                   max-address (-> ip
                                   (get-broadcast-address mask)
                                   numeric-value)
                   network-address (-> ip (get-network-address mask) numeric-value)
                   nth-address (+ network-address seq-number)]
               (if (<= nth-address max-address)
                 (make-ip-address nth-address)
                 not-found)))]
       :cljs
       [cljs.core/ISeq
        (-first [this] (get-network-address ip mask))
        (-rest [this] (rest (get-all-addresses ip mask)))
        cljs.core/ISeqable
        (-seq [this] (get-all-addresses ip mask))
        cljs.core/ILookup
        (-lookup [this target-ip]
                 (-lookup this target-ip nil))
        (-lookup [this target-ip default-val]
                 (let [value (-> target-ip make-ip-address)
                       max-address (get-broadcast-address ip mask)
                       min-address (get-network-address ip mask)]
                   (println (str value) (str max-address) (str min-address))
                   (if (and (<= (numeric-value value) (numeric-value max-address)) (>= (numeric-value value) (numeric-value min-address)))
                     value
                     default-val)))
        clojure.core/IIndexed
        (-nth [this seq-numb] (-nth this seq-numb nil))
        (-nth [this seq-numb not-found]
              (let [ip (make-ip-address ip)
                    max-address (-> ip
                                    (get-broadcast-address mask)
                                    numeric-value)
                    network-address (-> ip (get-network-address mask) numeric-value)
                    nth-address (+ network-address seq-numb)]
                (if (<= nth-address max-address)
                  (make-ip-address nth-address)
                  not-found)))])
      Object
      (toString [this] (str ip "/" mask)))


(defn make-network
      ([network] (do
                   (assert (string? network) "Input network has to be of string type in CIDR notation.")
                   (assert (re-find #"/" network) "Input network has to be of string type in CIDR notation.")
                   (apply make-network (clojure.string/split network #"/"))))
      ([ip-address subnet]
       (when-let [ip-address (make-ip-address ip-address)]
         (case (version ip-address)
           4 (cond
                  (string? subnet) (if (re-find #"^\d+$" subnet)
                                     (when-let [subnet #?(:clj
                                                           (try (Integer/parseInt subnet) (catch Exception e nil))
                                                           :cljs
                                                           (try (js/parseInt subnet) (catch js/Error e nil)))]
                                       (do
                                         (println subnet)
                                         (assert (and (<= subnet 32) (>= subnet 0)) (str "Subnet " subnet " is out of range."))
                                         (->Network ip-address subnet)))
                                     (do
                                       (assert (seq (re-find #"\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" subnet)) (str "Subnet: " subnet " is not valid!"))
                                       (let [subnet-count (fn subnet-count [x]
                                                            (let [testers (range 7 -1 -1)
                                                                  test! (map #(bit-test x %) testers)
                                                                  subnet-bits (count (take-while true? test!))
                                                                  all-bits (count (filter true? test!))]
                                                              (if (not= subnet-bits all-bits)
                                                                (throw
                                                                  #?(:clj (Exception. (str "Subnet part: " x " is not valid subnet!"))))
                                                                subnet-bits)))
                                             subnet (reduce
                                                      clojure.core/+
                                                      (map subnet-count
                                                           (map #?(:clj #(Integer/parseInt %)
                                                                   :cljs (partial js/parseInt)) (clojure.string/split subnet #"\."))))]
                                         (->Network ip-address subnet))))
                  (number? subnet) (do
                                     (assert (and (<= subnet 32) (>= subnet 0)) (str "Subnet " subnet " is out of range."))
                                     (->Network ip-address subnet))
                  :else #?(:clj (throw (UnsupportedOperationException. (str "Don't recongize subnet " (str subnet))))
                           :cljs (throw (js/Error. (str "Don't recongize subnet " (str subnet))))))
           6 (cond
                  (string? subnet) (if-let [subnet #?(:clj
                                                       (try (Integer/parseInt subnet) (catch Exception e nil))
                                                       :cljs
                                                       (try (js/parseInt subnet) (catch js/Error e nil)))]
                                     (->Network ip-address subnet)
                                     #?(:clj (throw (Exception. (str "Can't make subnet from: " subnet)))
                                        :cljs (throw (js/Error. (str "Can't make subnet from: " subnet)))))
                  (number? subnet) (->Network ip-address subnet)
                  :else #?(:clj (throw (UnsupportedOperationException. (str "Don't recongize subnet " (str subnet))))
                           :cljs (throw (js/Error. (str "Don't recongize subnet " (str subnet))))))))))

#?(:clj
    (defmethod print-method Network [record writter]
      (.write writter
              (let [ip (.ip record)
                    subnet (.mask record)]
                (str "Network: " ip \/  subnet \newline
                     "Network address: " (get-network-address ip subnet) \newline
                     "Broadcast address: " (get-broadcast-address ip subnet) \newline
                     "Address count: " (count record)
                     \newline)))))



;; NETWORK UTILS
(def IPv4_parts (take 33 (iterate #(* 2 %) 1)))
(def IPv6_parts (take 129 (iterate #(* 2 %) 1)))


(defn- get-position [coll element]
  (first (keep-indexed (fn [i e] (if (= e element) i)) coll)))

(defn devide-network
  "Given input network, devides network on specified
  parts. If input arg parts is i.e. 2 creates 2 smaller networks
  from input network. If it were 4, than 4 networks would be created.

  There are limits. If v4 network is used than IPv4_parts are valid
  parts argument and for v6 IPv6_parts are valid arguments."
  [network parts]
  (assert (not (zero? parts)) "Cannot divide network on 0 parts.")
  (let [ip (ip-address network)
        network-address (first network)
        delta-mask (case (version network-address)
                     4 (get-position IPv4_parts parts)
                     6 (get-position IPv6_parts parts))
        mask (network-mask network)
        new-mask (+ mask delta-mask)]
    (assert delta-mask (str "Wrong part operator: " parts \newline
                            "Use IPv4: " (clojure.string/join \space IPv4_parts) \newline
                            "Use IPv6: " (clojure.string/join \space IPv6_parts) \newline))
    (case (version network-address)
      4 (assert (<= new-mask 32) "Cannot devide network on this much segments.")
      6 (assert (<= new-mask 128) "Cannot devide network on this much segments."))
    (let [new-network (make-network ip new-mask)
          delta (count new-network)
          offset (get-position (seq new-network) ip)]
    (println offset)
      (map #(make-network (make-ip-address (+ offset (* % delta) (numeric-value network-address))) new-mask) (range parts)))))


(defn possible-break-count?
  "Returns sequence of valid arguments for network
  breaking."
  [mask version]
  (let [mask-index (case version
                     4 (- 32 mask)
                     6 (- 128 mask))]
    (case version
      4 (take-while (partial not= (nth IPv4_parts mask-index)) IPv4_parts)
      6 (take-while (partial not= (nth IPv6_parts mask-index)) IPv6_parts))))

(defn break-network
  "Given input network, breaks current network on
  smaller networks with address count as second argument."
  [network address-count]
  (case (version network)
    4 (assert (get-position IPv4_parts address-count) (apply str "Use: " IPv4_parts))
    6 (assert (get-position IPv6_parts address-count) (apply str "Use: " IPv6_parts)))
  (let [mask (network-mask network)
        mask-index (case (version network)
                     4 (- 32 mask)
                     6 (- 128 mask))
        address-count-index (case (version network)
                              4 (get-position IPv4_parts address-count)
                              6 (get-position IPv6_parts address-count))]
    (assert (< address-count-index mask-index)
            (apply str "Cannot break network on more networks than there are subnets. Use some of:\n"
                   (clojure.string/join \space (possible-break-count? mask (version network)))))
    (devide-network
      network
      (case (version network)
        4 (nth IPv4_parts (- mask-index address-count-index))
        6 (nth IPv6_parts (- mask-index address-count-index))))))
