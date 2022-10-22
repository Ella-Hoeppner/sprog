(ns sprog.dev.webcam-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [create-f8-tex
                                          copy-html-image-data!
                                          create-webcam-video-element]]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        square-maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.chunks.noise :refer [simplex-3d-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [clojure.walk :refer [postwalk-replace]]))

(def tex-count 16)

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))
(defonce texs-atom (atom nil))
(defonce video-element-atom (atom nil))
(defonce time-updated?-atom (atom nil))

(def frag-source
  (iglu->glsl
   {:TAU (.toFixed (* Math/PI 2) 12)}
   simplex-3d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}}
   {:uniforms (zipmap (map #(symbol (str "tex" %)) (range tex-count))
                      (repeat 'sampler2D))
    :functions {'main
                (concat
                 '([]
                   (=vec2 pos (/ gl_FragCoord.xy size))
                   (=vec2 texPos (vec2 pos.x (- "1.0" pos.y)))
                   (=float index
                           (* "0.5"
                              (+ "1.0"
                                 (snoise3D (vec3 (+ (* texPos
                                                       "7.0")
                                                    (vec2 100 -80))
                                                 time)))))
                   #_(=float index (* (+ (+ (cos (* :TAU texPos.x "4.0"))
                                            (sin (* :TAU texPos.y "1.0")))
                                         "2.0")
                                      "0.25")))
                 (postwalk-replace
                  {:threshold (.toFixed (/ tex-count) 12)}
                  '(("if" (< index :threshold)
                          (= fragColor
                             (texture tex0 texPos)))))
                 (map (fn [i]
                        (postwalk-replace
                         {:threshold (.toFixed (/ i tex-count) 12)
                          :tex (symbol (str "tex" i))}
                         '("else if" (< index :threshold)
                                     (= fragColor
                                        (texture :tex texPos)))))
                      (range 1 (dec tex-count)))
                 (postwalk-replace
                  {:tex (symbol (str "tex" (dec tex-count)))}
                  '(("else"
                     (= fragColor (texture :tex texPos))))))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-gl-canvas gl)
    (when @time-updated?-atom
      (swap! texs-atom
             #(->> %
                   cycle
                   (drop 1)
                   (take tex-count)))
      (copy-html-image-data! gl (first @texs-atom) @video-element-atom))
    (target-screen! gl)
    (run-purefrag-sprog @sprog-atom
                        resolution
                        {:floats {"size" resolution
                                  "time" (u/seconds-since-startup)}
                         :textures
                         (into {} (map (fn [tex index]
                                         [(str "tex" index) tex])
                                       @texs-atom
                                       (range)))})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (create-webcam-video-element
   (fn [video]
     (let [gl (create-gl-canvas)]
       (reset! gl-atom gl)
       (reset! texs-atom (u/gen tex-count (create-f8-tex gl 1)))
       (reset! sprog-atom (create-purefrag-sprog gl frag-source))
       (.addEventListener video "timeupdate" #(reset! time-updated?-atom true))
       (reset! video-element-atom video)
       (update-page!)))))
