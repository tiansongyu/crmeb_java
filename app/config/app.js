// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------
//移动端商城API
let runtimeOrigin = typeof window !== 'undefined' ? window.location.origin : ''
let domain = process.env.VUE_APP_BASE_API || runtimeOrigin || 'http://localhost:8081'
let h5Url = process.env.VUE_APP_H5_URL || runtimeOrigin || 'http://localhost:8082'
let imageDomain = process.env.VUE_APP_IMAGE_DOMAIN || ''

module.exports = {
	// 请求域名 格式： https://您的域名
	// #ifdef MP || APP-PLUS
		// HTTP_REQUEST_URL:'',
		HTTP_REQUEST_URL: domain,
		// H5商城地址
		HTTP_H5_URL: h5Url,
	// #endif
	// #ifdef H5
		HTTP_REQUEST_URL:domain,
	// #endif
	HEADER:{
		'content-type': 'application/json'
	},
	HEADERPARAMS:{
		'content-type': 'application/x-www-form-urlencoded'
	},
	// 图片资源域名。Docker 演示环境可通过 VUE_APP_IMAGE_DOMAIN 指向可访问的图片 CDN。
	IMAGE_DOMAIN: imageDomain,
	// 回话密钥名称 请勿修改此配置
	TOKENNAME: 'Authori-zation',
	// 缓存时间 0 永久
	EXPIRE:0,
	//分页最多显示条数
	LIMIT: 10
};
