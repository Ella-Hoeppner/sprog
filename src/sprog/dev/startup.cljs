(ns sprog.dev.startup
  (:require sprog.dev.basic-demo
            sprog.dev.multi-texture-output-demo
            sprog.dev.fn-sort-demo
            sprog.dev.pixel-sort-demo
            sprog.dev.raymarch-demo
            sprog.dev.physarum-demo
            sprog.dev.texture-channel-demo
            sprog.dev.struct-demo
            sprog.dev.simplex-demo
            sprog.dev.tilable-simplex-demo
            sprog.dev.macro-demo
            sprog.dev.bilinear-demo
            sprog.dev.vertex-demo
            sprog.dev.voronoise-demo
            sprog.dev.video-demo
            sprog.dev.webcam-demo
            sprog.dev.bloom-demo
            sprog.dev.texture-3d-demo
            sprog.dev.gaussian-demo
            sprog.dev.hsv-demo
            sprog.dev.params-demo
            sprog.dev.transform-feedback-demo))

(defn init []
  #_(sprog.dev.basic-demo/init)
  #_(sprog.dev.multi-texture-output-demo/init)
  #_(sprog.dev.fn-sort-demo/init)
  #_(sprog.dev.pixel-sort-demo/init)
  #_(sprog.dev.raymarch-demo/init)
  #_(sprog.dev.physarum-demo/init)
  #_(sprog.dev.texture-channel-demo/init)
  #_(sprog.dev.struct-demo/init)
  #_(sprog.dev.simplex-demo/init)
  #_(sprog.dev.tilable-simplex-demo/init)
  #_(sprog.dev.macro-demo/init)
  #_(sprog.dev.bilinear-demo/init)
  #_(sprog.dev.vertex-demo/init)
  #_(sprog.dev.voronoise-demo/init)
  #_(sprog.dev.video-demo/init)
  #_(sprog.dev.webcam-demo/init)
  #_(sprog.dev.bloom-demo/init)
  #_(sprog.dev.texture-3d-demo/init)
  #_(sprog.dev.gaussian-demo/init)
  #_(sprog.dev.hsv-demo/init)
  #_(sprog.dev.params-demo/init)
  (sprog.dev.transform-feedback-demo/init))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
