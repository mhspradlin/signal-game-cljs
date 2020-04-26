(ns ^:figwheel-hooks signal-game-cljs.core
  (:require
   [goog.dom :as gdom]
   [cljs.core.async :refer [chan poll! put!]]
   [clojure.string :refer [join]]
   [d3 :as d3]))

(defn get-app-element []
  (gdom/getElement "app"))

(def window-width 700)
(def window-height 500)
(def ship-speed 0.05)
(def ping-speed (* 3 ship-speed))
(def portal-radius 7)

(defn draw-background
  [svg]
  (-> svg
      (.append "rect")
        (.attr "width" window-width)
        (.attr "height" window-height)
        (.style "stroke" "black")
        (.style "fill" "black")))

(def level-7-state
  {:signal-tower-pos {:x 150 :y 105}
   :ship             {:x 150 :y 150 :vector {:v 0 :theta (/ Math/PI -2)}}
   :pings            []
   :goal             {:x 150 :y 40}
   :walls            [{:x0 50 :y0 50 :x1 50 :y1 250}
                      {:x0 50 :y0 50 :x1 140 :y1 50}
                      {:x0 160 :y0 50 :x1 250 :y1 50}
                      {:x0 250 :y0 50 :x1 250 :y1 250}
                      {:x0 50 :y0 250 :x1 250 :y1 250}
                      {:x0 70 :y0 70 :x1 230 :y1 70}
                      {:x0 70 :y0 140 :x1 230 :y1 140}
                      {:x0 70 :y0 160 :x1 140 :y1 160}
                      {:x0 160 :y0 160 :x1 230 :y1 160}
                      {:x0 70 :y0 230 :x1 140 :y1 230}
                      {:x0 160 :y0 230 :x1 230 :y1 230}
                      {:x0 70 :y0 70 :x1 70 :y1 140}
                      {:x0 230 :y0 70 :x1 230 :y1 140}
                      {:x0 70 :y0 160 :x1 70 :y1 230}
                      {:x0 140 :y0 160 :x1 140 :y1 230}
                      {:x0 230 :y0 160 :x1 230 :y1 230}
                      {:x0 160 :y0 160 :x1 160 :y1 230}
                      ]
   :portals          {:a {:x 100 :y 150 :dest :b}
                      :b {:x 150 :y 240 :dest :a}
                      :c {:x 240 :y 150 :dest :d}
                      :d {:x 150 :y 60 :dest :c}}
   :next-level       nil})
(def level-6-state
  {:signal-tower-pos {:x 100 :y 250}
   :ship             {:x 100 :y 225 :vector {:v 0 :theta (/ Math/PI -2)}}
   :pings            []
   :goal             {:x 120 :y 250}
   :walls            [{:x0 90 :y0 145 :x1 90 :y1 260}
                      {:x0 90 :y0 145 :x1 225 :y1 145}
                      {:x0 110 :y0 165 :x1 205 :y1 165}
                      {:x0 110 :y0 240 :x1 205 :y1 240}
                      {:x0 90 :y0 260 :x1 225 :y1 260}
                      {:x0 225 :y0 145 :x1 225 :y1 260}
                      {:x0 110 :y0 165 :x1 110 :y1 260}
                      {:x0 205 :y0 165 :x1 205 :y1 240}
                      ]
   :portals          {:a {:x 100 :y 155 :dest :b}
                      :b {:x 215 :y 250 :dest :a}}
   :next-level       level-7-state})
(def level-5-state
  {:signal-tower-pos {:x 100 :y 250}
   :ship             {:x 125 :y 250 :vector {:v 0 :theta 0}}
   :pings            []
   :goal             {:x 100 :y 50}
   :walls            [{:x0 75 :y0 30 :x1 75 :y1 70}
                      {:x0 75 :y0 30 :x1 170 :y1 30}
                      {:x0 75 :y0 70 :x1 130 :y1 70}
                      {:x0 130 :y0 70 :x1 130 :y1 105}
                      {:x0 130 :y0 105 :x1 170 :y1 105}
                      {:x0 170 :y0 30 :x1 170 :y1 105}
                      {:x0 75 :y0 230 :x1 75 :y1 270}
                      {:x0 75 :y0 230 :x1 130 :y1 230}
                      {:x0 75 :y0 270 :x1 170 :y1 270}
                      {:x0 170 :y0 270 :x1 170 :y1 205}
                      {:x0 130 :y0 205 :x1 170 :y1 205}
                      {:x0 130 :y0 230 :x1 130 :y1 205}
                      ]
   :portals          {:a {:x 150 :y 225 :dest :b}
                      :b {:x 150 :y 85 :dest :a}}
   :next-level       level-6-state})
(def level-4-state
  {:signal-tower-pos {:x 200 :y 275}
   :ship             {:x 200 :y 250 :vector {:v 0 :theta (* 1.5 Math/PI)}}
   :pings            []
   :goal             {:x 200 :y 50}
   :walls            [{:x0 100 :y0 0 :x1 100 :y1 300}
                      {:x0 300 :y0 0 :x1 300 :y1 300}
                      {:x0 100 :y0 175 :x1 300 :y1 175}
                      {:x0 100 :y0 0 :x1 300 :y1 0}
                      {:x0 100 :y0 300 :x1 300 :y1 300}]
   :portals          {:a {:x 200 :y 200 :dest :b}
                      :b {:x 200 :y 150 :dest :a}}
   :next-level       level-5-state})
(def level-3-state
  {:signal-tower-pos {:x 200 :y 275}
   :ship {:x 200 :y 250 :vector {:v 0 :theta (* 1.5 Math/PI)}}
   :pings []
   :goal {:x 200 :y 50}
   :walls [{:x0 100 :y0 0 :x1 100 :y1 300}
           {:x0 300 :y0 0 :x1 300 :y1 300}
           {:x0 100 :y0 150 :x1 250 :y1 150}
           {:x0 150 :y0 200 :x1 300 :y1 200}
           {:x0 150 :y0 100 :x1 300 :y1 100}
           {:x0 100 :y0 0 :x1 300 :y1 0}
           {:x0 100 :y0 300 :x1 300 :y1 300}]
   :next-level level-4-state})
(def level-2-state
  {:signal-tower-pos {:x 300 :y 275}
   :ship {:x 300 :y 250 :vector {:v 0 :theta (* 1.5 Math/PI)}}
   :pings []
   :walls [{:x0 0 :y0 50 :x1 400 :y1 50}
           {:x0 0 :y0 150 :x1 200 :y1 150}
           {:x0 200 :y0 150 :x1 200 :y1 300}
           {:x0 400 :y0 50 :x1 400 :y1 300}]
   :goal {:x 100 :y 100}
   :next-level level-3-state})
(def level-1-state
  {:signal-tower-pos {:x 200 :y 275}
   :ship {:x 200 :y 250 :vector {:v 0 :theta (* 1.5 Math/PI)}}
   :pings []
   :goal {:x 200 :y 50}
   :walls [{:x0 100 :y0 0 :x1 100 :y1 300}
           {:x0 300 :y0 0 :x1 300 :y1 300}
           {:x0 100 :y0 175 :x1 175 :y1 175}
           {:x0 225 :y0 175 :x1 300 :y1 175}
           {:x0 100 :y0 0 :x1 300 :y1 0}
           {:x0 100 :y0 300 :x1 300 :y1 300}]
   :next-level level-2-state})

(def new-direction-pings (chan 10))

(defn enter-signal-tower
  [enter]
  (-> enter
      (.append "g")
        (.classed "signal-tower" true)
        (.append "circle")
          (.attr "r" 5)
          (.attr "cx" (fn [d] (.-x d)))
          (.attr "cy" (fn [d] (.-y d)))
          (.attr "fill" "white")))

(defn update-signal-tower
  [update]
  (-> update
      (.select "circle")
        (.attr "cx" (fn [d] (.-x d)))
        (.attr "cy" (fn [d] (.-y d)))))

(defn draw-signal-tower
  [svg state]
  (let [{tower-pos :signal-tower-pos} state]
    (-> svg
        (.selectAll ".signal-tower")
        (.data (clj->js [tower-pos]))
        (.join enter-signal-tower update-signal-tower))))

(defn enter-ship
  [enter]
  (-> enter
      (.append "g")
        (.classed "ship" true)
        (.append "polygon")
          (.attr "points" (join "," [0 0
                                     10 5
                                     0 10]))
          (.attr "fill" "white")))

(defn update-ship
  [update]
  (-> update
      (.attr "transform"
             (fn [d] (str "translate(" (- (.-x d) 5) " " (- (.-y d) 5) ")"
                          "rotate(" (* (.-theta (.-vector d)) (/ 360 (* 2 Math/PI)))
                                     " " 5 " " 5 ")")))))

(defn draw-ship
  [svg state]
  (let [{ship :ship} state]
    (-> svg
        (.selectAll ".ship")
        (.data (clj->js [ship]))
        (.join enter-ship update-ship))))

(defn enter-pings
  [enter]
  (-> enter
      (.append "g")
        (.classed "ping" true)
        (.append "circle")
          (.attr "r" (fn [d] (.-r d)))
          (.attr "cx" (fn [d] (.-cx d)))
          (.attr "cy" (fn [d] (.-cy d)))
          (.attr "fill" "none")
          (.attr "stroke" "white")
          (.attr "stroke-width" 2)))

(defn update-pings
  [update]
  (-> update
      (.select "circle")
        (.attr "r" (fn [d] (.-r d)))))

(defn draw-pings
  [svg state]
  (let [{pings :pings
         signal-tower-pos :signal-tower-pos} state
        svg-pings (->> pings
                       (map #(select-keys % [:r]))
                       (map #(assoc % :cx (:x signal-tower-pos)
                                      :cy (:y signal-tower-pos))))]
    (-> svg
        (.selectAll ".ping")
        (.data (clj->js svg-pings))
        (.join enter-pings update-pings))))

(defn enter-goal
  [enter]
  (-> enter
      (.append "g")
      (.classed "goal" true)
      (.append "rect")
        (.attr "x" 0)
        (.attr "y" 0)
        (.attr "width" 15)
        (.attr "height" 15)
        (.attr "fill" "white")))

(defn update-goal
  [update]
  (-> update
      (.attr "transform"
             (fn [d]
               (str "translate(" (- (.-x d) 7.5) " " (- (.-y d) 7.5) ")"
                 "rotate(" (mod (/ (.now js/performance) 30) 360)
                           " " 7.5 " " 7.5 ")")))))

(defn draw-goal
  [svg state]
  (-> svg
      (.selectAll ".goal")
      (.data (clj->js [(:goal state)]))
      (.join enter-goal update-goal)))

(defn enter-walls
  [enter]
  (-> enter
      (.append "g")
      (.classed "wall" true)
      (.append "line")
        (.attr "x1" (fn [d] (.-x0 d)))
        (.attr "y1" (fn [d] (.-y0 d)))
        (.attr "x2" (fn [d] (.-x1 d)))
        (.attr "y2" (fn [d] (.-y1 d)))
        (.attr "stroke-width" 2)
        (.attr "stroke" "white")))

(defn update-walls
  [update]
  (-> update
      (.select "line")
        (.attr "x1" (fn [d] (.-x0 d)))
        (.attr "y1" (fn [d] (.-y0 d)))
        (.attr "x2" (fn [d] (.-x1 d)))
        (.attr "y2" (fn [d] (.-y1 d)))))

(defn draw-walls
  [svg state]
  (-> svg
      (.selectAll ".wall")
      (.data (clj->js (:walls state)))
      (.join enter-walls update-walls)))

(defn enter-portals
  [enter]
  (-> enter
      (.append "g")
      (.classed "portal" true)
      (.append "circle")
        (.attr "r" portal-radius)
        (.attr "cx" (fn [d] (.-x d)))
        (.attr "cy" (fn [d] (.-y d)))
        (.attr "stroke-width" 2)
        (.attr "stroke" "white")))

(defn update-portals
  [update]
  (-> update
      (.select "circle")
        (.attr "cx" (fn [d] (.-x d)))
        (.attr "cy" (fn [d] (.-y d)))))

(defn draw-portals
  [svg state]
  (-> svg
      (.selectAll ".portal")
      (.data (clj->js (vals (:portals state))))
      (.join enter-portals update-portals)))

(defn draw-state
  [svg state]
  (-> svg
      (.call #(draw-signal-tower % state))
      (.call #(draw-ship % state))
      (.call #(draw-pings % state))
      (.call #(draw-goal % state))
      (.call #(draw-walls % state))
      (.call #(draw-portals % state))))

(defn tick-ping
  [ms-diff ping]
  {:prev ping
   :curr (update ping :r + (* ms-diff ping-speed))})

(defn get-first-collision?
  [ping-ticks prev-ship-dist curr-ship-dist]
  (reduce #(when (and
                   (< (:r (:prev %2)) prev-ship-dist)
                   (> (:r (:curr %2)) curr-ship-dist))
             (reduced (:dv (:curr %2))))
          nil
          ping-ticks))

(defn drop-off-screen-pings
  [pings]
  (filter #(< (:r %) (* (max window-width window-height) 2)) pings))

; Returns {:pings new-pings :dv change-to-vector}
(defn tick-pings
  [ms-diff pings prev-ship-dist curr-ship-dist]
  (let [ping-ticks (map #(tick-ping ms-diff %) pings)
        dv? (get-first-collision? ping-ticks prev-ship-dist curr-ship-dist)
        dv (if (nil? dv?) identity dv?)
        new-pings (->> ping-ticks
                       (map :curr)
                       drop-off-screen-pings)]
    {:pings new-pings :dv dv}))

(defn move-ship
  [ms-diff ship]
  (let [v (:v (:vector ship))
        theta (:theta (:vector ship))
        magnitude-change (* ms-diff v)]
    (assoc ship
      :x (+ (:x ship) (* (Math/cos theta) magnitude-change))
      :y (+ (:y ship) (* (Math/sin theta) magnitude-change)))))

(defn get-ship-dist
  [ship xyentity]
  (Math/sqrt (+ (Math/pow (- (:x ship) (:x xyentity)) 2)
                (Math/pow (- (:y ship) (:y xyentity)) 2))))

(defn ship-at-goal?
  [ship goal]
  (< (get-ship-dist ship goal) 10))

; See https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect
(defn ship-collided-with-wall?
  [prev-ship curr-ship wall]
  (let [ship-move-x-diff (- (:x curr-ship) (:x prev-ship))
        ship-move-y-diff (- (:y curr-ship) (:y prev-ship))
        wall-x-diff (- (:x1 wall) (:x0 wall))
        wall-y-diff (- (:y1 wall) (:y0 wall))
        s (/ (+ (* (- ship-move-y-diff) (- (:x prev-ship) (:x0 wall)))
                (* ship-move-x-diff (- (:y prev-ship) (:y0 wall))))
             (+ (* (- wall-x-diff) ship-move-y-diff)
                (* ship-move-x-diff wall-y-diff)))
        t (/ (- (* wall-x-diff (- (:y prev-ship) (:y0 wall)))
                (* wall-y-diff (- (:x prev-ship) (:x0 wall))))
             (+ (* (- wall-x-diff) ship-move-y-diff)
                (* ship-move-x-diff wall-y-diff)))]
    (and (>= s 0) (<= s 1) (>= t 0) (<= t 1))))

(defn ship-destroyed?
  [prev-ship curr-ship walls]
  (reduce #(when (ship-collided-with-wall? prev-ship curr-ship %2)
                 (reduced true))
          false
          walls))

; Returns :dest of first portal collision, if there is one
(defn first-ship-portal-collision?
  [prev-ship curr-ship portals-positions]
  (reduce #(when (and (< (get-ship-dist curr-ship %2) portal-radius)
                      (> (get-ship-dist prev-ship %2) portal-radius))
             (reduced (:dest %2)))
          nil
          portals-positions))

; Returns new ship position
(defn ship-teleported?
  [prev-ship curr-ship portals]
  (let [teleported-portal-id? (first-ship-portal-collision? prev-ship curr-ship (vals portals))]
    (if teleported-portal-id?
      (select-keys (teleported-portal-id? portals) [:x :y]))))

(defn update-game-state
  [last-tick current-tick old-state]
  (let [ms-diff (- current-tick last-tick)
        moved-ship (move-ship ms-diff (:ship old-state))
        ping-updates (tick-pings ms-diff
                                 (:pings old-state)
                                 (get-ship-dist (:ship old-state)
                                                (:signal-tower-pos old-state))
                                 (get-ship-dist moved-ship
                                                (:signal-tower-pos old-state)))
        turned-ship (assoc moved-ship
                      :vector
                      ((:dv ping-updates) (:vector moved-ship)))
        teleported-ship-pos? (ship-teleported? (:ship old-state) turned-ship (:portals old-state))
        new-ship (if teleported-ship-pos?
                   (assoc turned-ship
                     :x (:x teleported-ship-pos?)
                     :y (:y teleported-ship-pos?))
                   turned-ship)
        new-state (if (ship-at-goal? moved-ship (:goal old-state))
                    (assoc (:next-level old-state)
                           :initial-state (:next-level old-state))
                    (assoc old-state
                      :ship new-ship
                      :pings (:pings ping-updates)))]
    (when teleported-ship-pos?
      (.info js/console (str teleported-ship-pos?)))
    (if (ship-destroyed? (:ship old-state) moved-ship (:walls old-state))
      (do
        (assoc (:initial-state old-state)
          :initial-state (:initial-state old-state)))
      new-state)))

(defn update-game-svg
  [svg state]
  (draw-state svg state))

(defn update-game
  [el game-state prev-timestamp curr-timestamp]
  (let [new-state (update-game-state prev-timestamp curr-timestamp game-state)]
    (update-game-svg (-> (d3/select el) (.select "svg")) new-state)
    new-state))

(defn add-ping
  [ping? game-state]
  (let [prev-pings (:pings game-state)
        pings (if (nil? ping?)
                prev-pings
                (conj prev-pings {:dv (:dv (:command ping?)) :r 0}))]
    (assoc game-state :pings pings)))

(defn render-game
  [el state]
  (do
    (-> (d3/select el)
        (.append "svg")
          (.attr "viewBox" (str "0 0 " 400 " " 300))
          (.attr "width" window-width)
          (.attr "height" window-height)
          (.style "margin" 0)
          (.style "top" "50%")
          (.style "left" "50%")
          (.style "position" "absolute")
          (.style "transform" "translate(-50%, -50%)")
        (.call draw-background)
        (.call #(draw-state % state)))))

(defn animate-frame
  ([el init-state]
    (let [now (.now js/performance)]
      (.requestAnimationFrame js/window #(animate-frame el init-state now %))))
  ([el state prev-timestamp curr-timestamp]
    (as-> state x
          (add-ping (poll! new-direction-pings) x)
          (update-game el x prev-timestamp curr-timestamp)
          (.requestAnimationFrame js/window #(animate-frame el x curr-timestamp %)))))

(defn rotate-vector
  [dtheta vector]
  (update vector :theta + dtheta))
(defn stop-vector
  [vector]
  (assoc vector :v 0))
(defn start-vector
  [vector]
  (assoc vector :v ship-speed))
(def arrow-keycodes-commands
  {37 {:dv #(rotate-vector (/ Math/PI -2) %)}               ; Left arrow
   38 {:dv start-vector}                                    ; Up arrow
   39 {:dv #(rotate-vector (/ Math/PI 2) %)}                ; Right arrow
   40 {:dv stop-vector}})                                   ; Down arrow
(defn handle-keypress
  [event]
  (if (contains? arrow-keycodes-commands (.-keyCode event))
    (let [now (.now js/performance)
          command (get arrow-keycodes-commands (.-keyCode event))]
      (put! new-direction-pings {:occur-time now :command command}))))

(defn register-keypress-handlers
  []
  (-> (d3/select "body")
      (.on "keydown" #(handle-keypress (.-event d3)))))

(defn mount [el]
  (let [starting-state (assoc level-1-state :initial-state level-1-state)]
    (render-game el starting-state)
    (register-keypress-handlers)
    (animate-frame el starting-state)))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (-> d3 (.selectAll "svg") (.remove))
  (-> d3 (.select "body") (.on "keydown" nil))
  (mount-app-element)
)
