(ns guestbook.core
  (:require
    [guestbook.config :refer [env]]
    [guestbook.middleware :refer [wrap-defaults]]
    [guestbook.routes :refer [router ws-handler]]
    [macchiato.server :as http]
    [macchiato.session.memory :as mem]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :refer-macros [log trace debug info warn error fatal]]))

(defn app []
  (mount/start)
  (let [host (or (:host @env) "127.0.0.1")
        port (or (some-> @env :port js/parseInt) 3000)]
    (->
      (http/start
        {:handler    (wrap-defaults router)
         :host       host
         :port       port
         :on-success #(info "guestbook started on" host ":" port)})
      (ws-handler))))

(defn start-workers [os cluster]
  (dotimes [_ (-> os .cpus .-length)]
    (.fork cluster))
  (.on cluster "exit"
       (fn [worker code signal]
         (info "worker terminated" (-> worker .-process .-pid)))))

(defn main [& args]
  (let [os      (js/require "os")
        cluster (js/require "cluster")]
    (if (.-isMaster cluster)
      (start-workers os cluster)
      (app))))
