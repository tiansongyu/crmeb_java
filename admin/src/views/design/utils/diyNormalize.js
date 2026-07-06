// +----------------------------------------------------------------------
// | DIY 数据规范化层（后台装修端）
// +----------------------------------------------------------------------
// 解决：旧版本保存的 DIY 模板缺少新版组件引入的配置字段
// （如 themeStyleConfig / priceThemeStyleConfig / checkThemeStyleConfig / 各类颜色块），
// 导致 mobilePage 预览组件 setConfig 访问 data.xxx.tabVal 时抛
// "Cannot read properties of undefined (reading 'tabVal')"，整块预览渲染失败。
//
// 策略：装修端每个 mobilePage 组件都在 data().defaultConfig 里维护了「当前版本完整默认配置」。
// 加载已存模板时，用对应组件的 defaultConfig 对缺失字段做深合并兜底（仅补缺，不覆盖已有值）。
// 与 app 端 utils/diyNormalize.js 思路一致，但这里以组件自身默认配置为权威来源，
// 天然覆盖全部字段、随组件演进自动生效，无需维护补丁清单。

/**
 * 递归深合并：仅当 target 对应字段缺失（undefined/null）时，用 source 的值填充。
 * 已存在的字段（含数组）保持原样，不做覆盖。
 * @param {Object} target 已存模板的区块数据（原地修改）
 * @param {Object} source 组件默认配置
 * @returns {Object} target
 */
export function deepMergeMissing(target, source) {
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

export function buildDefaultConfigMap(components) {
  const map = {};
  (components || []).forEach((comp) => {
    try {
      if (comp && comp.defaultName && typeof comp.data === 'function') {
        const def = comp.data().defaultConfig;
        if (def && typeof def === 'object') map[comp.defaultName] = def;
      }
    } catch (e) {
      // 单个组件默认配置提取失败不影响整体加载
    }
  });
  return map;
}

export function applyComponentDefaults(section, componentsOrMap) {
  if (!section || typeof section !== 'object') return section;
  const defaultsMap = Array.isArray(componentsOrMap) ? buildDefaultConfigMap(componentsOrMap) : componentsOrMap || {};
  const def = defaultsMap[section.name];
  if (def) deepMergeMissing(section, def);
  return section;
}

export default { deepMergeMissing, buildDefaultConfigMap, applyComponentDefaults };
