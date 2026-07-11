// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import {
	HTTP_REQUEST_URL,
	HEADER,
	TOKENNAME,
	HEADERPARAMS
} from '@/config/app';
import {
	normalizeImageTree
} from '@/utils/imageUrl.js';
import {
	toLogin,
	checkLogin
} from '../libs/login';
import store from '../store';


/**
 * 发送请求
 */
function baseRequest(url, method, data, {
	noAuth = false,
	noVerify = false
}, params,prefix) {
	const header = Object.assign({}, params !== undefined ? HEADERPARAMS : HEADER);
	if (!noAuth) {
		//登录过期自动登录
		if (!store.state.app.token && !checkLogin()) {
			toLogin();
			return Promise.reject({
				msg: '未登录'
			});
		}
	}
	if (store.state.app.token) header[TOKENNAME] = store.state.app.token;
	return new Promise((resolve, reject) => {
		uni.request({
			url: HTTP_REQUEST_URL + `${prefix ? '/api/public/' : '/api/front/'}` + url,
			method: method || 'GET',
			header: header,
			data: data || {},
			success: (res) => {
				const responseData = res.data || {};
				normalizeImageTree(responseData);
				const code = Number(responseData.code);
				if (noVerify) {
					resolve(responseData);
				} else if (code === 200) {
					resolve(responseData);
				} else if ([410000, 410001, 410002, 401, 402].indexOf(code) !== -1) {
					toLogin();
					reject(responseData);
				} else if (code === 500) {
					reject(responseData.message || '系统异常');
				} else if (code === 400) {
					reject(responseData.message || '参数校验失败');
				} else if (code === 404) {
					reject(responseData.message || '没有找到相关数据');
				} else if (code === 403) {
					reject(responseData.message || '没有相关权限');
				} else {
					reject(responseData.message || '系统错误');
				}
			},
			fail: (msg) => {
				reject(msg && msg.errMsg ? msg.errMsg : '请求失败');
			}
		})
	});
}

const request = {};

['options', 'get', 'post', 'put', 'head', 'delete', 'trace', 'connect'].forEach((method) => {
	request[method] = (api, data, opt, params,prefix) => baseRequest(api, method, data, opt || {}, params,prefix)
});



export default request;
