(ns clojure.network.l3.ip
  (:import [java.net InetAddress Inet4Address Inet6Address]
           [org.apache.commons.net.util SubnetUtils]
           [java.math BigInteger])
  (:require [clojure.core.reducers :as r]))


(defprotocol IpAddress
  (ip-address [address])
  (hostname [address]))

(defprotocol Networkfy
  (network [cidr] [ip-address subnet]))

(defn- make-ip-address [#^bytes ip]
  (InetAddress/getByAddress ip))

(defn- get-mark [#^bytes ip-address ^Integer subnet]
  (if (<= (count ip-address) 4) (- 32 subnet) (- 128 subnet)))

(defn- get-network-address [#^bytes ip-address ^Integer subnet]
  "Function takes ip address and subnet and returns network
  IP address in byte-array form"
  (let [mark (get-mark ip-address subnet)
        ip (BigInteger. ip-address)]
    (-> (.shiftRight ip mark) (.shiftLeft mark) (.toByteArray))))

(defn- get-max-address  [#^bytes ip-address ^Integer subnet]
  "Returns maximum ip address for given ip-address and subnet.
  In other words, maximum address of network"
  (let [mark (get-mark ip-address subnet)
        ip (BigInteger. ip-address)
        mask (BigInteger/valueOf (dec (bit-shift-left 1 mark)))]
    (.toByteArray (.or ip mask))))



(defn subnet-count [x]
  "Returns set bits in byte"
  (let [testers (range 7 -1 -1)
        test! (map #(bit-test x %) testers)
        subnet-bits (count (take-while true? test!))
        all-bits (count (filter true? test!))]
    (if (not= subnet-bits all-bits) 
      (throw (Exception. (str "Subnet part: " x " is not valid subnet!")))
      subnet-bits)))






(defn- #^String get-all-addresses-helper [#^bytes ip-address ^Integer subnet]
  (let [high-address (biginteger (get-max-address ip-address subnet))
        low-address (biginteger (get-network-address ip-address subnet))
        mapper (fn [x] (-> x 
                           biginteger
                           (.toByteArray)
                           InetAddress/getByAddress
                           .getHostAddress))]
    (map mapper (range low-address (inc high-address)))))



(defn- is-in-network?-helper [#^bytes ip-address ^Integer subnet ^String q-address]
  (let [q-address (BigInteger. (.getAddress (InetAddress/getByName q-address)))
        h-address (BigInteger. (get-max-address ip-address subnet))
        l-address (BigInteger. (get-network-address ip-address subnet))]
    (boolean (and (>= h-address q-address) (<= l-address q-address)))))


(defprotocol NetworkCommon
  (get-subnet [this] "Returns subnet in String form")
  (get-highest-address [this] "Returns highest address in String form")
  (get-lowest-address [this] "Returns lowest address in String from")
  (get-network [this] "Returns CIDR notated String form")
  (is-in-network? [this ^String ip-address] "Evaluates if ip-address is in this network")
  (get-all-addresses [this] [this inclusive?] "Returns seq of addresses in this network. If second argument
                                              is true than ALL adresses are returned. If false, than addresses
                                              without first and last ip addresses are returned"))

(defrecord Network [^InetAddress ip-address ^Integer subnet]
  NetworkCommon
  (get-subnet [_] subnet)
  (get-highest-address [_] (-> (get-max-address (.getAddress ip-address) subnet) make-ip-address (.getHostAddress)))
  (get-lowest-address [this] (-> (get-network-address (.getAddress (:ip-address this)) (:subnet this)) make-ip-address (.getHostAddress)))
  (get-network [_] (-> (get-network-address (.getAddress ip-address) subnet) make-ip-address (.getHostAddress) (str "/" subnet)))
  (is-in-network? [_ q-address] (is-in-network?-helper (.getAddress ip-address) subnet q-address))
  (get-all-addresses [this inclusive?]
    (let [addresses (get-all-addresses-helper (.getAddress ip-address) subnet)]
      (if inclusive? 
        addresses
        (rest (butlast addresses)))))
  (get-all-addresses [this] (get-all-addresses this true)))

(extend java.net.InetAddress
  IpAddress
  {:ip-address #(.getHostAddress %)
   :hostname #(.getHostName %)})

(extend java.net.Inet4Address
  IpAddress
  {:ip-address #(.getHostAddress %)
   :hostname #(.getHostName %)})

(extend java.net.Inet6Address
  IpAddress
  {:ip-address #(.getHostAddress %)
   :hostname #(.getHostName %)})

(extend Network 
  IpAddress
  {:ip-address #(-> % :ip-address .getHostAddress)
   :hostname #(-> % :ip-address .getHostName)})

(extend java.lang.String
  IpAddress
  {:ip-address #(when-let [[ip sub]  (clojure.string/split % #"/")]
                  (.getHostAddress (java.net.InetAddress/getByName ip)))
   :hostname #(when-let [address (java.net.InetAddress/getByName %)]
                (.getHostName address))}
  Networkfy
  {:network (fn 
              ([x] (let [[ip sub] (clojure.string/split x #"/")
                         sub (if-not (string? sub) sub (Integer/parseInt sub))
                         ip (java.net.InetAddress/getByName ip)] 
                     (if (instance? Inet4Address ip) 
                       (do (assert (and (<= sub 32) (pos? sub)) "Subnet mask IPv4 is limited to 0-32 bits.")
                           (Network. ip sub))
                       (do (assert (and (<= sub 128) (pos? sub)) "Subnet mask IPv6 is limited to 0-128 bits.")
                           (Network. ip sub)))))
              ([^String ip-address subnet]
               (if (string? subnet)
                 (do
                   (assert (seq (re-find #"\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" subnet)) (str "Subnet: " subnet " is not valid!"))
                   (let [subnet (if (string? subnet)
                                  (reduce + (map subnet-count (map read-string (clojure.string/split subnet #"\."))))
                                  subnet)]
                     (network (str ip-address "/" subnet))))
                 (do
                   (assert (and (<= subnet 32) (> subnet 0)) (str "Subnet: " subnet " is not valid!"))
                   (network (str ip-address "/" subnet))))))})
  ;;NetworkCommon
  ;;{:get-subnet (fn [x] (-> x network get-subnet))}
  ;;{:get-highest-address(fn [x] (-> x network get-highest-address))}
  ;;{:get-lowest-address (fn [x] (-> x network get-lowest-address))}
  ;;{:get-network (fn [x] (-> x network get-network))}
  ;;{:is-in-network? (fn [x q-address] (->> x network (is-in-network? q-address)))}
  ;;{:get-all-addresses (fn [x] (get-all-addresses (network x)))}
  ;;{:get-all-addresses (fn [x inclusive?] (get-all-addresses (network x) inclusive?))})


;;(def test-network "192.168.0.253/12")
;;
;;(def x (network test-network))
;;
;;(defn apache-test []
;;  (let [addresses (time (.. (SubnetUtils. test-network) (getInfo) (getAllAddresses)))]
;;    (println "Address count:" (count addresses))))
;;
;;(defn my-test []
;;  (let [addresses (get-all-addresses (network test-network) false)]
;;    (println (time (count addresses)))
;;    (println (first addresses) (last addresses))
;;    (println "Address count:" (count addresses))))
;;
;;
;;(defn compare-speed []
;;  (println "APACHE:")
;;  (apache-test)
;;  (println "MY:")
;;  (my-test))
