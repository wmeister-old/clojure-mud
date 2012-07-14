(ns mud.core
  (:import (mud.exception ClientClosedConnectionException)
           (java.net ServerSocket)
           (java.io IOException BufferedReader InputStreamReader PrintWriter)))

(def keep-server-alive (ref true))
(def clients (ref []))

(defstruct client :socket :input :output)

;; thread-client
;; client-handler
;; parse-client-message

(defn sync-client [socket]
  (println "Attempting to sync client socket:" socket)
  (let [new-client (struct client
                           socket
                           (BufferedReader. (InputStreamReader. (.getInputStream socket)))
                           (PrintWriter. (.getOutputStream socket) true))]
    (dosync (alter clients conj new-client))
    (println "Finished syncing client:" socket)
    new-client))

(defn kill-client [client]
  (println "Attempting to kill client:" (client :socket))
  (.close (client :input))
  (.close (client :output))
  (.close (client :socket))
  (println "Current clients: " @clients)
  (dosync (ref-set clients (remove #(= client %) @clients)))
  (println "Killed client:" (client :socket))
  (throw (ClientClosedConnectionException. (str "Client socket closed: " (client :socket)))))

(defn client-print [client msg]
  (let [output (client :output)]
    (.print  output msg)
    (.flush output)))

(defn client-println [client msg]
  (.println (client :output) msg))


(defn client-prompt [client]
  (println "Waiting for input from client:" (client :socket))
  (if-let [input (.readLine (client :input))]
    (do (str "Received input from client: " (client :socket) " said \"" input "\"")
        input)
    (do (println "Didn't receive any input from client:" (client :socket))
        (kill-client client))))

(defn authenticate-client [client username password]
  (when (and (= username "dev")
             (= password "dev"))
    ()))

(defn registration-process [client]
)

(defn client-exit [client]
  (println "Client requested to close socket:" (client :socket))
  (kill-client client))



(defn zone-client [client]
)


(defn login-process [client]
)


(defn main-menu [client]
  (client-print client (str "Choose an option:\n\n"
                            "\t1 - login\n"
                            "\t2 - register\n"
                            "\t3 - exit\n"
                            "=> "))
  (case (client-prompt client)
    "1" (login-process client)
    "2" (registration-process client)
    "3" (client-exit client)
    (do (client-println client "Invalid option.")
        (main-menu client))))


(defn login-process [client]
  (let [username (client-prompt client)
        password (client-prompt client)]
    (if (authenticate-client client username password)
      (zone-client :sandbox client)
      (do (client-println "Invalid login credentials.")
          (main-menu client)))))

(defn sync-and-thread-client [client]
    (.start (Thread. #(main-menu (sync-client client)))))

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

(defn -main [& args]
  (println "h0h0!,... server start!")
  (server))
