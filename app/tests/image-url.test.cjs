const assert = require('assert');

const {
  getImageHost,
  normalizeImageTree,
  normalizeImageUrl,
} = require('../utils/imageUrl.js');

const imageHost = 'https://cdn.example.com';

assert.strictEqual(
  getImageHost('crmebimage/public/logo.png', { imageHost }),
  'https://cdn.example.com/'
);

assert.strictEqual(
  normalizeImageUrl('crmebimage/public/logo.png', { imageHost }),
  'https://cdn.example.com/crmebimage/public/logo.png'
);

assert.strictEqual(
  normalizeImageUrl('/crmebimage/public/logo.png', { imageHost }),
  'https://cdn.example.com/crmebimage/public/logo.png'
);

assert.strictEqual(
  normalizeImageUrl('crmebimage/public/logo.png', { imageHost: '' }),
  '/crmebimage/public/logo.png'
);

assert.strictEqual(
  normalizeImageUrl('undefinedcrmebimage/presets/seckill_bg_pic.png', { imageHost }),
  'https://cdn.example.com/crmebimage/presets/seckill_bg_pic.png'
);

assert.strictEqual(
  normalizeImageUrl('http://192.168.31.35:2500/crmebimage/public/banner.png', { imageHost }),
  'https://cdn.example.com/crmebimage/public/banner.png'
);

assert.strictEqual(
  normalizeImageUrl('http://img.alicdn.com/imgextra/i2/product.jpg', { imageHost }),
  'http://img.alicdn.com/imgextra/i2/product.jpg'
);

assert.strictEqual(
  normalizeImageUrl('https://apia.beta.crmeb.xbdzz.cn/crmebimage/public/product/missing.png', { imageHost }),
  '/__image/apia/crmebimage/public/product/missing.png'
);

assert.strictEqual(
  normalizeImageUrl('https://crmebjavasingle.oss-cn-beijing.aliyuncs.com/crmebimage/public/content/icon.png', { imageHost }),
  '/__image/oss/crmebimage/public/content/icon.png'
);

assert.strictEqual(
  normalizeImageUrl('/__image/oss/crmebimage/public/content/icon.png', { imageHost }),
  '/__image/oss/crmebimage/public/content/icon.png'
);

const data = {
  banner: [{ img: 'crmebimage/public/banner.png' }],
  product: { image: 'http://192.168.31.35:2500/crmebimage/public/product.png' },
  nested: [{ css: 'url(undefinedcrmebimage/presets/bg.png)' }],
};

normalizeImageTree(data, { imageHost });

assert.strictEqual(data.banner[0].img, 'https://cdn.example.com/crmebimage/public/banner.png');
assert.strictEqual(data.product.image, 'https://cdn.example.com/crmebimage/public/product.png');
assert.strictEqual(data.nested[0].css, 'url(https://cdn.example.com/crmebimage/presets/bg.png)');

console.log('image-url normalization checks passed');
