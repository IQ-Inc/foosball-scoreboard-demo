(ns foosball-score.persistence)

(defonce ^:private in-mem-db (atom {}))

(defn lookup-player-by-id
  "Returns the player's name, or nil if it does not exist"
  [id]
  (get @in-mem-db id))

(defn add-player!
  "Add a player by id and name"
  [id name]
  (swap! in-mem-db assoc id name))