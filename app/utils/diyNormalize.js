// +----------------------------------------------------------------------
// | DIY 数据规范化层
// +----------------------------------------------------------------------
// 解决：旧版本 admin 保存的 DIY 模板缺少新版组件引入的配置字段
// （如 themeStyleConfig / priceThemeStyleConfig / checkThemeStyleConfig），
// 导致 app 端组件 computed 访问 .tabVal 时抛 Cannot read properties of undefined。
//
// 策略：在拿到后端 DIY 数据后、传入组件前，按区块 name 做 deep merge 兜底。
// 仅补齐"新版新增、且组件代码会读"的字段，不动存量字段。

// 通用主题色调配置（tabVal=0 跟随主题，tabVal=1 自定义）
const themeStyleConfigDefault = {
  title: '色调',
  tabVal: 0,
  isShow: 1,
  list: [{ val: '跟随主题风格' }, { val: '自定义' }],
};

// 单色块默认值生成器
const colorDefault = (item, title = '颜色', name = '') => ({
  isShow: 1,
  title,
  name,
  default: [{ item }],
  color: [{ item }],
});

// 按组件 name（与 admin defaultName / app 渲染分支一致）索引的兜底补丁
const patches = {
  swiperBg: {
    themeStyleConfig: themeStyleConfigDefault,
    docColor: colorDefault('#E93323', '指示器颜色', 'docColor'),
  },
  homeComb: {
    themeStyleConfig: themeStyleConfigDefault,
    docColor: colorDefault('#E93323', '指示器颜色', 'docColor'),
  },
  bargain: {
    themeStyleConfig: themeStyleConfigDefault,
    priceColor: colorDefault('#E93323', '砍价价格颜色', 'priceColor'),
    groupTitleColor: colorDefault('#E93323', '标签颜色', 'groupTitleColor'),
    btnColor: {
      isShow: 1,
      title: '按钮颜色',
      name: 'btnColor',
      default: [{ item: '#FF7931' }, { item: '#E93323' }],
      color: [{ item: '#FF7931' }, { item: '#E93323' }],
    },
  },
  group: {
    themeStyleConfig: themeStyleConfigDefault,
    priceColor: colorDefault('#E93323', '拼团价格颜色', 'priceColor'),
    groupTitleColor: colorDefault('#E93323', '标签颜色', 'groupTitleColor'),
    btnColor: {
      isShow: 1,
      title: '按钮颜色',
      name: 'btnColor',
      default: [{ item: '#FF7931' }, { item: '#E93323' }],
      color: [{ item: '#FF7931' }, { item: '#E93323' }],
    },
  },
  homeCoupons: {
    themeStyleConfig: themeStyleConfigDefault,
    priceColor: colorDefault('#E93323', '价格颜色', 'priceColor'),
    itemBgColor: colorDefault('#FFFFFF', '卡片背景色', 'itemBgColor'),
    btnColor: {
      isShow: 1,
      title: '按钮颜色',
      name: 'btnColor',
      default: [{ item: '#FF7931' }, { item: '#E93323' }],
      color: [{ item: '#FF7931' }, { item: '#E93323' }],
    },
  },
  seckill: {
    themeStyleConfig: themeStyleConfigDefault,
    priceColor: colorDefault('#E93323', '价格颜色', 'priceColor'),
  },
  goodList: {
    themeStyleConfig: themeStyleConfigDefault,
    priceColor: colorDefault('#E93323', '价格颜色', 'priceColor'),
  },
  tabNav: {
    themeStyleConfig: themeStyleConfigDefault,
    checkColor: colorDefault('#E93323', '选中颜色', 'checkColor'),
  },
  pageFooter: {
    themeStyleConfig: themeStyleConfigDefault,
    checkColor: colorDefault('#E93323', '选中颜色', 'checkColor'),
  },
  // admin 端 home_footer 保存到 DIY 数据时 name='footer'，但 app 端 pageFoot.vue 的 name='pageFooter'
  // 两者都注册以兼容历史数据和未来变化
  footer: {
    themeStyleConfig: themeStyleConfigDefault,
    checkColor: colorDefault('#E93323', '选中颜色', 'checkColor'),
  },
  homeTab: {
    priceThemeStyleConfig: { ...themeStyleConfigDefault },
    checkThemeStyleConfig: { ...themeStyleConfigDefault },
    priceColor: colorDefault('#E93323', '价格颜色', 'priceColor'),
    checkColor: colorDefault('#E93323', '选中颜色', 'checkColor'),
  },
};

// 递归 deep merge：仅当 target 对应字段缺失或为 null/undefined 时填入 source 的值
function deepMergeMissing(target, source) {
  if (target === null || target === undefined) return JSON.parse(JSON.stringify(source));
  if (typeof target !== 'object' || Array.isArray(target)) return target;
  if (typeof source !== 'object' || Array.isArray(source)) return target;
  Object.keys(source).forEach((key) => {
    if (target[key] === undefined || target[key] === null) {
      target[key] = JSON.parse(JSON.stringify(source[key]));
    } else if (typeof target[key] === 'object' && !Array.isArray(target[key])) {
      deepMergeMissing(target[key], source[key]);
    }
  });
  return target;
}

/**
 * 兜底单个区块
 * @param {Object} section DIY 区块原始数据
 * @returns {Object} 已兜底的同一对象（in-place + 返回）
 */
export function normalizeDiySection(section) {
  if (!section || typeof section !== 'object') return section;
  const patch = patches[section.name];
  if (patch) deepMergeMissing(section, patch);
  return section;
}

/**
 * 兜底整个 DIY value（key=timestamp 的对象映射）
 * @param {Object} value 后端返回的 res.data.value
 * @returns {Object} 已兜底的同一对象
 */
export function normalizeDiyValue(value) {
  if (!value || typeof value !== 'object') return value;
  Object.keys(value).forEach((k) => normalizeDiySection(value[k]));
  return value;
}

export default {
  normalizeDiyValue,
  normalizeDiySection,
};
