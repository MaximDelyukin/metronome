(ns ^:figwheel-always metronome.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def ctx (js/AudioContext.))

(def DEFAULT_TEMPO 110)
(def app-state (atom {:tempo DEFAULT_TEMPO :osc nil :isCurrentlyTicking false}))

(defn increaseTempo 
 	[]
	(swap! app-state assoc :tempo (+ 1 (get @app-state :tempo)))
)

(defn decreaseTempo
	[]
	(swap! app-state assoc :tempo (- (get @app-state :tempo) 1)))

(def DURATION_OF_TICK_IN_SECONDS .05)
(def SECONDS_IN_MINUTE 60)

(defn calculateIntervalBetweenTicks
	[]
	(- (/ SECONDS_IN_MINUTE (get @app-state :tempo)) DURATION_OF_TICK_IN_SECONDS)
)

(defn isCurrentlyTicking
	[]
	(get @app-state :isCurrentlyTicking)
)

(defn setCurrentlyTicking
	[]
	(swap! app-state assoc :isCurrentlyTicking true)
)

(defn setCurrentlyNotTicking
	[]
	(swap! app-state assoc :isCurrentlyTicking false)
)

(defn whenToPlay
	[]
 	(if (isCurrentlyTicking)
    	(calculateIntervalBetweenTicks)
    	.04;for some reason firefox chunks the start of the sound if there is no delay
    )	
)

(defn playLoop
	[]
	(if (get @app-state :osc)
    	(.disconnect (get @app-state :osc))
	)
	(swap! app-state assoc :osc (.createOscillator ctx))
	(def osc (get @app-state :osc))
	(.connect osc (.-destination ctx))
 	(def currTime (.-currentTime ctx))
  	(set! (.-onended osc) playLoop)
	(.start osc (+ currTime (whenToPlay)))
	(.stop osc (+ currTime (whenToPlay) DURATION_OF_TICK_IN_SECONDS))
)

(defn startCounting
	[]
	(playLoop)
	(setCurrentlyTicking)
)

(defn stopCounting
	[]
 	(set! (.-onended osc) nil)
	(.stop (get @app-state :osc))
	(.disconnect (get @app-state :osc))
	(setCurrentlyNotTicking)
)

(defn playStopButtonClickHandler
 	[]
  	(if (isCurrentlyTicking)
     	(stopCounting)
     	(startCounting)
    )
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
                  		"\u25a0"
                    	"\u25ba") 
            	)
	        )
    	)
    )
  )
  app-state
  {:target (. js/document (getElementById "app"))})


