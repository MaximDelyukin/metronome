(ns ^:figwheel-always metronome.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def ctx (js/AudioContext.))

(def DEFAULT_TEMPO 60)
(def DURATION_OF_TICK .1)
(def app-state (atom {:tempo DEFAULT_TEMPO :osc nil :intervalId nil}))

(defn
	increaseTempo 
 	[]
	(swap! app-state assoc :tempo (+ 1 (get @app-state :tempo))))

(defn 
  decreaseTempo
  []
  (swap! app-state assoc :tempo (- (get @app-state :tempo) 1)))

(defn 
  playSingleTick
  []
  (swap! app-state assoc :osc (.createOscillator ctx))
  (def osc (get @app-state :osc))
  (.connect osc (.-destination ctx))
  (.start osc)
  (.stop osc (+ DURATION_OF_TICK (.-currentTime ctx)))
)

(def MILLISECONDS_IN_MINUTE 60000)
(defn 
  calculateIntervalBasedOnTempoValue
  []
  (/ MILLISECONDS_IN_MINUTE (get @app-state :tempo))
)

(defn 
  startCounting
  []
  (swap! app-state 
        assoc :intervalId (js/setInterval playSingleTick (calculateIntervalBasedOnTempoValue)))
)

(defn 
  stopCounting
  []
  (js/clearInterval (get @app-state :intervalId))
)

(om/root
  (fn [app owner]
    (om/component
    	(dom/div #js {:className "main"}
        (dom/button #js {:onClick increaseTempo} "^")
        (dom/button #js {:onClick decreaseTempo} "V") 
        (:tempo app)
        (dom/div nil 
          (dom/button #js {:onClick startCounting} "Play")
          (dom/button #js {:onClick stopCounting} "Stop")
        )
      )
    )
  )
  app-state
  {:target (. js/document (getElementById "app"))})


