(ns place-for-things.util
  (:use [amontillado.core :only [cask]]
        [graft.core :only [uri->symbol]])
  (:import [java.io ByteArrayInputStream]))

(defn is->bb [is bytecount]
  (let [bb (java.nio.ByteBuffer/allocate bytecount)
        tmp (make-array Byte/TYPE 1024)]
    (loop []
      (let [av (.available is)
            read-size (if (> av 1024)
                        1024 av)]
        (if (.hasRemaining bb)
          (do (.read is tmp 0 read-size)
              (.put bb tmp 0 read-size)
              (recur))
          bb)))))

(defn get-stored-length [map]
  (-> map :uri (str "#length")
      .getBytes
      (->> (get cask))
      (String. "utf8")))

(defn get-stored-type [map]
  (-> map :uri (str "#type")
      .getBytes
      (->> (get cask))
      (String. "utf8")))

(defn response-function [map]
  (case (:request-method map)
        :get {:status 200
              :headers {"Content-Type" (get-stored-type map)
                        "Content-Length" (get-stored-length map)}
              :body (->> map :uri .getBytes (get cask) ByteArrayInputStream.)}
        :put ((deref (ns-resolve *ns* 'place-for-things.core/four-oh-four))
              map)))

(defn put-helper [map]
  (assoc cask
    (.getBytes (:uri map))
    (.array (is->bb (:body map) (:content-length map))))
  (assoc cask
    (.getBytes (str (:uri map) "#type"))
    (.getBytes (str (:content-type map))))
  (assoc cask
    (.getBytes (str (:uri map) "#length"))
    (.getBytes (str (:content-length map))))
  (let [s (uri->symbol 'place-for-things.core (:uri map))]
    (intern (symbol (namespace s))
            (symbol (name s))
            response-function))
  {:status 200
   :body "Ok"})

(defn post-helper [map]
  (let [bytes (-> map
                  :params
                  (get "body")
                  .getBytes)]
    (assoc cask (.getBytes (:uri map)) bytes)
    (assoc cask
      (.getBytes (str (:uri map) "#type"))
      (-> map
          :params
          (get "content-type")
          .getBytes))
    (assoc cask
      (.getBytes (str (:uri map) "#length")) (.getBytes (str (count bytes))))
    (let [s (uri->symbol 'place-for-things.core (:uri map))]
      (intern (create-ns (symbol (namespace s)))
              (symbol (name s))
              response-function))
    {:status 200
     :body "Ok"}))

(defn get-helper [map]
  (if (get cask (.getBytes (:uri map)))
    (let [s (uri->symbol 'place-for-things.core (:uri map))]
      (intern (create-ns (symbol (namespace s)))
              (symbol (name s))
              response-function)
      (response-function map))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (format "
<html>
<head>
</head>
<body>
<form action=\"%s\" method=\"post\">
Content-Type: <input value=\"text/plain\" type=\"text\" name=\"content-type\" />
<br/>
<textarea style=\"width:40em;height:20em;\" name=\"body\"></textarea>
<br/>
<input type=\"submit\" value=\"submit\"/>
</form>
</body>
</html>
"
                   (:uri map))}))
