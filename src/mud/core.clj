(ns mud.core
  (:import (java.net ServerSocket)
           (java.io IOException BufferedReader InputStreamReader PrintWriter)))

(def keep-server-alive (ref true))
(def clients (ref []))

(defstruct client :socket :input :output)

;; thread-client
;; client-handler
;; parse-client-message

(defn sync-client [socket]
  (println "Attempting to sync client socket:" socket)
  (time (let [new-client (struct client
                                 socket
                                 (BufferedReader. (InputStreamReader. (.getInputStream socket)))
                                 (PrintWriter. (.getOutputStream socket) true))]
          (dosync (alter clients conj new-client))
          (println "Finished syncing client:" socket)
          new-client)))

(defn parse-client-input [client input]
  (println (str "Received input from client: " (client :socket) " said \"" input "\"")))

(defn kill-client [client]
  (println "Attempting to kill client:" (client :socket))
  (dosync (ref-set clients (remove #(= client %) @clients)))
  (println "Killed client:" (client :socket)))

(defn client-prompt [client]
  (println "Waiting for input from client:" (client :socket))
  (if-let [input (.readLine (client :input))]
    (do (parse-client-input client input)
        (client-prompt client))
    (do (println "Didn't receive any input from client:" (client :socket))
        (kill-client client))))

(defn sync-and-thread-client [client]
    (.start (Thread. #(client-prompt (sync-client client)))))

(defn await-connection [server]
  (println "Waiting for a client connection...")
  (try (let [client (.accept server)]
         (println "Accepted client connection:" client)
         (sync-and-thread-client client)) ;; TODO remember to .close the clients
       (catch IOException ex
         (println "Failed to accept client connection."))))

(defn server []
  (.start
   (Thread.
    #(let [port 6689]
       (try (let [socket (ServerSocket. port)]
              (println "Server bound to port " port ".")
              (println "Connection accepting thread is: " (Thread/currentThread))
              (while @keep-server-alive (await-connection socket))
              (println "Cleaning up socket on port " port " and exiting.")
              (.close socket))
            (catch IOException ex
              (println "Server could not bind to port " port ".")))))))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
