(ns status-im.ui.screens.discover.popular-hashtags.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [status-im.components.react :refer [view
                                        list-view
                                        list-item
                                        touchable-highlight
                                        scroll-view
                                        text]]
    [status-im.ui.screens.discover.styles :as st]
    [status-im.utils.listview :refer [to-datasource]]
    [status-im.ui.screens.discover.components.views :as components]
    [status-im.utils.platform :refer [platform-specific]]
    [status-im.components.toolbar-new.view :as toolbar]))

(defview discover-all-hashtags []
  (letsubs [current-account       [:get-current-account]
            popular-tags          [:get-popular-tags 10]
            {:keys [discoveries]} [:get-popular-discoveries 10]] ;uses the tags passed via :discover-search-tags state
    [view st/discover-container
     [toolbar/toolbar2 {}
      toolbar/default-nav-back
      [view {} [text {} "All hashtags"]]]
     [components/tags-menu (map :name popular-tags)]
     [scroll-view st/list-container
      [view st/recent-container
       [view st/recent-list
        (let [discoveries (map-indexed vector discoveries)]
          (for [[i {:keys [message-id] :as message}] discoveries]
            ^{:key (str "message-hashtag-" message-id)}
            [components/discover-list-item {:message         message
                                            :show-separator? (not= (inc i) (count discoveries))
                                            :current-account current-account}]))]]]]))
