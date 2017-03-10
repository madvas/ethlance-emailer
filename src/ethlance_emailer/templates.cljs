(ns ethlance-emailer.templates
  (:require [goog.string :as gstring]
            [goog.string.format]
            [ethlance-emailer.utils :as u]))

(defn on-invoice-added [invoice freelancer]
  (gstring/format
    "You just received an invoice to pay from %s. The amount is <b>%s</b>.
    <div style='margin-bottom: 20px'></div>
    <div>Message from %s:</div>
    <div>%s</div>"
    (:user/name freelancer)
    (u/format-currency (:invoice/amount invoice) 0 {:full-length? true})
    (:user/name freelancer)
    (:invoice/description invoice)))

(defn on-invoice-paid [invoice employer]
  (gstring/format
    "Your invoice was just paid by your employer %s. The amount is <b>%s</b>."
    (:user/name employer)
    (u/format-currency (:invoice/amount invoice) 0 {:full-length? true})))

(defn on-invoice-cancelled [invoice freelancer]
  (gstring/format
    "Invoice your received before was just cancelled by %s. The amount was <b>%s</b>."
    (:user/name freelancer)
    (u/format-currency (:invoice/amount invoice) 0 {:full-length? true})))

(defn on-job-proposal-added [job contract freelancer]
  (gstring/format
    "%s just applied for your job <i>%s</i> with a rate <b>%s</b>.
    <div style='margin-bottom: 20px'></div>
    <div>Proposal message:</div>
    <div>%s</div>"
    (:user/name freelancer)
    (:job/title job)
    (u/format-currency (:proposal/rate contract) (:job/reference-currency job) {:full-length? true
                                                                                :display-code? true})
    (:proposal/description contract)))

(defn on-job-contract-added [job contract]
  (gstring/format
    "Congratulations, you've been hired for the job <i>%s</i>!
    <div style='margin-bottom: 20px'></div>
    <div>Message from employer:</div>
    <div>%s</div>"
    (:job/title job)
    (:contract/description contract)))

(defn on-job-contract-cancelled [job contract freelancer]
  (gstring/format
    "%s just cancelled contract for your job <i>%s</i>.
    <div style='margin-bottom: 20px'></div>
    <div>Message from freelancer:</div>
    <div>%s</div>"
    (:user/name freelancer)
    (:job/title job)
    (:contract/cancel-description contract)))


(defn on-job-contract-feedback-added [rating feedback sender]
  (gstring/format
    "You just received feedback from %s
    <div style='margin-bottom: 20px'></div>
    <div>Rating: %s</div>
    <div>Feedback:</div>
    <div>%s</div>"
    (:user/name sender)
    (u/rating->star rating)
    feedback))

(defn on-job-invitation-added [job contract]
  (gstring/format
    "You've been invited to apply for a job <i>%s</i>!
    <div style='margin-bottom: 20px'></div>
    <div>Invitation Message:</div>
    <div>%s</div>"
    (:job/title job)
    (:invitation/description contract)))

(defn on-job-contract-message-added [message sender]
  (gstring/format
    "You've just received message from %s:
    <div style='margin-bottom: 20px'></div>
    <div>%s</div>"
    (:user/name sender)
    (:message/text message)))

(defn job-recommendations [intro-text jobs]
  (-> (gstring/format "<div>%s</div>" intro-text)
    (str "<ul class=\"link-list\">")
    (str (reduce (fn [acc {:keys [:job/title :job/description :job/id]}]
                   (str acc
                        (gstring/format
                          "<li><a href=\"http://ethlance.com/#/job/%s\">%s</a><div>%s</div></li>"
                          id title (u/truncate description 150)))) "" jobs))
    (str "</ul>")))

(def on-job-added (partial job-recommendations "We just got a new job matching your skills!"))
(def on-job-recommendations-interval (partial job-recommendations "We have some new jobs matching your skills!"))

