(ns sprog.iglu.chunks
  (:require [clojure.walk :refer [postwalk 
                                  postwalk-replace]]))

(def trivial-vert-source
  '{:version "300 es"
    :precision {float lowp}
    :inputs {vertPos vec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (= gl_Position vertPos))}})

(defn merge-chunks [& chunks]
  (assoc (reduce (partial merge-with merge)
                 (map #(dissoc % :version) chunks))
         :version "300 es"))

(def rand-chunk
  '{:signatures {rand ([vec2] float)}
    :functions {rand
                ([p]
                 (=vec3 p3 (fract (* (vec3 p.xyx) "0.1031")))
                 (+= p3 (dot p3 (+ p3.yzx "33.33")))
                 (fract (* (+ p3.x p3.y) p3.z)))}})

; based on https://thebookofshaders.com/edit.php#11/2d-snoise-clear.frag
(def simplex-2d-chunk
  {:signatures '{mod289_3 ([vec3] vec3)
                 mod289 ([vec2] vec2)
                 permute ([vec3] vec3)
                 snoise ([vec2] float)}
   :functions
   {'mod289_3 '([x] (- x (* (floor (/ x "289.0")) "289.0")))
    'mod289 '([x] (- x (* (floor (/ x "289.0")) "289.0")))
    'permute '([x] (mod289_3 (* x (+ "1.0" (* x "34.0")))))
    'snoise
    (postwalk-replace
     {:c (conj (list (/ (- 3 (Math/sqrt 3)) 6)
                     (/ (- (Math/sqrt 3) 1) 2)
                     (- (/ (- 3 (Math/sqrt 3)) 3) 1)
                     (/ 1 41))
               'vec4)}
     '([v]
       (+= v (vec2 "12.5" "-3.6"))
       (=vec4 C :c)
       (=vec2 i (floor (+ v (dot v C.yy))))
       (=vec2 x0 (- (+ v (dot i C.xx))
                    i))

       (=vec2 i1 (if (> x0.x x0.y) (vec2 1 0) (vec2 0 1)))
       (=vec2 x1 (- (+ x0.xy C.xx) i1))
       (=vec2 x2 (+ x0.xy C.zz))

       (= i (mod289 i))

       (=vec3 p (permute
                 (+ (permute (+ i.y (vec3 0 i1.y 1)))
                    i.x
                    (vec3 0 i1.x 1))))
       (=vec3 m (max (vec3 "0.0")
                     (- "0.5"
                        (vec3 (dot x0 x0)
                              (dot x1 x1)
                              (dot x2 x2)))))

       (= m (* m m))
       (= m (* m m))

       (=vec3 x (- (* "2.0" (fract (* p C.www))) "1.0"))
       (=vec3 h (- (abs x) "0.5"))
       (=vec3 ox (floor (+ x "0.5")))
       (=vec3 a0 (- x ox))

       (*= m (- "1.79284291400159"
                (* "0.85373472095314"
                   (+ (* a0 a0)
                      (* h h)))))

       (=vec3 g (vec3 (+ (* a0.x x0.x) (* h.x x0.y))
                      (+ (* a0.yz (vec2 x1.x x2.x))
                         (* h.yz (vec2 x1.y x2.y)))))
       (+ "0.5"
          (* "65.0" (dot m g)))))}})

; based on https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
(def simplex-3d-chunk
  {:signatures '{permute ([vec4] vec4)
                 taylorInvSqrt ([vec4] vec4)
                 snoise ([vec3] float)}
   :functions
   '{permute ([x] (mod (* x (+ "1.0" (* "34.0" x))) "289.0"))
     taylorInvSqrt ([r] (- "1.79284291400159"
                           (* r "0.85373472095314")))
     snoise
     ([v]
      (=vec2 C (vec2 (/ "1.0" "6.0") (/ "1.0" "3.0")))
      (=vec4 D (vec4 "0.0" "0.5" "1.0" "2.0"))
      
      ; first corner
      (=vec3 i (floor (+ v (dot v C.yyy))))
      (=vec3 x0 (+ (- v i)
                   (dot i C.xxx)))
      
      ; other corners
      (=vec3 g (step x0.yzx x0.xyz))
      (=vec3 l (- "1.0" g))
      (=vec3 i1 (min g.xyz l.zxy))
      (=vec3 i2 (max g.xyz l.zxy))

      (=vec3 x1 (+ (- x0 i1) C.xxx))
      (=vec3 x2 (+ (- x0 i2) (* "2.0" C.xxx)))
      (=vec3 x3 (+ (- x0 "1.0") (* "3.0" C.xxx)))

      ; permutations
      (= i (mod i "289.0"))
      (=vec4 p (permute (+ (permute (+ (permute (+ i.z (vec4 0 i1.z i2.z 1)))
                                       i.y
                                       (vec4 0 i1.y i2.y 1)))
                           i.x
                           (vec4 0 i1.x i2.x 1))))
      
      ; gradients
      (=vec3 ns (- (* D.wyz (/ "1.0" "7.0")) D.xzx))

      (=vec4 j (- p (* "49.0" (floor (* p ns.z ns.z)))))

      (=vec4 x_ (floor (* j ns.z)))
      (=vec4 y_ (floor (- j (* "7.0" x_))))
      
      (=vec4 x (+ ns.yyyy (* ns.x x_)))
      (=vec4 y (+ ns.yyyy (* ns.x y_)))
      (=vec4 h (- "1.0" (+ (abs x) (abs y))))

      (=vec4 b0 (vec4 x.xy y.xy))
      (=vec4 b1 (vec4 x.zw y.zw))

      (=vec4 s0 (+ "1.0" (* "2.0" (floor b0))))
      (=vec4 s1 (+ "1.0" (* "2.0" (floor b1))))
      (=vec4 sh (- "0.0" (step h (vec4 0))))

      (=vec4 a0 (+ b0.xzyw (* s0.xzyw sh.xxyy)))
      (=vec4 a1 (+ b1.xzyw (* s1.xzyw sh.zzww)))

      (=vec3 p0 (vec3 a0.xy h.x))
      (=vec3 p1 (vec3 a0.zw h.y))
      (=vec3 p2 (vec3 a1.xy h.z))
      (=vec3 p3 (vec3 a1.zw h.w))

      ; normalize gradients
      (=vec4 norm (taylorInvSqrt (vec4 (dot p0 p0)
                                       (dot p1 p1)
                                       (dot p2 p2)
                                       (dot p3 p3))))
      
      (*= p0 norm.x)
      (*= p1 norm.y)
      (*= p2 norm.z)
      (*= p3 norm.w)

      ; mix final noise value
      (=vec4 m (max (- "0.6"
                       (vec4 (dot x0 x0)
                             (dot x1 x1)
                             (dot x2 x2)
                             (dot x3 x3)))
                    "0.0"))
      (*= m m)
      (+ "0.5"
         (* "21.0" (dot (* m m)
                        (vec4 (dot p0 x0)
                              (dot p1 x1)
                              (dot p2 x2)
                              (dot p3 x3)))))
      )}})

; fractional brownian motion
(defn get-fbm-chunk [noise-fn & [noise-dimensions]]
  (postwalk-replace
   {:noise-fn noise-fn
    :vec ({1 'float
           2 'vec2
           3 'vec3}
          (or noise-dimensions 2))}
   '{:signatures {fbm ([:vec int float] float)}
     :functions
     {fbm
      ([x octaves hurstExponent]
       (=float g (exp2 (- "0.0" hurstExponent)))
       (=float f "1.0")
       (=float a "1.0")
       (=float t "0.0")
       ("for(int i=0;i<octaves;i++)"
        (+= t (* a (:noise-fn (* f x))))
        (*= f "2.0")
        (*= a g))
       t)}}))

(defn random-shortcut [expression & [rand-fn]]
  (let [rand-fn (or rand-fn rand)]
    (postwalk
     (fn [subexp]
       (if (and (vector? subexp)
                (= (first subexp) :rand))
         (postwalk-replace
          {:scale (* (if (> (rand-fn) 0.5) 1 -1)
                     (+ 200 (* (rand-fn) 300)))
           :x (- (* (rand-fn) 100) 50)
           :y (- (* (rand-fn) 100) 50)
           :seed-exp (second subexp)}
          '(rand (+ (* :seed-exp :scale)
                    (vec2 :x :y))))
         subexp))
     expression)))

(def hsl-to-rgb-chunk
  '{:signatures {hsl2rgb ([vec3] vec3)}
    :functions {hsl2rgb
                ([color]
                 (=vec3 chroma
                        (clamp (- "2.0"
                                  (abs
                                   (- (mod (+ (* color.x "6.0")
                                              (vec3 "3.0"
                                                    "1.0"
                                                    "5.0"))
                                           "6.0")
                                      "3.0")))
                               "0.0"
                               "1.0"))
                 (mix (mix (vec3 0)
                           (mix (vec3 "0.5") chroma color.y)
                           (clamp (* color.z "2.0") "0.0" "1.0"))
                      (vec3 1)
                      (clamp (- (* color.z "2.0") "1.0")
                             "0.0"
                             "1.0")))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cubic-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (= rgb (* rgb rgb (- "3.0" (* "2.0" rgb))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from @Frizzil's comment on https://www.shadertoy.com/view/MsS3Wc
(def quintic-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (= rgb (* rgb
                           rgb
                           rgb
                           (+ "10.0" (* rgb (- (* rgb "6.0") "15.0")))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cosine-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (= rgb (+ "0.5" (* "-0.5" (cos (* rgb "3.14159265359")))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

(def particle-vert-source-u16
  '{:version "300 es"
    :precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=int agentIndex (/ gl_VertexID 6))
      (=int corner "gl_VertexID % 6")

      (=ivec2 texSize (textureSize particleTex 0))

      (=vec2 texPos
             (/ (+ "0.5" (vec2 (% agentIndex texSize.x)
                               (/ agentIndex texSize.x)))
                (vec2 texSize)))

      (=uvec4 particleColor (texture particleTex texPos))
      (= particlePos (/ (vec2 particleColor.xy) "65535.0"))

      (= gl_Position
         (vec4 (- (* (+ particlePos
                        (* radius
                           (- (* "2.0"
                                 (if (|| (== corner 0)
                                         (== corner 3))
                                   (vec2 0 1)
                                   (if (|| (== corner 1)
                                           (== corner 4))
                                     (vec2 1 0)
                                     (if (== corner 2)
                                       (vec2 0 0)
                                       (vec2 1 1)))))
                              "1.0")))
                     "2.0")
                  "1.0")
               0
               1)))}})

(def particle-vert-source-u32
  '{:version "300 es"
    :precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=int agentIndex (/ gl_VertexID 6))
      (=int corner "gl_VertexID % 6")

      (=ivec2 texSize (textureSize particleTex 0))

      (=vec2 texPos
             (/ (+ "0.5" (vec2 (% agentIndex texSize.x)
                               (/ agentIndex texSize.x)))
                (vec2 texSize)))

      (=uvec4 particleColor (texture particleTex texPos))
      (= particlePos (/ (vec2 particleColor.xy) "4294967295.0"))

      (= gl_Position
         (vec4 (- (* (+ particlePos
                        (* radius
                           (- (* "2.0"
                                 (if (|| (== corner 0)
                                         (== corner 3))
                                   (vec2 0 1)
                                   (if (|| (== corner 1)
                                           (== corner 4))
                                     (vec2 1 0)
                                     (if (== corner 2)
                                       (vec2 0 0)
                                       (vec2 1 1)))))
                              "1.0")))
                     "2.0")
                  "1.0")
               0
               1)))}})

(def particle-frag-source-u16
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor uvec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (uvec4 65535 0 0 0)))}})

(def particle-frag-source-u32
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor uvec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (uvec4 65535 0 0 0)))}})

(def particle-frag-source-f8
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (vec4 1 0 0 1)))}})

(defn sparse-gaussian-expression [value-fn radius sigma & [skip-factor]]
  (let [coords (conj (mapcat (fn [r]
                               (list [0 r]
                                     [r r]
                                     [r 0]
                                     [r (- r)]
                                     [0 (- r)]
                                     [(- r) (- r)]
                                     [(- r) 0]
                                     [(- r) r]))
                             (range 1 (inc radius) (or skip-factor 1)))
                     [0 0])
        factors (map (fn [[x y]]
                       (Math/exp
                        (/ (- (+ (* x x) (* y y)))
                           (* 2 sigma sigma))))
                     coords)
        factor-sum (apply + factors)]
    (conj (map (fn [[x y] factor]
                 (postwalk-replace
                  {:x (.toFixed x 1)
                   :y (.toFixed y 1)
                   :factor (.toFixed (/ factor factor-sum) 8)
                   :value-fn value-fn}
                  '(* (:value-fn :x :y) :factor)))
               coords
               factors)
          '+)))

; from https://stackoverflow.com/a/42179924
(def bicubic-sample-chunk
  '{:signatures {cubic ([float] vec4)
                 textureBicubic ([usampler2D vec2] vec4)}
    :functions
    {cubic
     ([v]
      (=vec4 n (- (vec4 1 2 3 4) v))
      (=vec4 s (* n n n))
      (=float x s.x)
      (=float y (- s.y (* "4.0" s.x)))
      (=float z (+ s.z
                   (* "-4.0" s.y)
                   (* "6.0" s.x)))
      (=float w (- "6.0"
                   (+ x y z)))
      (/ (vec4 x y z w)
         "6.0"))
     textureBicubic
     ([tex pos]
      (=vec2 texSize (vec2 (textureSize tex 0)))

      (=vec2 texCoords (- (* pos texSize) "0.5"))

      (=vec2 fxy (fract texCoords))
      (-= texCoords fxy)

      (=vec4 xcubic (cubic fxy.x))
      (=vec4 ycubic (cubic fxy.y))

      (=vec4 c (+ texCoords.xxyy
                  (vec4 "-0.5"
                        "1.5"
                        "-0.5"
                        "1.5")))

      (=vec4 s (vec4 (+ xcubic.xz
                        xcubic.yw)
                     (+ ycubic.xz
                        ycubic.yw)))

      (=vec4 offset (/ (+ c (/ (vec4 xcubic.yw ycubic.yw) s))
                       texSize.xxyy))

      (=vec4 sample0 (vec4 (texture tex offset.xz)))
      (=vec4 sample1 (vec4 (texture tex offset.yz)))
      (=vec4 sample2 (vec4 (texture tex offset.xw)))
      (=vec4 sample3 (vec4 (texture tex offset.yw)))

      (=float sx (/ s.x (+ s.x s.y)))
      (=float sy (/ s.z (+ s.z s.w)))

      (mix (mix sample3 sample2 sx)
           (mix sample1 sample0 sx)
           sy))}})
