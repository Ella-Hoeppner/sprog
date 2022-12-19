(ns sprog.dev.transform-feedback-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-shaders!]]
            [sprog.webgl.attributes :refer [create-boj!]]
            [sprog.webgl.core :refer-macros [with-context]]))

(def pos-buffer-data [0 0
                      0.1 0
                      0 0.1])

(def color-buffer-data [1 0 0
                        0 1 0
                        0 0 1])

(defonce gl-atom (atom nil))

(defonce pos-bojs-atom (atom nil))
(defonce color-boj-atom (atom nil))

(def vert-source
  '{:version "300 es"
    :precision {float highp}
    :inputs {vertexPos vec2
             vertexColor vec3}
    :outputs {color vec3
              newVertexPos vec2}
    :uniforms {rotation mat2}
    :main ((= color vertexColor)
           (= newVertexPos (+ vertexPos (vec2 0.001 0)))
           (= gl_Position (vec4 (* vertexPos rotation) 0 1)))})

(def frag-source
  '{:version "300 es"
    :precision {float highp}
    :inputs {color vec3}
    :outputs {fragColor vec4}
    :main ((= fragColor (vec4 color 1)))})

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas {:square? true})
    (run-shaders! [vert-source frag-source]
                  (canvas-resolution)
                  {:matrices {"rotation"
                              (let [angle (u/seconds-since-startup)]
                                [(Math/cos angle) (- (Math/sin angle))
                                 (Math/sin angle) (Math/cos angle)])}}
                  {"vertexPos" (first @pos-bojs-atom)
                   "vertexColor" @color-boj-atom}
                  0
                  3
                  {:transform-feedback {"newVertexPos" (last @pos-bojs-atom)}})
    (js/requestAnimationFrame update-page!)
    #_(swap! pos-bojs-atom reverse))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (reset! pos-bojs-atom
            
            (u/gen 2
                   (create-boj! 2
                                {:initial-data 
                                 (js/Float32Array. pos-buffer-data)})))
    (reset! color-boj-atom
            (create-boj! 3
                         {:initial-data (js/Float32Array. color-buffer-data)}))
    (update-page!)))
