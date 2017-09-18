(ns status-im.ui.screens.discover.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [clojure.string :as str]
    [status-im.components.react :refer [view
                                        scroll-view
                                        text
                                        text-input
                                        touchable-highlight]]
    [status-im.components.icons.vector-icons :as vi]
    [status-im.components.toolbar-new.view :as toolbar]
    [status-im.components.toolbar-new.actions :as act]
    [status-im.components.drawer.view :as drawer]
    [status-im.components.carousel.carousel :as carousel]
    [status-im.ui.screens.discover.components.views :as components]
    [status-im.utils.platform :as platform]
    [status-im.i18n :as i18n]
    [status-im.ui.screens.discover.styles :as styles]
    [status-im.ui.screens.contacts.styles :as contacts-st]))

(defn get-hashtags [status]
  (let [hashtags (map #(str/lower-case (str/replace % #"#" "")) (re-seq #"[^ !?,;:.]+" status))]
    (or hashtags [])))

(defn toolbar-view [show-search? search-text]
  [toolbar/toolbar-with-search
   {:show-search?       show-search?
    :search-text        search-text
    :search-key         :discover
    :title              (i18n/label :t/discover)
    :search-placeholder (i18n/label :t/search-tags)
    :nav-action         (act/hamburger drawer/open-drawer!)
    :on-search-submit   (fn [text]
                          (when-not (str/blank? text)
                            (let [hashtags (get-hashtags text)]
                              (dispatch [:set :discover-search-tags hashtags])
                              (dispatch [:navigate-to :discover-search-results]))))}])


(defview top-status-for-popular-hashtag [{:keys [tag current-account]}]
  (letsubs [discoveries [:get-popular-discoveries 1 [tag]]]
    [view (merge styles/popular-list-container
                 (get-in platform/platform-specific [:component-styles :discover :popular]))
     [view styles/row
      [view {}
       [touchable-highlight {:on-press #(do (dispatch [:set :discover-search-tags [tag]])
                                            (dispatch [:navigate-to :discover-search-results]))}
        [view {}
         [text {:style styles/tag-name
                :font  :medium}
          (str " #" (name tag))]]]]
      [view styles/tag-count-container
       [text {:style styles/tag-count
              :font  :default}
        (:total discoveries)]]]
     [components/discover-list-item {:message         (first (:discoveries discoveries))
                                     :show-separator? false
                                     :current-account current-account}]]))

(defview popular-hashtags-preview [{:keys [contacts current-account]}]
  (letsubs [popular-tags [:get-popular-tags 10]]
    [view styles/popular-container
     [components/title :t/popular-tags false :t/all #(do (dispatch [:set :discover-search-tags (map :name popular-tags)])
                                                         (dispatch [:navigate-to :discover-all-hashtags]))]
     (if (pos? (count popular-tags))
       [carousel/carousel {:pageStyle styles/carousel-page-style
                           :gap       8
                           :sneak     16
                           :count     (count popular-tags)}
        (for [{:keys [name]} popular-tags]
          [top-status-for-popular-hashtag {:tag             name
                                           :contacts        contacts
                                           :current-account current-account}])]
       [text (i18n/label :t/none)])]))


(defn empty-discoveries []
  [view contacts-st/empty-contact-groups
   ;; todo change the icon
   [vi/icon :icons/group-big {:style contacts-st/empty-contacts-icon}]
   [text {:style contacts-st/empty-contacts-text}
    (i18n/label :t/no-statuses-discovered)]])

(defn recent-statuses-preview [current-account discoveries]
  [view styles/recent-statuses-preview-container
   [components/title :t/recent false :t/all #(dispatch [:navigate-to :discover-all-recent])]
   (if (pos? (count discoveries))
     [carousel/carousel {:pageStyle styles/carousel-page-style
                         :gap       8
                         :sneak     16
                         :count     (count discoveries)}
      (for [discovery discoveries]
        [view styles/recent-statuses-preview-content
         [components/discover-list-item {:message         discovery
                                         :show-separator? false
                                         :current-account current-account}]])]
     [text (i18n/label :t/none)])])

(defview discover [current-view?]
  (letsubs [show-search     [:get-in [:toolbar-search :show]]
            search-text     [:get-in [:toolbar-search :text]]
            contacts        [:get-contacts]
            current-account [:get-current-account]
            discoveries     [:get-recent-discoveries]]
    [view styles/discover-container
     [toolbar-view (and current-view?
                        (= show-search :discover)) search-text]
     (if discoveries
       [scroll-view styles/list-container
        [recent-statuses-preview current-account discoveries]
        [popular-hashtags-preview {:contacts        contacts
                                   :current-account current-account}]]
       [empty-discoveries])]))
