(ns ethlance-emailer.constants)

(def currencies
  {0 "Ξ"
   1 "$"
   2 "€"
   3 "£"
   4 "\u20BD"
   5 "¥"
   6 "¥"})

(def currency-code->id
  {:ETH 0
   :USD 1
   :EUR 2
   :GBP 3
   :RUB 4
   :CNY 5
   :JPY 6})

(def currency-id->code
  {0 :ETH
   1 :USD
   2 :EUR
   3 :GBP
   4 :RUB
   5 :CNY
   6 :JPY})
