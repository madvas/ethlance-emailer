(ns ethlance-emailer.ethlance-db
  (:require [ethlance-emailer.utils :as u]
            [cljs.spec :as s]
            [ethlance-emailer.web3 :as web3]
            [medley.core :as medley]
            [clojure.string :as string]
            [ethlance-emailer.constants :as constants]))

(s/def :user/email string?)
(s/def :user/name string?)
(s/def :job/title string?)
(s/def :job/reference-currency u/uint8?)
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
(s/def :job/skills u/uint-coll?)
(s/def :job/skills-count u/uint?)
(s/def :job/description string?)
(s/def :user.notif/disabled-all? boolean?)
(s/def :user.notif/disabled-newsletter? boolean?)
(s/def :user.notif/disabled-on-invoice-added? boolean?)
(s/def :user.notif/disabled-on-invoice-paid? boolean?)
(s/def :user.notif/disabled-on-job-contract-added? boolean?)
(s/def :user.notif/disabled-on-job-contract-feedback-added? boolean?)
(s/def :user.notif/disabled-on-job-invitation-added? boolean?)
(s/def :user.notif/disabled-on-job-proposal-added? boolean?)
(s/def :user.notif/job-recommendations u/uint8?)

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

(defn id-counts->ids [id-counts]
  (reduce (fn [acc [id count]]
            (concat acc (map #(vec [id %]) (range count))))
          [] id-counts))

(defn get-entities-field-items-args [id-counts field]
  (let [ids+sub-ids (id-counts->ids id-counts)
        records (map (fn [[id sub-id]]
                       (u/sha3 field id sub-id)) ids+sub-ids)]
    [ids+sub-ids field records [(field-pred->solidity-type 'ethlance-emailer.utils/uint?)]]))

(defn parse-entities-field-items [ids+sub-ids field result]
  (reduce (fn [acc [i result-item]]
            (let [[id] (nth ids+sub-ids i)]
              (update-in acc [id field] conj (uint->value result-item field))))
          {} (medley/indexed (first result))))

(defn get-entities-field-items [id-counts field instance]
  (let [[ids+sub-ids field records types] (get-entities-field-items-args id-counts field)]
    (->> (web3/contract-call instance
                             :get-entity-list
                             records
                             types)
      (parse-entities-field-items ids+sub-ids field))))

(defn get-user [user-id {:keys [:ethlance-db]} & [additional-fields]]
  (u/map-val (get-entities [user-id] (concat [:user/name :user/email] additional-fields) ethlance-db)))

(defn get-job [job-id {:keys [:ethlance-db]} & [fields]]
  (u/map-val (get-entities [job-id] (or fields [:job/title :job/reference-currency]) ethlance-db)))

(defn get-invoice [invoice-id {:keys [:ethlance-db]} & [fields]]
  (-> (u/map-val (get-entities [invoice-id] (or fields
                                                [:invoice/amount :invoice/description]) ethlance-db))))

(defn get-contract [contract-id fields {:keys [:ethlance-db]}]
  (-> (u/map-val (get-entities [contract-id] fields ethlance-db))))

(defn get-job-skills [job-id skill-count {:keys [:ethlance-db]}]
  (u/map-val (get-entities-field-items {job-id skill-count} :job/skills ethlance-db)))

(defn search-freelancers-by-any-of-skills [category skills job-recommendations offset limit {:keys [:ethlance-search]}]
  (let [rates (take (count constants/currencies) (repeat 0))]
    (-> (web3/contract-call ethlance-search
                            :search-freelancers
                            category [] skills 0 0 rates rates [0 0 0 job-recommendations offset limit 0])
      u/big-nums->nums)))

(defn search-jobs [min-created-on offset limit {:keys [:ethlance-search]}]
  (let [min-budgets (take (count constants/currencies) (repeat 0))]
    (-> (web3/contract-call ethlance-search
                            :search-jobs
                            0 [] [] [] [] [] [] min-budgets [0 0 0 0 0 min-created-on offset limit])
      u/big-nums->nums)))
