(ns edacam.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]

   [edacam.guide :refer [guide-page]]))

(def state (atom {}))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :home]
    ["/photos" :photos]
    ["/guide" :guide]
    ["/help" :help]

    (defn path-for [route & [params]]
      (if params
        (:path (reitit/match-by-name router route params))
        (:path (reitit/match-by-name router route))))]))

;; -------------------------
;; Page components

(defn home-page [state]
  [:div.container
   [:p "Kezdőlap"]])

(defn modal-bg [state]
  (fn []
    (let [selected (:selected @state)]
      (when selected [:a.modal-bg {:on-click (fn [] (swap! state dissoc :selected))}]))))

(defn pictures [state image]
  (fn []
    (let [selected (:selected @state)]
      [:div.grid-item>div.card
       [:a {:on-click (fn [] (swap! state assoc :selected (when-not (= image selected) image)))}
        [:img {:class (when (= image selected) "modal") :src (str "/images/" image ".jpg")}]
        (when (= image selected) [:img {:src (str "/images/" image ".jpg")}])]
       [:div.dropdown
        [:button.label-button "Nincs címke"]
        [:div.label-content
         (for [rovar ["Heteromurus nitidus" "Folsomia candida"]]
           [:a rovar])]]])))

(defn photos-page [state]
  [:div.container
   [modal-bg state]
   [:div.filters
    [:div.filter.dropdown
     [:button.filter-button "kamerák"]
     [:div.dropdown-content
      (for [sorszam (range 6)]
        [:a (str (+ 1 sorszam) ". kamera")])]]
    [:div.filter.dropdown
     [:button.filter-button "kategóriák"]
     [:div.dropdown-content
      (for [sorszam (range 6)]
        [:a (str (+ 1 sorszam) ". bogár")])]]]
   [:div.grid-container
    (for [image (range 8)]
      [pictures state image])]])

(defn help-page [state]
  [:div.container
   [:p "Felhasználók közvetlen írhatnak a fejlesztőknek"]])

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :home #'home-page
    :photos #'photos-page
    :guide #'guide-page
    :help #'help-page))

;; -------------------------
;; Page mounting component

(defn current-page [state]
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:ul.menu
         [:li.logo [:a.menu-link {:href (path-for :home)} "EDACAM"]]
         [:li.menu-item [:a.menu-link {:href (path-for :help)} "Kapcsolat"]]
         [:li.menu-item [:a.menu-link {:href (path-for :guide)} "Útmutató"]]
         [:li.menu-item [:a.menu-link {:href (path-for :photos)} "Képek"]]]]
       [page state]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page state] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
