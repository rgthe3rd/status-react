(ns status-im.chat.models.commands
  (:require [status-im.chat.constants :as chat-consts]
            [status-im.bots.constants :as bots-constants]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn scope->int [{:keys [global? registered-only? personal-chats? group-chats? can-use-for-dapps?]}]
  (bit-or (if global? 1 0)
          (if registered-only? 2 0)
          (if personal-chats? 4 0)
          (if group-chats? 8 0)
          (if can-use-for-dapps? 16 0)))

(defn can-be-suggested?
  [first-char name-key text]
  (fn [command]
    (let [name (get command name-key)]
      (let [text' (cond
                    (.startsWith text first-char)
                    text

                    (str/blank? text)
                    first-char

                    :default
                    nil)]
        (.startsWith (str first-char name) text')))))

(defn get-mixable-commands [{:contacts/keys [contacts]}]
  (->> contacts
       (vals)
       (filter :mixable?)
       (mapv :commands)
       (apply merge)))

(defn get-mixable-identities [{:contacts/keys [contacts]}]
  (->> contacts
       (vals)
       (filter :mixable?)
       (map (fn [{:keys [whisper-identity]}]
              {:identity whisper-identity}))))

(defn find-suggestions
  ([commands text]
    (find-suggestions chat-consts/command-char :name commands text))
  ([first-char name-key commands text]
   (->> commands
        (map val)
        (map (fn [items]
               (filter #((can-be-suggested? first-char name-key text) %) items)))
        (remove empty?)
        (flatten))))

(defn get-request-suggestions
  [{:keys [current-chat-id] :as db} text]
  (let [requests (->> (get-in db [:chats current-chat-id :requests])
                      (map (fn [{:keys [type] :as req}]
                             [type (map (fn [resp]
                                          (assoc resp :request req))
                                        (get-in db [:contacts/contacts current-chat-id :responses type]))]))
                      (remove (fn [[_ items]] (empty? items)))
                      (into {}))]
    (find-suggestions requests text)))

(defn get-command-suggestions
  [{:keys [current-chat-id] :as db} text]
  (->> (get-in db [:chats current-chat-id :contacts])
       (into (get-mixable-identities db))
       (map (fn [{:keys [identity]}]
              (let [commands (get-in db [:contacts/contacts identity :commands])]
                (find-suggestions commands text))))
       (flatten)))

(defn get-global-command-suggestions
  [{:keys [global-commands] :as db} text]
  (find-suggestions chat-consts/bot-char :name global-commands text))

(defn commands-for-chat
  [{:keys          [global-commands chats]
    :contacts/keys [contacts]
    :accounts/keys [accounts current-account-id]
    :as            db} chat-id text]
  (let [global-commands (get-global-command-suggestions db text)
        commands        (get-command-suggestions db text)
        account         (get accounts current-account-id)
        commands        (-> (into [] global-commands)
                            (into commands))
        {chat-contacts :contacts} (get chats chat-id)]
    (->> commands
         (remove (fn [{:keys [scope]}]
                   (or
                     (and (:registered-only? scope)
                          (not (:address account)))
                     (and (not (:personal-chats? scope))
                          (= (count chat-contacts) 1))
                     (and (not (:group-chats? scope))
                          (> (count chat-contacts) 1))
                     (and (not (:can-use-for-dapps? scope))
                          (every? (fn [{:keys [identity]}]
                                    (get-in contacts [identity :dapp?]))
                                  chat-contacts))))))))

(defn parse-command-message-content
  [commands global-commands content]
  (if (map? content)
    (let [{:keys [command bot]} content]
      (if (and bot (not (bots-constants/mailman-bot? bot)))
        (update content :command #((keyword bot) global-commands))
        (update content :command #((keyword command) commands))))
    content))

(defn parse-command-request [commands content]
  (log/debug "ALWX parse-command-request" commands content)
  (update content :command (fn [name]
                             ;; TODO(alwx): probably not `first` !!
                             (first ((keyword name) commands)))))