;TODO: use input type with min and max attributes from MIN_TEMPO to MAX_TEMPO or some range macro or proxy
(ns metronome.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def ctx (if js/AudioContext
           (js/AudioContext.)
           (js/webkitAudioContext.)
         )
)

(def DEFAULT_TEMPO 90)
(def MAX_TEMPO 250)
(def MIN_TEMPO 1)
(def app-state (atom {:tempo DEFAULT_TEMPO :osc nil :isCurrentlyTicking false}))

(defn increaseTempo 
 	[]
	(swap! app-state assoc :tempo (+ 1 (get @app-state :tempo)))
)

(defn decreaseTempo
	[]
	(swap! app-state assoc :tempo (- (get @app-state :tempo) 1)))

(def DURATION_OF_TICK_IN_SECONDS .025)
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
    	0
    )	
)

(defn playLoop
	[]
	(if (get @app-state :osc)
    	(.disconnect (get @app-state :osc))
	)
  (def newOsc (.createOscillator ctx))
  (set! (.-value (.-frequency newOsc)) 400)
	(swap! app-state assoc :osc newOsc)
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
  (if (> MAX_TEMPO (get @app-state :tempo))
    (increaseTempo)
  )
)

(defn
 	decreaseTempoButtonClickHandler
  []
  (if (< MIN_TEMPO (get @app-state :tempo))
    (decreaseTempo)
  )
)

(defn
  mouseWheelHandler
  [e]
  (def tempo (get @app-state :tempo))
  (if (> 0 (.-wheelDelta (.-nativeEvent e)))
    (if (< MIN_TEMPO tempo)
      (decreaseTempo)
    )
    (if (> MAX_TEMPO tempo)
      (increaseTempo)
    )
  )
)

(defn onInputHandler
  [e]
  (def value (.-value (.-target (.-nativeEvent e))))
  (if (and (< MIN_TEMPO value) (> MAX_TEMPO value))
    (swap! app-state assoc :tempo value)
    (if (< MIN_TEMPO value)
       (swap! app-state assoc :tempo MAX_TEMPO)
       (swap! app-state assoc :tempo MIN_TEMPO)    
    )
  )
)

(om/root
  (fn [app owner]
    (om/component
    	(dom/div #js {:className "main" :onWheel mouseWheelHandler}
        	(dom/div #js {:className "row"} 
            (dom/input #js {:value (:tempo app) :type "text" :className "tempo" :onInput onInputHandler})
          )
			(dom/div #js {:className "row"} 
     			(dom/button 
           			#js {:onClick inCreaseTempoButtonClickHandler :className "change-tempo tempo-increase"} 
          		)
	        	(dom/button 
           			#js {:onClick decreaseTempoButtonClickHandler :className "change-tempo tempo-decrease"} 
          		)
          	) 
	        (dom/div #js {:className "row"} 
	        	(dom/button 
            	#js {
                  :onClick playStopButtonClickHandler 
                  :className (if (isCurrentlyTicking) 
                               "change-state ticking" 
                               "change-state not-ticking"
                             )
                  } 
            )
	        )
    	)
    )
  )
  app-state
  {:target (. js/document (getElementById "app"))})
