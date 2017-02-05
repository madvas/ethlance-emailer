(ns ethlance-emailer.ethlance-db
  (:require [ethlance-emailer.utils :as u]
            [cljs.spec :as s]
            [ethlance-emailer.web3 :as web3]
            [medley.core :as medley]
            [clojure.string :as string]))

(s/def :user/email string?)
(s/def :user/name string?)
(s/def :job/title string?)
(s/def :invitation/description string?)
(s/def :proposal/description string?)
(s/def :proposal/rate u/big-num?)
(s/def :contract/description string?)
(s/def :contract/employer-feedback string?)
(s/def :contract/employer-feedback-rating u/uint8?)
(s/def :contract/freelancer-feedback string?)
(s/def :contract/freelancer-feedback-rating u/uint8?)
(s/def :contract/freelancer u/uint?)
(s/def :contract/job u/uint?)
(s/def :invoice/description string?)
(s/def :invoice/amount u/big-num?)

(defn remove-uint-coll-fields [fields]
  (remove #(= (s/form %) 'ethlance-emailer.utils/uint-coll?) fields))

(defn string-type? [field]
  (contains? #{'cljs.core/string? 'ethlance-emailer.utils/string-or-nil?} (s/form field)))

(defn get-entities-args [ids fields]
  (let [fields (remove-uint-coll-fields fields)
        records (flatten (for [id ids]
                           (for [field fields]
                             (u/sha3 field id))))]
    [fields records]))

(def str-delimiter "99--DELIMITER--11")
(def list-delimiter "99--DELIMITER-LIST--11")

(defn string-blob->strings [string-blob delimiter]
  (->> (u/split-include-empty string-blob str-delimiter)
    (map (fn [s]
           (when (seq s)
             (subs s 1))))))

(defn uint->value [val field]
  (condp = (s/form field)
    'cljs.core/boolean? (if (zero? (u/to-number val)) false true)
    'ethlance-emailer.utils/bytes32? (u/remove-zero-chars (web3/to-ascii (web3/from-decimal val)))
    'ethlance-emailer.utils/address? (u/prepend-address-zeros (web3/from-decimal val))
    'ethlance-emailer.utils/big-num? (u/to-number (web3/from-wei val :ether))
    (u/to-number val)))

(def field-pred->solidity-type
  {'cljs.core/boolean? 1
   'ethlance-emailer.utils/uint8? 2
   'ethlance-emailer.utils/uint? 3
   'ethlance-emailer.utils/address? 4
   'ethlance-emailer.utils/bytes32? 5
   'cljs.cljs.core/int? 6
   'cljs.core/string? 7
   'ethlance-emailer.utils/big-num? 3
   })

(defn parse-entities [ids fields result]
  (let [ids (vec ids)
        grouped-by-string (group-by string-type? fields)
        string-fields (get grouped-by-string true)
        uint-fields (get grouped-by-string false)
        uint-fields-count (count uint-fields)]
    (let [parsed-result
          (reduce (fn [acc [i result-item]]
                    (let [entity-index (js/Math.floor (/ i uint-fields-count))
                          field-name (nth uint-fields (mod i uint-fields-count))]
                      (assoc-in acc [(nth ids entity-index) field-name]
                                (uint->value result-item field-name))))
                  {} (medley/indexed (first result)))]
      (reduce (fn [acc [entity-index entity-strings]]
                (reduce (fn [acc [string-index string-value]]
                          (let [field-name (nth string-fields string-index)]
                            (if (seq ids)
                              (assoc-in acc [(nth ids entity-index) field-name] string-value)
                              acc)))
                        acc (medley/indexed (string-blob->strings entity-strings str-delimiter))))
              parsed-result (medley/indexed (u/split-include-empty (second result) list-delimiter))))))

(defn get-entities [ids fields instance]
  (let [[fields records] (get-entities-args ids fields)]
    (->> (web3/contract-call instance
                             :get-entity-list
                             records
                             (map (comp field-pred->solidity-type s/form) fields)
                             #_(fn [err result]
                                 (if err
                                   (on-error err)
                                   (on-success (parse-entities ids fields result)))))
      (parse-entities ids fields))))

(defn get-user [user-id {:keys [:ethlance-db]}]
  (u/map-val (get-entities [user-id] [:user/name :user/email] ethlance-db)))

(defn get-job [job-id {:keys [:ethlance-db]}]
  (u/map-val (get-entities [job-id] [:job/title] ethlance-db)))

(defn get-invoice [invoice-id {:keys [:ethlance-db]} & [fields]]
  (-> (u/map-val (get-entities [invoice-id] (or fields
                                                [:invoice/amount :invoice/description]) ethlance-db))))

(defn get-contract [contract-id fields {:keys [:ethlance-db]}]
  (-> (u/map-val (get-entities [contract-id] fields ethlance-db))))
