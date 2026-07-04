// uni-app H5 的 PostCSS 配置。
// 关键：必须挂载 @dcloudio/vue-cli-plugin-uni/packages/postcss —— 它负责把 rpx 转成
// 视口单位、把 image/scroll-view 等标签选择器改写成 uni-* 自定义元素等。
// 此前该文件被置空为 { plugins: {} }，导致构建产物里 rpx 原样残留（浏览器忽略），
// 页面所有 rpx 尺寸失效、区块塌陷。
//
// 注意：不要引入 postcss-import —— 它会去内联并解析 App.vue 里 @import 的字体 css
// （D-DIN-PRO-*.otf）导致构建失败；@import 交给 css-loader 处理即可。
module.exports = {
  plugins: [
    require('@dcloudio/vue-cli-plugin-uni/packages/postcss'),
  ],
};
