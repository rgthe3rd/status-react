(ns status-im.ui.screens.discover.search-results.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [status-im.utils.listview :refer [to-datasource]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.react :refer [view
                                                text
                                                list-view
                                                list-item
                                                scroll-view
                                                touchable-highlight]]
            [status-im.components.icons.vector-icons :as vi]
            [status-im.components.toolbar.view :refer [toolbar]]
            [status-im.ui.screens.discover.components.views :as components]
            [status-im.utils.platform :refer [platform-specific]]
            [status-im.i18n :as i18n]
            [status-im.ui.screens.discover.styles :as styles]
            [status-im.ui.screens.contacts.styles :as contacts-styles]
            [status-im.components.toolbar-new.view :as toolbar]))

(defn render-separator [_ row-id _]
  (list-item [view {:style styles/row-separator
                    :key   row-id}]))


(defview discover-search-results []
  (letsubs [{:keys [discoveries total]}   [:get-popular-discoveries 250]
            tags                          [:get :discover-search-tags]
            current-account               [:get-current-account]]
    (let [datasource  (to-datasource discoveries)]
      [view styles/discover-tag-container
       [status-bar]
       [toolbar/toolbar2 {}
        toolbar/default-nav-back
        [view {:flex-direction  :row
               :justify-content :flex-start}
         [text {} (str "#" (first tags) " " total)]]]
       (if (empty? discoveries)
         [view styles/empty-view
          [vi/icon :icons/group-big {:style contacts-styles/empty-contacts-icon}]
          [text {:style contacts-styles/empty-contacts-text}
           (i18n/label :t/no-statuses-found)]]
         [list-view {:dataSource      datasource
                     :renderRow       (fn [row _ _]
                                        (list-item [components/discover-list-item
                                                    {:message         row
                                                     :current-account current-account}]))
                     :renderSeparator render-separator
                     :style           styles/recent-list}])])))
