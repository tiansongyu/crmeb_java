// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

export default {
  data() {
    return {
      disabled: false,
      text: "获取验证码",
      verifyCodeTimer: null
    };
  },
  beforeDestroy() {
    this.clearVerifyCodeTimer();
  },
  onUnload() {
    this.clearVerifyCodeTimer();
  },
  methods: {
    sendCode() {
      if (this.disabled) return;
      this.disabled = true;
      this.clearVerifyCodeTimer();
      const expiresAt = Date.now() + 60 * 1000;
      const update = () => {
        const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
        if (remaining === 0) {
          this.clearVerifyCodeTimer();
          this.disabled = false;
          this.text = "重新获取";
          return;
        }
        this.text = "剩余 " + remaining + "s";
      };
      update();
      this.verifyCodeTimer = setInterval(update, 1000);
    },
    clearVerifyCodeTimer() {
      if (this.verifyCodeTimer) {
        clearInterval(this.verifyCodeTimer);
        this.verifyCodeTimer = null;
      }
    }
  }
};
