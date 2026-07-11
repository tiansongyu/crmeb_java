const assert = require('assert');
const fs = require('fs');
const path = require('path');

const source = fs.readFileSync(
  path.join(__dirname, '..', 'pages', 'users', 'login', 'index.vue'),
  'utf8'
);

assert.match(
  source,
  /class="login-mode-tabs"/,
  'login page should render explicit login mode tabs'
);

assert.match(
  source,
  /class="mode-tab account-tab"[\s\S]*账号登录/,
  'account/password mode should be visible as a first-class tab'
);

assert.match(
  source,
  /class="mode-tab sms-tab"[\s\S]*验证码登录/,
  'sms login mode should remain available as a tab'
);

assert.match(
  source,
  /current:\s*0,/,
  'account/password login should be the default test mode'
);

assert.match(
  source,
  /v-if="current === 0"/,
  'account/password form should be explicitly tied to account mode'
);

assert.match(
  source,
  /v-if="current === 1 \|\| appLoginStatus \|\| appleLoginStatus"/,
  'sms form should be explicitly tied to sms mode and app binding states'
);

console.log('login page account/password mode checks passed');
