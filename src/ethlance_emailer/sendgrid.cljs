(ns ethlance-emailer.sendgrid
  (:require [cljs.nodejs :as nodejs]
            [ethlance-emailer.utils :as u]))

(def dry-run? false)

(def Sendgrid (js/require "sendgrid"))
(def helper (aget Sendgrid "mail"))
(def Email (aget helper "Email"))
(def Content (aget helper "Content"))
(def Substitution (aget helper "Substitution"))
(def Mail (aget helper "Mail"))
(def sendgrid (Sendgrid (aget nodejs/process "env" "SENDGRID_API_KEY")))

(aget Sendgrid "Email")

(defn api [request user-id to email-type]
  (if dry-run?
    (println "Sending" to user-id email-type)
    (u/safe-js-apply sendgrid
                     "API"
                     [request (fn [error response]
                                (if error
                                  (.error js/console error)
                                  (.log js/console
                                        to
                                        user-id
                                        (-> (js->clj response :keywordize-keys true)
                                          (get-in [:headers :date]))
                                        (name email-type))))])))

(defn set-template-id! [mail id]
  (u/safe-js-apply mail "setTemplateId" [id]))

(defn empty-request [mail]
  (u/safe-js-apply sendgrid "emptyRequest" [{:method "POST"
                                             :path "/v3/mail/send"
                                             :body (.toJSON mail)}]))

(defn add-substitution! [mail key value]
  (u/safe-js-apply (first (aget mail "personalizations")) "addSubstitution" [(new Substitution key value)]))


(defn send-notification-mail
  ([user-id to subject body receiver-name button-text button-href email-type]
   (send-notification-mail ["Ethlance" "noreply@ethlance.com"] user-id to subject body receiver-name button-text button-href email-type))
  ([from user-id to subject body receiver-name button-text button-href email-type]
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
         (api user-id to email-type))))))
