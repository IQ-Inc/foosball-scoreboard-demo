(ns foosball-score.persistence
  "Defines means of interacting with a persistence layer, notably 'athletes'.
  
  An 'athlete' object is associated by a card ID. An athlete has the following
  structure:
    - key 'name',     value type string
    - key 'stats',    value type map containing...
      - key 'win',    value type int
      - key 'losses', value type int
      - key 'ties',   value type int
      
  The athlete convenience methods provided out-of-the-box access to
  these values."
  {:author "Ian McIntyre"})

;;;;;;;;;;;;;;;;;;;;;
;; Hard-coded players
;;;;;;;;;;;;;;;;;;;;;
(def ian    "18A632")
(def mikel  "18A644")
(def mikes  "18A64E")
(def tim    "18A642")
(def ryan   "18A63C")
(def eric   "18A631")
(def norb   "18A639")

(defn- make-new-athlete
  "Make the structure for a new athlete"
  [id]
  (hash-map
    :id id                ;; unique ID
    :name nil             ;; athlete name is empty and "unclaimed" by default
    :stats                ;; Statistics
      (hash-map
        :wins 0           ;; athlete wins
        :losses 0         ;; athlete losses
        :ties 0)))        ;; athlete ties

(defmacro with-name
  [name id]
  `(assoc (make-new-athlete ~id) :name ~name))

(def hard-coded-players
  (hash-map
    ian   (with-name "IAN" ian)
    mikel (with-name "MIKE L" mikel)
    mikes (with-name "MIKE S" mikes)
    tim   (with-name "TIM" tim)
    ryan  (with-name "RYAN" ryan)
    eric  (with-name "ERIC" eric)
    norb  (with-name "NORB" norb)))

(def ^:dynamic *in-mem-db* (atom hard-coded-players))

;;;;;;;;;;
;; Avatars
;;;;;;;;;;

;; Avatar icon names will follow the convention
;;    avatar-face-icon-#.png
;; where # is a number starting at 1 and a sequential increment from
;; the previous avatar icon name. When you add a new avatar, increment
;; number-of-avatars appropriately.
(def all-avatar-imgs
  (let [number-of-avatars 4]  ;; Change me if there are more avatars
    (take number-of-avatars
      (map #(str "/img/avatar-face-icon-" % ".png")
        (iterate inc 1)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Athlete convenience methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn athlete-name
  "Get the athlete's name. May be nil if the athlete is not claimed"
  [athlete]
  (get athlete :name))

(defn athlete-claimed?
  "Returns true if the athlete is claimed"
  [athlete]
  (not (nil? (athlete-name athlete))))

(defn athlete-wins
  "Get the athlete's win count"
  [athlete]
  (get-in athlete [:stats :wins]))

(defn athlete-losses
  "Get the athlete's loss count"
  [athlete]
  (get-in athlete [:stats :losses]))

(defn athlete-ties
  "Get the athlete's tie count"
  [athlete]
  (get-in athlete [:stats :ties]))

(defn athlete-games
  "Get the total number of games played by athlete"
  [athlete]
  (let [[w l t] [(athlete-wins athlete)
                 (athlete-losses athlete)
                 (athlete-ties athlete)]]
    (+ w l t)))

;;;;;;;;;;;;;;;;;;;;;;
;; Persistence methods
;;;;;;;;;;;;;;;;;;;;;;

(defn lookup-athlete
  "Returns the athlete or nil if there is no athlete by that ID."
  [id]
  (get @*in-mem-db* id))

(defn create-athlete!
  "Create an athlete by ID id. If the ID exists, create-athlete! does nothing."
  [id]
  (when-not (lookup-athlete id)
    (swap! *in-mem-db* assoc id (make-new-athlete id))
    (lookup-athlete id)))

(defn delete-athlete!
  "Remove the athlete by ID id. Does nothing if the ID does not exist."
  [id]
  (swap! *in-mem-db* dissoc id))

(defn claim-athlete!
  "Claim an athlete with ID id by associating a name. Does nothing if the
  athlete is already claimed"
  [id name]
  (let [athlete       (lookup-athlete id)
        named-athlete (assoc athlete :name name)]
    (when (not (athlete-claimed? athlete))
      (swap! *in-mem-db* assoc id named-athlete))))

(defn- change-stats!
  "Update the stat for an athlete with ID id using a function f.
  Does nothing if the athlete does not exist."
  [id stat f]
    (when (lookup-athlete id)
      (swap! *in-mem-db* update-in [id :stats stat] f)))

(defn win-for!
  "Add a win for an athlete with ID id"
  [id]
  (change-stats! id :wins inc))

(defn loss-for!
  "Add a loss for an athlete with ID id"
  [id]
  (change-stats! id :losses inc))

(defn tie-for!
  "Add a tie for an athlete with ID id"
  [id]
  (change-stats! id :ties inc))

;;;;;;;;;;;;;;;;;;;
;; Athlete querying
;;;;;;;;;;;;;;;;;;;

(defn athletes-who?
  "Returns a collection of all athletes who match a predicate function. Consider
  composing equality functions with the athlete accessors defined above to
  create other queries.
  
  The returned collection is a lazy seq returned from filter, allowing for
  efficient composition of queries. Use :as-map as an optional argument to
  immediately aggregate the results into a map.
  
  This is just so Ian can practice optional arguments."
  [pred & opts]
  (let [table     @*in-mem-db*
        aggregate (if (some #{:as-map} opts)
                      (partial into {})
                      identity)]
    (aggregate (filter (comp pred second) table))))

(def are-claimed
  "Prefabricated query for use with athletes-who? Returns the claimed athletes."
  athlete-claimed?)

(def are-unclaimed
  "Prefabricated query for use with athletes-who? Returns the unclaimed
  athelets."
  (comp not are-claimed))