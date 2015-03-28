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
                      :accentBarStart true
                      :subDivision 4
                      :timeout nil}))

(defn increaseTempo 
  []
  (swap! app-state assoc :tempo (inc (get @app-state :tempo)))
)

(defn decreaseTempo
	[]
	(swap! app-state assoc :tempo (dec (get @app-state :tempo)))
)

(def DURATION_OF_TICK_IN_SECONDS .025)
(def SECONDS_IN_MINUTE 60)
(defn calculateIntervalBetweenTicks
  []
	(- (/ SECONDS_IN_MINUTE (* (get @app-state :tempo) (/ (get @app-state :subDivision) 4))) DURATION_OF_TICK_IN_SECONDS)
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

(defn setTempo
  [value]
  (swap! app-state assoc :tempo value)  
)

(defn playLoop
	[]
  (def oldOsc (get @app-state :osc))
	(if oldOsc
    (.disconnect oldOsc)
	)
  (def oldCounter (get @app-state :counter))
  (def isFirstNoteOfTheBar (or (= oldCounter 0) (= (mod oldCounter (get @app-state :subDivision)) 0)))
  (def newOsc (.createOscillator ctx))
  (set! (.-value (.-frequency newOsc)) (if (and isFirstNoteOfTheBar (accentBarStart)) 800 1000))
  (swap! app-state assoc :osc newOsc :counter (inc oldCounter))
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
)

(defn playStopButtonClickHandler
 	[]
  (if (isCurrentlyTicking)
    (do
      (setCurrentlyNotTicking)
      (stopTicking)
      (resetCounter)
    )
    (startTicking)
  )
)

(defn scheduleNext
  []
  (do
    (resetCounter)
    (js/clearTimeout (get @app-state :timeout))
    (stopTicking)
    (swap! app-state assoc :timeout (js/setTimeout 
        (fn 
          []
          (setCurrentlyNotTicking)
          (startTicking)
        ) 
        400)
    )
  )
)

(defn
  mouseWheelHandler
  [e]
  (if (and (< 0 (.-deltaY e)) (< MIN_TEMPO (get @app-state :tempo)))
    (decreaseTempo)
  )
  (if (and (> 0 (.-deltaY e)) (> MAX_TEMPO (get @app-state :tempo)))
    (increaseTempo)
  )
  (if (isCurrentlyTicking)
    (scheduleNext) 
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

(defn changeHandler
  [e]
  (swap! app-state assoc :tempo (js/parseInt (.-value (.-target e))))
  (if (isCurrentlyTicking)
    (scheduleNext) 
  )
)

(defn toggleAccentBarStart
  [e]
  (swap! app-state assoc :accentBarStart (not (accentBarStart)))
  (set! (.-checked (.-target e)) (not (.-checked (.-target e))))
)

(defn subDivisionChangeHandler
  [e]
  (swap! app-state assoc :subDivision (js/parseInt (.-value (.-target e))))
)

(om/root
  (fn [app owner]
    (om/component
    	(dom/div #js {:className "main"}
          (dom/div #js {:className "row"}
            (dom/label
              #js {
                    :className "accent-bar-start" 
                  }
              (dom/input
                #js {
                      :type "checkbox" 
                      :onChange toggleAccentBarStart 
                      :checked (:accentBarStart app)
                      :title "Accent bar start"
                    }
              )
              (dom/span
                nil
                "!" 
              )
            )          
            (dom/select
              #js {
                    :onChange subDivisionChangeHandler 
                    :value (:subDivision app) 
                    :className "subdivision"
                    :title "Subdivision"
                  }
              (dom/option
                #js {:value 4}
                "\u2669"
              )
              (dom/option
                #js {:value 8}
                "\u266A"
              )
              (dom/option
                #js {:value 16}
                "\u266C"
              )
            )
          )    
        	(dom/div #js {:className "row"} 
            (dom/input #js {:value (:tempo app) :type "text" :className "tempo" :onInput onInputHandler})
          )
          (dom/div #js {:className "row"} 
            (dom/input 
              #js {
                    :type "range"
                    :min 1
                    :max 250
                    :step 1
                    :value (:tempo app)
                    :onChange changeHandler
                    :onWheel mouseWheelHandler
                  } 
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
