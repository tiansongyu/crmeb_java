/**
 * 全局使用的变量值
 */
import Cache from '@/utils/cache'
import Vue from 'vue'
import { getImageHost } from '@/utils/imageUrl.js'
const global = {
  //图片域名
  urlDomain: getImageHost(Cache.get('imgHost'))
}
Vue.prototype.$GLOBAL = global
