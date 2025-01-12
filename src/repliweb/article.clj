(ns repliweb.article
  (:require [datomic-type-extensions.api :as d]
            [phosphor.icons :as icons]
            [powerpack.markdown :as md]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.showcase :as showcase]
            [repliweb.elements.typography :as typo]))

(defn pages-by-kind [db kind]
  (->> (d/q '[:find [?a ...]
              :in $ ?kind
              :where [?a :page/kind ?kind]]
            db kind)
       (map #(d/entity db %))
       (sort-by :page/order)))

(defn navlist [current-page pages]
  [:nav.mb-8
   [:ol
    (for [page pages]
      [:li
       (if (= (:page/uri current-page) (:page/uri page))
         [:span.menu-item.menu-item-selected
          (:page/title page)
          (icons/render :phosphor.bold/caret-right {:size 16})]
         [:a.menu-item
          {:href (:page/uri page)}
          (:page/title page)
          (icons/render :phosphor.bold/caret-right {:size 16})])])]])

(defn h3 [text]
  [:h3.text-whitish.mb-2 {:class "ml-4"} text])

(defn menu [db page]
  [:aside.basis-60.shrink-0
   (h3 "Guides")
   (navlist page (pages-by-kind db :page.kind/guide))
   (h3 "Tutorials")
   (navlist page (pages-by-kind db :page.kind/tutorial))
   (h3 "Articles")
   (navlist page (pages-by-kind db :page.kind/article))])

(def page-kind->text
  {:page.kind/guide "Guide"
   :page.kind/tutorial "Tutorial"
   :page.kind/article "Article"})

(defn ^{:indent 2} layout [{:keys [app/db]} page & body]
  (layout/layout
   {:title (:page/title page)}
   (layout/header {:logo-url "/"})
   (if (= (:page/uri page) "/learn/")
     [:div.flex.pt-8
      (menu db page)
      [:main.mx-8.grow.mb-8
       body]]
     (list
      [:div.p-4.bg-base-200.items-center.flex.flex-row.gap-4
       [:button {:popovertarget "menu"}
        (icons/render :phosphor.regular/list {:size 24})]
       [:div.bg-base-200.ml-0.mb-0.px-0 {:popover "auto" :id "menu"}
        (menu db page)]
       (page-kind->text (:page/kind page)) ": " (:page/title page)]
      [:div.my-8.mx-4.md:mx-0 body]))))

(defn render-heading [block]
  (when-let [title (:block/title block)]
    [(case (:block/level block)
       1 :h1.h1
       2 :h2.h2
       3 :h3.h3
       :h4.h4) {:class #{"mx-auto" "max-w-screen-md"}
                :id (:block/id block)}
     [:a.group.relative {:href (str "#" (:block/id block))}
      [:span.absolute.-left-4.group-hover:visible.invisible "§ "]
      title]]))

(defn render-markdown
  ([md] (render-markdown nil md))
  ([block md]
   (when (not-empty md)
     [:div.prose.mt-4.max-w-screen-md.mx-auto
      (cond-> {}
        (and (:block/id block) (nil? (:block/title block)))
        (assoc :id (:block/id block)))
      (render-heading block)
      (md/render-html md)])))

(def sizes
  {:small "max-w-screen-sm"
   :medium "max-w-screen-md"
   :large "max-w-screen-lg"})

(defn render-block [block]
  (list
   (if (not-empty (:block/markdown block))
     (render-markdown block (:block/markdown block))
     (render-heading block))
   (when (:block/a-lang block)
     (showcase/render-showcase {::showcase/style :light
                                :class #{(or (sizes (:block/comparison-size block))
                                             "max-w-screen-md")
                                         "my-6" "mx-auto"}}
       [(showcase/render-code {::showcase/lang (:block/a-lang block)
                               ::showcase/title (:block/a-title block)}
          [(:block/a-code block)])
        (showcase/render-code {::showcase/lang (:block/b-lang block)
                               ::showcase/title (:block/b-title block)
                               :class ["bg-base-100"]}
          [(:block/b-code block)])]))
   (when (:block/code block)
     [:div.max-w-screen-md.mx-auto.my-6
      (showcase/render-code {::showcase/lang (:block/lang block)
                             :class ["bg-base-200 max-w-screen-md"]}
        [(:block/code block)])])))

(defn render-page [ctx page]
  (layout ctx page
    (typo/h1 {:class #{"max-w-screen-md" "mx-auto"}} (:page/title page))
    (->> (:page/blocks page)
         (sort-by :block/idx <)
         (map render-block))
    (render-markdown (:page/body page))))
