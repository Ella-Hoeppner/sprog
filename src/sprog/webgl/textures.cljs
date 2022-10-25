(ns sprog.webgl.textures)


(defn set-texture-parameters [gl texture filter-mode wrap-mode & [three-d?]]
  (let [texture-target (if three-d? gl.TEXTURE_3D gl.TEXTURE_2D)]
    (.bindTexture gl texture-target texture)
    (let [gl-filter-mode ({:linear gl.LINEAR
                           :nearest gl.NEAREST}
                          filter-mode)]
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_MIN_FILTER
                      gl-filter-mode)
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_MAG_FILTER
                      gl-filter-mode))
    (let [wrap-mode->gl-enum (fn [mode]
                               (case mode
                                 :clamp gl.CLAMP_TO_EDGE
                                 :repeat gl.REPEAT
                                 :mirror gl.MIRRORED_REPEAT
                                 mode))
          [gl-wrap-s gl-wrap-t gl-wrap-r]
          (if (coll? wrap-mode)
            (map wrap-mode->gl-enum wrap-mode)
            (repeat (wrap-mode->gl-enum wrap-mode)))]
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_WRAP_S
                      gl-wrap-s)
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_WRAP_T
                      gl-wrap-t)
      (when three-d?
        (.texParameteri gl
                        texture-target
                        gl.TEXTURE_WRAP_R
                        gl-wrap-r)))))

(defn create-texture [gl
                      resolution
                      texture-type
                      & [{:keys [wrap-mode
                                 filter-mode
                                 channels
                                 data]
                          :or {wrap-mode :repeat
                               filter-mode :linear
                               channels 4}
                          three-d? :3d}]]
  (let [texture-target (if three-d? gl.TEXTURE_3D gl.TEXTURE_2D)
        tex (.createTexture gl texture-target)]
    (.bindTexture gl texture-target tex)
    (let [internal-format (({:f8 [gl.R8 gl.RG8 gl.RGB8 gl.RGBA]
                             :u16 [gl.R16UI gl.RG16UI gl.RGB16UI gl.RGBA16UI]
                             :u32 [gl.R32UI gl.RG32UI gl.RGB32UI gl.RGBA32UI]}
                            texture-type)
                           (dec channels))
          format (({:f8 [gl.RED gl.RG gl.RGB gl.RGBA]
                    :u16 [gl.RED_INTEGER
                          gl.RG_INTEGER
                          gl.RGB_INTEGER
                          gl.RGBA_INTEGER]
                    :u32 [gl.RED_INTEGER
                          gl.RG_INTEGER
                          gl.RGB_INTEGER
                          gl.RGBA_INTEGER]}
                   texture-type)
                  (dec channels))
          webgl-type ({:f8 gl.UNSIGNED_BYTE
                       :u16 gl.UNSIGNED_SHORT
                       :u32 gl.UNSIGNED_INT}
                      texture-type)]
      (if three-d?
        (let [[width height depth] (if (number? resolution)
                                     [resolution resolution resolution]
                                     resolution)]
          (.texImage3D gl
                       gl.TEXTURE_3D
                       0
                       internal-format
                       width
                       height
                       depth
                       0
                       format
                       webgl-type
                       data))
        (let [[width height] (if (number? resolution)
                               [resolution resolution]
                               resolution)]
          (.texImage2D gl
                       gl.TEXTURE_2D
                       0
                       internal-format
                       width
                       height
                       0
                       format
                       webgl-type
                       data))))
    (set-texture-parameters gl tex filter-mode wrap-mode three-d?)
    tex))

(defn create-f8-tex [gl resolution & [options]]
  (create-texture gl resolution :f8 options))

(defn create-u16-tex [gl resolution & [options]]
  (create-texture gl resolution :u16 (merge options {:filter-mode :nearest})))

(defn create-u32-tex [gl resolution & [options]]
  (create-texture gl resolution :u32 (merge options {:filter-mode :nearest})))

(defn copy-html-image-data! [gl tex element-or-id]
  (let [element (if (string? element-or-id)
                  (.getElementById js/document element-or-id)
                  element-or-id)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (.texImage2D gl
                 gl.TEXTURE_2D
                 0
                 gl.RGBA
                 gl.RGBA
                 gl.UNSIGNED_BYTE
                 element)))

(defn html-image-texture [gl element-or-id & [{:keys [wrap-mode
                                                      filter-mode]
                                               :or {wrap-mode :repeat
                                                    filter-mode :linear}}]]
  (let [texture (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D texture)
    (set-texture-parameters gl texture filter-mode wrap-mode)
    (copy-html-image-data! gl texture element-or-id)
    texture))

(defn create-webcam-video-element [callback & [{:keys [width 
                                                       height 
                                                       brightness]
                                                :or {width 1024
                                                     height 1024
                                                     brightness 2}}]]
  (let [media-constraints (clj->js {:audio false
                                    :video {:width width
                                            :height height
                                            :brightness {:ideal brightness}}})
        video (js/document.createElement "video")]
    (.then (js/navigator.mediaDevices.getUserMedia media-constraints)
           (fn [media-stream]
             (set! video.srcObject media-stream)
             (.setAttribute video "playsinline" true)
             (set! video.onloadedmetadata
                   (fn [e]
                     (.play video)
                     (callback video)))))))
