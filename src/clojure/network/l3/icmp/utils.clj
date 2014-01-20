(ns clojure.network.l3.icmp.utils
  (:import [jpcap NetworkInterface JpcapCaptor JpcapSender PacketReceiver]
           [jpcap.packet EthernetPacket ARPPacket]))
