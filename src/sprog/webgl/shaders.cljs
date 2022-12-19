(ns sprog.webgl.shaders
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.misc :refer [trivial-vert-source]]
            [sprog.webgl.uniforms :refer [set-sprog-uniforms!]]
            [sprog.webgl.textures :refer [target-textures!
                                          target-screen!]]
            [sprog.webgl.attributes :refer [set-sprog-attributes!
                                            set-sprog-attribute!
                                            create-boj!]]
            [clojure.string :refer [split-lines
                                    join]]))

(defn create-shader [gl shader-type source]
  (let [source-glsl
        (if (string? source)
          source
          (iglu->glsl source))
        shader (.createShader gl (or ({:frag gl.FRAGMENT_SHADER
                                       :vert gl.VERTEX_SHADER}
                                      shader-type)
                                     shader-type))]
    shader
    (.shaderSource gl shader source-glsl)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (do (u/log (join "\n"
                       (map #(str %2 ":\t" %1)
                            (split-lines source-glsl)
                            (rest (range)))))
          (throw (js/Error. (str (.getShaderInfoLog gl shader))))))))

(defn create-program [gl vert-shader frag-shader]
  (let [program (.createProgram gl)]
    (.attachShader gl program vert-shader)
    (.attachShader gl program frag-shader)
    (when-let [feedback-outputs (:transform-feedback-outputs vert-shader)]
      (.transformFeedbackVaryings program
                                  (clj->js feedback-outputs
                                           gl.SEPARATE_ATTRIBS)))
    (.linkProgram gl program)
    (if (.getProgramParameter gl program gl.LINK_STATUS)
      program
      (throw (js/Error. (str (.getProgramInfoLog gl program)))))))

(defn create-sprog [gl vert-source frag-source]
  (let [program (create-program gl
                                (create-shader gl :vert vert-source)
                                (create-shader gl :frag frag-source))]
    {:program program
     :uniforms-atom (atom {})
     :attributes-atom (atom {})}))

(def purefrag-vert-glsl (iglu->glsl trivial-vert-source))

(defn create-purefrag-sprog [gl frag-source] 
  (let [sprog (create-sprog gl purefrag-vert-glsl frag-source)]
    (set-sprog-attribute! gl
                          sprog
                          "vertPos"
                          (create-boj! gl
                                       2
                                       {:initial-data (js/Float32Array.
                                                       (clj->js [-1 -1
                                                                 -1 3
                                                                 3 -1]))}))
    sprog))

(defn use-sprog! [gl {:keys [program] :as sprog} uniform-map attribute-map]
  (.useProgram gl program)
  (set-sprog-uniforms! gl sprog uniform-map)
  (set-sprog-attributes! gl sprog attribute-map))

(def transform-feedback-outputs (memoize (comp sort keys)))

(defn run-sprog! [gl sprog size uniform-map attribute-map start length
                  & [{:keys [target transform-feedback]}]]
  (if target
    (if (coll? target)
      (apply (partial target-textures! gl) target)
      (target-textures! gl target))
    (target-screen! gl))
  (let [[offset-x offset-y width height]
        (cond (number? size) [0 0 size size]
              (= (count size) 2) (vec (concat [0 0] size))
              (= (count size) 4) (vec size))]
    (.viewport gl offset-x offset-y width height)
    (use-sprog! gl sprog uniform-map attribute-map)
    (when transform-feedback
      (.))
    (.drawArrays gl gl.TRIANGLES start length)))

(defn run-purefrag-sprog! [gl sprog size uniform-map & [options]]
  (run-sprog! gl
              sprog
              size
              uniform-map
              nil
              0
              3
              options))

(defonce autosprog-cache-atom (atom {}))

(defn get-autosprog [gl shader-sources]
  (let [autosprog-key [gl shader-sources]]
    (if-let [autosprog (@autosprog-cache-atom autosprog-key)]
      autosprog
      (let [autosprog (apply (partial create-sprog gl) shader-sources)]
        (swap! autosprog-cache-atom assoc autosprog-key autosprog)
        autosprog))))

(defn run-shaders! [gl 
                    [frag-source vert-source]
                    size
                    uniform-map
                    attribute-map
                    start
                    length
                      & [{:keys [transform-feedback] :as options}]]
  (run-sprog! gl
              (get-autosprog gl 
                             [frag-source
                              (cond-> vert-source
                                transform-feedback 
                                (assoc :transform-feedback-outputs
                                       (transform-feedback-outputs
                                        transform-feedback)))])
              size
              uniform-map
              attribute-map
              start
              length
              options))

(defonce purefrag-autosprog-cache-atom (atom {}))

(defn get-purefrag-autosprog [gl shader-source]
  (let [autosprog-key [gl shader-source]]
    (if-let [autosprog (@purefrag-autosprog-cache-atom autosprog-key)]
      autosprog
      (let [autosprog (create-purefrag-sprog gl shader-source)]
        (swap! purefrag-autosprog-cache-atom assoc autosprog-key autosprog)
        autosprog))))

(defn run-purefrag-shader! [gl source size uniform-map & [options]]
  (run-purefrag-sprog! gl
                       (get-purefrag-autosprog gl source)
                       size
                       uniform-map
                       options))
