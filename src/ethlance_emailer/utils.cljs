(ns ethlance-emailer.utils
  (:require
    [bidi.bidi :as bidi]
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [clojure.string :as string]
    [ethlance-emailer.constants :as constants]
    [ethlance-emailer.routes :refer [routes]]
    [goog.string :as gstring]))

(def SoliditySha3 (aget (js/require "solidity-sha3") "default"))

(defn js-val [clj-or-js-dict]
  (cond
    (map? clj-or-js-dict) (clj->js clj-or-js-dict)
    (vector? clj-or-js-dict) (clj->js clj-or-js-dict)
    :else clj-or-js-dict))

(def js->cljk #(js->clj % :keywordize-keys true))

(def js->cljkk (comp (partial transform-keys cs/->kebab-case) js->cljk))

(def cljkk->js (comp clj->js (partial transform-keys cs/->camelCase)))

(defn callback-js->clj [x]
  (if (fn? x)
    (fn [err res]
      (x err (js->cljkk res)))
    x))

(defn args-cljkk->js [args]
  (map (comp cljkk->js callback-js->clj) args))

(defn js-apply
  ([this method-name]
   (js-apply this method-name nil))
  ([this method-name args]
   (let [method-name (name method-name)]
     (if-let [method (aget this (if (string/includes? method-name "-") ; __callback gets wrongly transformed
                                  (cs/->camelCase method-name)
                                  method-name))]
       (js->cljkk (.apply method this (clj->js (args-cljkk->js args))))
       (throw (str "Method: " method-name " was not found in object."))))))

(defn safe-js-apply
  ([this method-name]
   (js-apply this method-name []))
  ([this method-name args]
   (let [method-name (name method-name)]
     (if-let [method (aget this method-name)]
       (.apply method this (clj->js args))
       (throw (str "Method: " method-name " was not found in object."))))))

(defn js-prototype-apply [js-obj method-name args]
  (js-apply (aget js-obj "prototype") method-name args))

(defn prop-or-clb-fn [& ks]
  (fn [web3 & args]
    (if (fn? (first args))
      (js-apply (apply aget web3 (butlast ks)) (str "get" (cs/->PascalCase (last ks))) args)
      (js->cljkk (apply aget web3 ks)))))

(defn to-number [x]
  (safe-js-apply x "toNumber"))

(defn big-num->num [x]
  (if (and x (aget x "toNumber"))
    (to-number x)
    x))

(defn big-nums->nums [coll]
  (map big-num->num coll))

(defn ns+name [x]
  (when x
    (str (when-let [n (namespace x)] (str n "/")) (name x))))

(defn sha3 [& args]
  (apply SoliditySha3 (map #(if (keyword? %) (ns+name %) %) args)))

(defn remove-zero-chars [s]
  (string/join (take-while #(< 0 (.charCodeAt % 0)) s)))

(defn prepend-address-zeros [address]
  (let [n (- 42 (count address))]
    (if (pos? n)
      (->> (subs address 2)
        (str (string/join (take n (repeat "0"))))
        (str "0x"))
      address)))

(defn uint8? [x]
  (and x (not (neg? x))))

(defn uint? [x]
  (and x (not (neg? x))))

(defn address? [x]
  (string? x))

(defn bytes32? [x]
  (string? x))

(defn bytes32-or-nil? [x]
  (or (nil? x) (string? x)))

(defn uint-coll? [x]
  (and x (every? uint? x)))

(defn string-or-nil? [x]
  (or (nil? x) (string? x)))

(defn big-num? [x]
  (and x (aget x "toNumber")))

(defn split-include-empty [s re]
  (butlast (string/split (str s " ") re)))

(defn map-val [x]
  (second (first x)))

(defn path-for [& args]
  (str "#" (apply bidi/path-for routes args)))

(defn full-path-for [& args]
  (str "http://ethlance.com/" (apply path-for args)))

(defn rating->star [rating]
  (/ (or rating 0) 20))

(defn replace-comma [x]
  (string/replace x \, \.))

(defn parse-float [number]
  (if (string? number)
    (js/parseFloat (replace-comma number))
    number))

(defn to-locale-string [x max-fraction-digits]
  (let [parsed-x (cond
                   (string? x) (parse-float x)
                   (nil? x) ""
                   :else x)]
    (if-not (js/isNaN parsed-x)
      (.toLocaleString parsed-x js/undefined #js {:maximumFractionDigits max-fraction-digits})
      x)))

(defn with-currency-symbol [value currency]
  (case currency
    1 (str (constants/currencies 1) value)
    (str value (constants/currencies currency))))

(defn number-fraction-part [x]
  (let [frac (second (string/split (str x) #"\."))]
    (if frac
      (str "." frac)
      "")))

(defn format-currency [value currency & [{:keys [:full-length? :display-code?]}]]
  (let [value (-> (or value 0)
                big-num->num)
        value (if full-length?
                (str (to-locale-string (js/parseInt value) 0) (number-fraction-part value))
                (to-locale-string value (if (= currency 0) 3 2)))]
    (if display-code?
      (str value " " (name (constants/currency-id->code currency)))
      (with-currency-symbol value currency))))

(defn truncate
  "Truncate a string with suffix (ellipsis by default) if it is
   longer than specified length."
  ([string length]
   (truncate string length "..."))
  ([string length suffix]
   (let [string-len (count string)
         suffix-len (count suffix)]
     (if (<= string-len length)
       string
       (str (subs string 0 (- length suffix-len)) suffix)))))

(defn days-ago-from-now [days]
  (let [d (new js/Date)]
    (.setDate d (- (.getDate d) days))
    d))

(defn hours-ago-from-now [hours]
  (let [d (new js/Date)]
    (.setHours d (- (.getHours d) hours))
    d))

(defn log! [& args]
  (apply js/console.log (concat args [(str (new js/Date))])))

(defn get-time-without-milis [date]
  (js/Math.floor (/ (.getTime date) 1000)))