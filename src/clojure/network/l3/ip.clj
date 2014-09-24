(ns clojure.network.l3.ip
  (:refer-clojure :exclude [+ - inc dec])
  (:require [clojure.network.l3.ip.IPAddress])
  (:import [clojure.network.l3.ip IPAddress Network]
           [java.lang UnsupportedOperationException]))

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

(defmethod print-method clojure.network.l3.ip.Network [n ^java.io.Writer w]
  (.write w (apply str "Network: " (str n))))

(defmethod print-method clojure.network.l3.ip.IPAddress [ip ^java.io.Writer w]
  (.write w (str "IPAddress: " ip)))

(. clojure.pprint/simple-dispatch addMethod clojure.network.l3.ip.Network #(.write ^java.io.Writer *out* (pr-str %)))
(. clojure.pprint/simple-dispatch addMethod clojure.network.l3.ip.IPAddress #(.write ^java.io.Writer *out* (pr-str %)))


(defn make-ip-address 
  "Returns IPAddress class instance for input data.
  Data can be of String type, i.e. \"192.168.0.1\" or
  it can be byte-array of size 4 or 16."[data]
  (IPAddress. data))


(defn make-network
  "Returns Network class instance for input data. Function
  accepts one or two argutmets. 

  In case of one argument, argument has to be cannoncial String
  representation of network:
  
  v4 - \"192.168.0.23/24\"
  v6 - \"2001:db8:85a3:0:0:8a2e:370:7334/120\"
  
  If there are 2 arguments as input, then first argument is representation of
  IPAddress or string representation of IPAddress or byte[], and second argument can be
  of type String or Integer
  
  Integer is used for CIDR notation and String type can be used for v4 subnet
  masks i.e. 255.255.255.0"
  ([^String network] (Network. network))
  ([ip mask] (if (instance? IPAddress ip)
               (Network. (str ip) mask)
               (Network. ip mask))))
