(ns metronome.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def ctx (if js/AudioContext
           (js/AudioContext.)
           (js/webkitAudioContext.)
         )
)

(def DEFAULT_TEMPO 96)
(def MAX_TEMPO 250)
(def MIN_TEMPO 1)
(def app-state (atom {:tempo DEFAULT_TEMPO 
                      :osc nil 
                      :isCurrentlyTicking false 
                      :counter 0 
                      :accentBarStart true}))

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

(defn accentBarStart 
  []
  (get @app-state :accentBarStart)
)

(defn playLoop
	[]
	(if (get @app-state :osc)
    (.disconnect (get @app-state :osc))
	)
  (def newOsc (.createOscillator ctx))
  (def oldCounter (get @app-state :counter))
  (def isFirstNoteOfTheBar (or (= oldCounter 0) (= (mod oldCounter 4) 0)))
  (set! (.-value (.-frequency newOsc)) (if (and isFirstNoteOfTheBar (accentBarStart)) 700 400))
  (swap! app-state assoc :counter (+ 1 oldCounter))
	(swap! app-state assoc :osc newOsc)
	(def osc (get @app-state :osc))
	(.connect osc (.-destination ctx))
 	(def currTime (.-currentTime ctx))
  (set! (.-onended osc) playLoop)
	(.start osc (+ currTime (whenToPlay)))
	(.stop osc (+ currTime (whenToPlay) DURATION_OF_TICK_IN_SECONDS))
)

(defn startTicking
	[]
	(playLoop)
	(setCurrentlyTicking)
)

(defn resetCounter
  []
  (swap! app-state assoc :counter 0)
)

(defn stopTicking
	[]
 	(set! (.-onended osc) nil)
	(.stop (get @app-state :osc))
	(.disconnect (get @app-state :osc))
	(setCurrentlyNotTicking)
  (resetCounter)
)

(defn playStopButtonClickHandler
 	[]
  (if (isCurrentlyTicking)
    (stopTicking)
    (startTicking)
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
  (if (< 0 (.-deltaY e))
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

(defn toggleAccentBarStart
  [e]
  (swap! app-state assoc :accentBarStart (not (accentBarStart)))
  (set! (.-checked (.-target e)) (not (.-checked (.-target e))))
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
           			#js {:onClick inCreaseTempoButtonClickHandler :className "change-tempo change-tempo-tempo-increase"} 
          		)
	        	(dom/button 
           			#js {:onClick decreaseTempoButtonClickHandler :className "change-tempo change-tempo-tempo-decrease"} 
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
          (dom/div #js {:className "row"} 
            (dom/label
              nil
              "Accent bar start "
              (dom/input
                #js {:type "checkbox" :onChange toggleAccentBarStart :checked (:accentBarStart app)}
              )  
            )
          )
    	)
    )
  )
  app-state
  {:target (. js/document (getElementById "app"))})
