(ns foosball-score.serial-test
  (:require
    [clojure.test :refer :all]
    [foosball-score.serial :refer [serial-message-accumulate]]
    [clojure.java.io :as io]))

(defn- str->input-stream
  [msg]
  (io/input-stream
    (byte-array
      (map byte (conj (vec msg) \newline)))))

(deftest serial-message-accumulate-test

  (testing "A serial message of one character"
    (let [input (str->input-stream "A")]
      (is (= "A" (serial-message-accumulate input)))))
      
  (testing "A serial message of multiple characters"
    (let [input (str->input-stream "Hello world")]
      (is (= "Hello world" (serial-message-accumulate input)))))
      
  (testing "An empty serial message"
    (is (= ""
          (serial-message-accumulate
            (io/input-stream (byte-array [])))))))