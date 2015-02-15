(ns ^:figwheel-always metronome.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def ctx (js/AudioContext.))

(def DEFAULT_TEMPO 110)
(def app-state (atom {:tempo DEFAULT_TEMPO :osc nil :intervalId nil :isCurrentlyTicking false}))

(defn
	increaseTempo 
 	[]
	(swap! app-state assoc :tempo (+ 1 (get @app-state :tempo))))

(defn 
  decreaseTempo
  []
  (swap! app-state assoc :tempo (- (get @app-state :tempo) 1)))

(def DURATION_OF_TICK .05)
(defn 
  playSingleTick
  []
  (if (get @app-state :osc)
    (.disconnect (get @app-state :osc));to refactor
  )
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
  isCurrentlyTicking
  []
  (get @app-state :isCurrentlyTicking)
)

(defn 
  setCurrentlyTicking
  []
  (swap! app-state assoc :isCurrentlyTicking true)
)

(defn 
  setCurrentlyNotTicking
  []
  (swap! app-state assoc :isCurrentlyTicking false)
)

(defn 
  startCounting
  []
  (playSingleTick)
  (swap! app-state 
        assoc :intervalId (js/setInterval playSingleTick (calculateIntervalBasedOnTempoValue)))
  (setCurrentlyTicking)
)

(defn 
  stopCounting
  []
  (js/clearInterval (get @app-state :intervalId))
  (setCurrentlyNotTicking)
)

(defn
	playStopButtonClickHandler
 	[]
  	(if (isCurrentlyTicking)
     	(stopCounting)
     	(startCounting))
)

(defn
	inCreaseTempoButtonClickHandler 
	[]
 	(if (isCurrentlyTicking)
    	(
      		(stopCounting)
      		(increaseTempo)
    		(startCounting)
     	)
     	(increaseTempo)
    )
)

(defn
 	decreaseTempoButtonClickHandler
  	[]
  	(if (isCurrentlyTicking)
    	(
			(stopCounting)
	        (decreaseTempo)
	        (startCounting)    		
      	)
    	(decreaseTempo) 
    )
)

(om/root
  (fn [app owner]
    (om/component
    	(dom/div #js {:className "main"}
        	(dom/div #js {:className "row tempo"} 
                (:tempo app)
            )
			(dom/div #js {:className "row"} 
     			(dom/button 
           			#js {:onClick inCreaseTempoButtonClickHandler} 
	        		"+"
          		)
	        	(dom/button 
           			#js {:onClick decreaseTempoButtonClickHandler} 
	        		"\u2212"
          		)
          	) 
	        (dom/div #js {:className "row"} 
	        	(dom/button 
            		#js {:onClick playStopButtonClickHandler}
              		(if (isCurrentlyTicking)
                  		"Stop"
                    	"Play") 
            	)
	        )
    	)
    )
  )
  app-state
  {:target (. js/document (getElementById "app"))})


