(ns sprog.dev.bilinear-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.iglu.chunks.misc :refer [bilinear-usampler-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def u16-max (dec (Math/pow 2 16)))

(def pixel-data
  [u16-max 0 0 u16-max
   0 u16-max 0 u16-max
   0 0 u16-max u16-max
   0 0 0 u16-max])

(def nearest-frag-source
  (iglu->glsl
   {:constants {:u16-max-f (.toFixed u16-max 1)}}
   '{:version "300 es"
     :precision {float highp
                 usampler2D highp}
     :uniforms {size vec2
                tex usampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= fragColor (/ (vec4 (texture tex pos)) :u16-max-f)))}))

(def bilinear-frag-source
  (iglu->glsl
   {:constants {:u16-max-f (.toFixed u16-max 1)}}
   bilinear-usampler-chunk
   '{:version "300 es"
     :precision {float highp
                 usampler2D highp}
     :uniforms {size vec2
                offset vec2
                tex usampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ (- gl_FragCoord.xy offset) size))
            (= fragColor (/ (textureBilinear tex pos) :u16-max-f)))}))

(defn update-page! [{:keys [gl texture] :as state}]
  (with-context gl
    (maximize-gl-canvas)
    (let [[width height] (canvas-resolution)
          half-width (* width 0.5)]
      (run-purefrag-shader! nearest-frag-source
                            [half-width height]
                            {:floats {"size" [half-width height]}
                             :textures {"tex" texture}})
      (run-purefrag-shader! bilinear-frag-source
                            [half-width 0 half-width height]
                            {:floats {"size" [half-width height]
                                      "offset" [half-width 0]}
                             :textures {"tex" texture}})))
  state)

(defn init-page! [gl]
  {:gl gl
   :texture (with-context gl
              (create-tex :u16
                          2
                          {:wrap-mode :clamp
                           :data (js/Uint16Array. pixel-data)}))})

(defn init []
  (start-sprog! init-page! update-page!))
