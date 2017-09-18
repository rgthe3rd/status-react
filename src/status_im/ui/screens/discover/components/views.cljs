(ns status-im.ui.screens.discover.components.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [status-im.components.react :refer [view text image touchable-highlight]]
            [status-im.ui.screens.discover.styles :as st]
            [status-im.components.status-view.view :refer [status-view]]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.utils.identicon :refer [identicon]]
            [status-im.components.chat-icon.screen :as ci]
            [status-im.utils.platform :refer [platform-specific]]
            [status-im.components.icons.vector-icons :as vi]
            [status-im.i18n :as i18n]))

(defn title [label-kw spacing? action-kw action-fn]
  [view (merge st/title
               (when spacing? {:margin-top 16}))
   [text {:style      (get-in platform-specific [:component-styles :discover :subtitle])
          :uppercase? (get-in platform-specific [:discover :uppercase-subtitles?])
          :font       :medium}
    (i18n/label label-kw)]
   [touchable-highlight {:on-press action-fn}
    [view {} [text {:style st/title-action-text} (i18n/label action-kw)]]]])

(defn tags-menu [tags]
  [view st/tag-title-container
   (for [tag (take 3 tags)]
     ^{:key (str "tag-" tag)}
     [touchable-highlight {:on-press #(do (dispatch [:set :discover-search-tags [tag]])
                                          (dispatch [:navigate-to :discover-search-results]))}
      [view (merge (get-in platform-specific [:component-styles :discover :tag])
                   {:margin-left 2 :margin-right 2})
       [text {:style st/tag-title
              :font  :default}
        (str " #" tag)]]])])

(defn display-name [me? account-name contact-name name whisper-id]
  (cond
    me? account-name                                        ;status by current user
    (not (str/blank? contact-name)) contact-name            ; what's the
    (not (str/blank? name)) name                            ;difference
    :else (gfycat/generate-gfy whisper-id)))

(defn display-image [me? account-photo-path contact-photo-path photo-path whisper-id]
  (cond
    me? account-photo-path
    (not (str/blank? contact-photo-path)) contact-photo-path
    (not (str/blank? photo-path)) photo-path
    :else (identicon whisper-id)))

(defview discover-list-item [{:keys [message show-separator? current-account]}]
  (letsubs [{contact-name       :name
             contact-photo-path :photo-path} [:get-in [:contacts/contacts (:whisper-id message)]]]
    (let [{:keys [name photo-path whisper-id message-id status]} message
          {account-photo-path :photo-path
           account-address    :public-key
           account-name       :name} current-account
          me?        (= account-address whisper-id)
          item-style (get-in platform-specific [:component-styles :discover :item])]
      [view
       [view st/popular-list-item
        [status-view {:id     message-id
                      :style  (:status-text item-style)
                      :status status}]
        [view st/popular-list-item-second-row
         [view st/popular-list-item-name-container
          [view (merge st/popular-list-item-avatar-container
                       (:icon item-style))
           [ci/chat-icon
            (display-image me? account-photo-path contact-photo-path photo-path whisper-id)
            {:size 20}]]
          [text {:style           st/popular-list-item-name
                 :font            :medium
                 :number-of-lines 1}
           (display-name me? account-name contact-name name whisper-id)]]
         (when-not me?
           [touchable-highlight {:on-press #(dispatch [:start-chat whisper-id])}
            [view st/popular-list-chat-action
             [vi/icon :icons/chats {:color "rgb(110, 0, 228)"}]
             [text {:style st/popular-list-chat-action-text} (i18n/label :t/chat)]]])]
        (when show-separator?
          [view st/separator])]])))
