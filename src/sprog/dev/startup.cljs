(ns sprog.dev.startup
  (:require sprog.dev.basic-demo
            sprog.dev.multi-texture-output-demo
            sprog.dev.fn-sort-demo
            sprog.dev.pixel-sort-demo
            sprog.dev.raymarch-demo
            sprog.dev.physarum-demo
            sprog.dev.texture-channel-demo
            sprog.dev.struct-demo))

(defn init []
  #_(sprog.dev.basic-demo/init)
  #_(sprog.dev.multi-texture-output-demo/init)
  #_(sprog.dev.fn-sort-demo/init)
  #_(sprog.dev.pixel-sort-demo/init)
  #_(sprog.dev.raymarch-demo/init)
  #_(sprog.dev.physarum-demo/init)
  #_(sprog.dev.texture-channel-demo/init)
  (sprog.dev.struct-demo/init))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
