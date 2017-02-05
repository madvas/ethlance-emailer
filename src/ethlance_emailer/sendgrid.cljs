(ns ethlance-emailer.sendgrid
  (:require [cljs.nodejs :as nodejs]
            [ethlance-emailer.utils :as u]))

(def Sendgrid (js/require "sendgrid"))
(def helper (aget Sendgrid "mail"))
(def Email (aget helper "Email"))
(def Content (aget helper "Content"))
(def Substitution (aget helper "Substitution"))
(def Mail (aget helper "Mail"))
(def sendgrid (Sendgrid (aget nodejs/process "env" "SENDGRID_API_KEY")))

(aget Sendgrid "Email")

(defn api [request to]
  (u/safe-js-apply sendgrid
                   "API"
                   [request (fn [error response]
                              (if error
                                (.error js/console error)
                                (.log js/console to (-> (js->clj response :keywordize-keys true)
                                                      (get-in [:headers :date])))))]))

(defn set-template-id! [mail id]
  (u/safe-js-apply mail "setTemplateId" [id]))

(defn empty-request [mail]
  (u/safe-js-apply sendgrid "emptyRequest" [{:method "POST"
                                             :path "/v3/mail/send"
                                             :body (.toJSON mail)}]))

(defn add-substitution! [mail key value]
  (u/safe-js-apply (first (aget mail "personalizations")) "addSubstitution" [(new Substitution key value)]))


(defn send-notification-mail
  ([to subject body receiver-name button-text button-href]
   (send-notification-mail ["Ethlance" "noreply@ethlance.com"] to subject body receiver-name button-text button-href))
  ([from to subject body receiver-name button-text button-href]
   (when (seq to)
     (let [from-email (new Email (second from) (first from))
           to-email (new Email to)
           content (new Content "text/html" body)
           mail (new Mail from-email subject to-email content)]
       (add-substitution! mail "%name%" receiver-name)
       (add-substitution! mail "%open-detail-button-text%" button-text)
       (add-substitution! mail "%open-detail-button-href%" button-href)
       (set-template-id! mail "ba84a298-b36e-4c0a-bf65-fe6c496d4f5c")
       (-> mail
         empty-request
         (api to))))))
