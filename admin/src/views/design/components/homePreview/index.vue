<template>
  <!--
    DIY 首页只读预览
    复用 design/components/mobilePage 中的后台布局组件渲染，
    与小程序 / H5 端使用同一份 DIY 数据，避免依赖 iframe 嵌入 H5 时出现的画面错乱。
  -->
  <div class="home-preview">
    <div class="phone-frame">
      <div class="status-bar">
        <span class="time">9:41</span>
        <span class="battery">100%</span>
      </div>
      <div class="page-title" :style="pageTitleStyle">{{ titleTxt }}</div>
      <div class="scroll-box-wrap">
        <div
          class="scroll-box"
          :class="[
            picTxt && tabValTxt == 2 ? 'fullsize noRepeat' : picTxt && tabValTxt == 1 ? 'repeat ysize' : 'noRepeat ysize',
          ]"
          :style="bgStyle"
        >
          <div v-if="loading" class="preview-tip">加载中…</div>
          <div v-else-if="!mConfig.length" class="preview-tip">暂无页面内容</div>
          <div v-for="(item, key) in mConfig" :key="item.id || key" class="preview-item">
            <component :is="item.name" :configData="propsObj" :index="key" :num="item.num"></component>
          </div>
        </div>
        <!-- 透明遮罩，预览只读不可交互 -->
        <div class="preview-mask"></div>
      </div>
    </div>
  </div>
</template>

<script>
// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------
import { mapState, mapGetters } from 'vuex';
import { pagediyInfoApi, pagediyGetSetHome } from '@/api/pagediy';
import { mediaDomainApi, changeColorApi } from '@/api/systemConfig';
import mPage from '../mobilePage/index.js';

export default {
  name: 'HomePreview',
  components: {
    ...mPage,
  },
  props: {
    // 指定预览的 DIY 模板 id；不传则预览当前设为首页的模板
    pageId: {
      type: [Number, String],
      default: 0,
    },
  },
  data() {
    return {
      themeColor: ['#e93323', '#FE5C2D', '#42CA4D', '#1db0fc', '#ff448f'],
      lConfig: [], // mobilePage 组件定义数组
      mConfig: [], // 当前页面需渲染的组件列表
      propsObj: {},
      loading: true,
    };
  },
  computed: {
    ...mapState('mobildConfig', {
      titleTxt: (state) => state.pageTitle || '首页',
      colorTxt: (state) => state.pageColor,
      picTxt: (state) => state.pagePic,
      colorPickerTxt: (state) => state.pageColorPicker,
      tabValTxt: (state) => state.pageTabVal,
      picUrlTxt: (state) => state.pagePicUrl,
      titleColor: (state) => state.titleColor,
      titleBgColor: (state) => state.titleBgColor,
    }),
    ...mapGetters(['mediaDomain']),
    pageTitleStyle() {
      return { backgroundColor: this.titleBgColor, color: this.titleColor };
    },
    bgStyle() {
      return (
        'background-color:' +
        (this.colorTxt ? this.colorPickerTxt : '') +
        ';background-image: url(' +
        (this.picTxt ? this.picUrlTxt : '') +
        ');'
      );
    },
  },
  watch: {
    pageId() {
      this.loadPreview();
    },
  },
  created() {
    this.lConfig = this.objToArr(mPage);
  },
  mounted() {
    this.getMobileTheme();
    if (!localStorage.getItem('mediaDomain')) this.getMediadomain();
    this.loadPreview();
  },
  beforeDestroy() {
    this.$store.commit('mobildConfig/SETEMPTY');
  },
  methods: {
    objToArr(data) {
      return Object.keys(data).map((key) => data[key]);
    },
    getMobileTheme() {
      changeColorApi()
        .then((res) => {
          this.$store.commit('settings/SET_mobileThemeColor', this.themeColor[res.value - 1]);
        })
        .catch(() => {});
    },
    getMediadomain() {
      mediaDomainApi()
        .then((res) => {
          localStorage.setItem('mediaDomain', res);
          this.$store.commit('settings/SET_mediaDomain', res);
        })
        .catch(() => {});
    },
    loadPreview() {
      this.loading = true;
      this.$store.commit('mobildConfig/SETEMPTY');
      this.mConfig = [];
      const fetchId = this.pageId
        ? Promise.resolve(Number(this.pageId))
        : pagediyGetSetHome();
      fetchId
        .then((id) => {
          if (!id) {
            this.loading = false;
            return;
          }
          return pagediyInfoApi(id).then((data) => this.applyData(data));
        })
        .catch(() => {
          this.loading = false;
        });
    },
    // 写入页面设置 + 构建组件渲染列表（与 creatDevise.defaultData 保持一致）
    applyData(data) {
      try {
      this.$store.commit('mobildConfig/titleUpdata', data.title);
      this.$store.commit('mobildConfig/colorUpdata', data.isBgColor || 0);
      this.$store.commit('mobildConfig/picUpdata', data.isBgPic || 0);
      this.$store.commit('mobildConfig/pickerUpdata', data.colorPicker || '#f5f5f5');
      this.$store.commit('mobildConfig/radioUpdata', data.bgTabVal || 0);
      this.$store.commit('mobildConfig/picurlUpdata', data.bgPic || '');
      this.$store.commit('mobildConfig/titleBgColorUpdata', data.titleBgColor);
      this.$store.commit('mobildConfig/titleColorUpdata', data.titleColor);

      const newArr = this.objToArr(data.value || {});
      newArr.sort((a, b) => a.timestamp - b.timestamp);
      const mConfig = [];
      newArr.forEach((el) => {
        if (el.name === 'goodList' && el.selectConfig) {
          localStorage.setItem(el.timestamp, el.selectConfig.activeValue);
        }
        el.id = 'id' + el.timestamp;
        this.lConfig.forEach((item) => {
          if (el.name === item.defaultName) {
            item.num = el.timestamp;
            item.id = 'id' + el.timestamp;
            const tempItem = JSON.parse(JSON.stringify(item));
            mConfig.push(tempItem);
            this.$store.commit('mobildConfig/ADDARRAY', { num: el.timestamp, val: el });
          }
        });
      });
      this.mConfig = mConfig;
      this.loading = false;
      } catch (err) {
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped lang="scss">
.home-preview {
  display: flex;
  justify-content: center;
}
.phone-frame {
  width: 377px;
  border-radius: 10px;
  border: 1px solid #eee;
  background: #f5f5f5;
  overflow: hidden;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.04);
}
.status-bar {
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  font-size: 12px;
  color: #333;
  background: #fff;
}
.page-title {
  height: 35px;
  line-height: 35px;
  text-align: center;
  font-size: 15px;
  color: #333;
  background: #fff;
}
.scroll-box-wrap {
  position: relative;
  height: 600px;
}
.scroll-box {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: #bfc1c4;
    border-radius: 4px;
  }
}
.preview-mask {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: transparent;
}
.preview-tip {
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 13px;
}
.ysize {
  background-size: 100%;
}
.fullsize {
  background-size: 100% 100%;
}
.repeat {
  background-repeat: repeat;
}
.noRepeat {
  background-repeat: no-repeat;
}
</style>
