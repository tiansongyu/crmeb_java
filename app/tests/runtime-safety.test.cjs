const assert = require('assert');
const fs = require('fs');
const path = require('path');

function read(relativePath) {
  return fs.readFileSync(path.join(__dirname, '..', relativePath), 'utf8');
}

const requestSource = read('utils/request.js');
assert.match(
  requestSource,
  /Object\.assign\(\{\}, params !== undefined \? HEADERPARAMS : HEADER\)/,
  'each request should clone headers instead of mutating shared configuration'
);

const verifyCodeSource = read('mixins/SendVerifyCode.js');
assert.match(verifyCodeSource, /beforeDestroy\(\)[\s\S]*clearVerifyCodeTimer/);
assert.match(verifyCodeSource, /onUnload\(\)[\s\S]*clearVerifyCodeTimer/);
assert.match(verifyCodeSource, /Math\.ceil\(\(expiresAt - Date\.now\(\)\) \/ 1000\)/);

for (const component of [
  'components/countDown/index.vue',
  'components/homeIndex/countDown.vue',
]) {
  const source = read(component);
  assert.match(source, /beforeDestroy:\s*function\(\)[\s\S]*this\.clearTimer\(\)/);
  assert.match(source, /clearInterval\(this\.timer\)/);
  assert.match(source, /watch:\s*\{[\s\S]*datatime/);
}

console.log('runtime resource cleanup checks passed');
