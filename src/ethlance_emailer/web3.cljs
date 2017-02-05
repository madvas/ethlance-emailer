(ns ethlance-emailer.web3
  (:require [ethlance-emailer.utils :as u]))

(def Web3 (js/require "web3"))

(defn eth [web3]
  (aget web3 "eth"))

(defn http-provider [Web3 uri]
  (let [constructor (aget Web3 "providers" "HttpProvider")]
    (constructor. uri)))

(defn create-web3 [Web3 url]
  (new Web3 (http-provider Web3 url)))

(defn contract [web3 & [abi :as args]]
  (u/js-apply (eth web3) "contract" args))

(defn contract-at [web3 abi & args]
  (u/js-apply (contract web3 abi) "at" args))

(defn contract-call [contract-instance method & args]
  (u/js-apply contract-instance method args))

(defn to-hex [any]
  (u/js-prototype-apply Web3 "toHex" [any]))

(defn to-ascii [hex-string]
  (u/js-prototype-apply Web3 "toAscii" [hex-string]))

(defn from-ascii [string & [padding]]
  (u/js-prototype-apply Web3 "fromAscii" [string padding]))

(defn to-decimal [hex-string]
  (u/js-prototype-apply Web3 "toDecimal" [hex-string]))

(defn from-decimal [number]
  (u/js-prototype-apply Web3 "fromDecimal" [number]))

(defn from-wei [number unit]
  (u/js-prototype-apply Web3 "fromWei" [number (name unit)]))

(defn to-wei [number unit]
  (u/js-prototype-apply Web3 "toWei" [number (name unit)]))

(defn to-big-number [number-or-hex-string]
  (u/js-prototype-apply Web3 "toBigNumber" [number-or-hex-string]))
