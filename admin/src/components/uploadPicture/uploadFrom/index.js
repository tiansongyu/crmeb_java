import uploadFromComponent from './index.vue';
const uploadFrom = {};
uploadFrom.install = function (Vue, options) {
  const ToastConstructor = Vue.extend(uploadFromComponent);
  // 生成一个该子类的实例
  const instance = new ToastConstructor();
  instance.$mount(document.createElement('div'));
  document.body.appendChild(instance.$el);
  Vue.prototype.$modalUpload = function (callback, isMore, modelName, boolean, defaultName) {
    instance.visible = true;
    instance.callback = callback;
    instance.isMore = isMore;
    instance.modelName = modelName;
    instance.defaultName = defaultName || '';
    instance.booleanVal = boolean;
  };
};
export default uploadFrom;
