(ns place-for-things.core
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.params :only [wrap-params]]
        [graft.core :only [graft uri->symbol]]
        [amontillado.core :only [cask recover key-table]]
        [place-for-things.util :only [put-helper get-helper is->bb post-helper]]))

(defn four-oh-four [map]
  (case (:request-method map)
        :get (get-helper map)
        :put (put-helper map)
        :post (post-helper ((wrap-params identity) map))))

(defn five-hundred [map]
  (println map)
  (.printStackTrace (:graft.core/exception map))
  {:status 200
   :body ""})

(defonce connect true)

(when connect
  (future (run-jetty (graft 'place-for-things.core) {:port 8080}))
  (recover))

(alter-var-root #'connect (constantly false))

(defn ls [map]
  {:status 200
   :body (format "
<html>
<head>
</head>
<body>
<ul>
%s
</ul>
</body
</html>
"
                 (reduce
                  str
                  (for [k (sort (keys key-table)) :when (not (.contains k "#"))]
                    (format "<li><a href=\"%s\">%s</a></li>" k k))))})
