const { IMAGE_DOMAIN, HTTP_REQUEST_URL } = require('../config/app');

const CRMEB_IMAGE_RE = /(https?:\/\/[^'"(),\s]+\/)?(?:undefined)?\/?crmebimage\/[^'"(),\s]+/g;
const CRMEB_IMAGE_PATH_RE = /crmebimage\/.*/;
const PRIVATE_HOST_RE = /^(localhost|127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)/;
const KNOWN_IMAGE_PROXIES = {
  'apia.beta.crmeb.xbdzz.cn': '/__image/apia/',
  'crmebjavasingle.oss-cn-beijing.aliyuncs.com': '/__image/oss/',
};

function trimTrailingSlash(value) {
  return (value || '').toString().trim().replace(/\/+$/, '');
}

function runtimeOrigin() {
  if (typeof window !== 'undefined' && window.location && window.location.origin) {
    return window.location.origin;
  }
  return '';
}

function isAbsoluteUrl(value) {
  return /^https?:\/\//i.test(value);
}

function isDataUrl(value) {
  return /^data:/i.test(value);
}

function isPrivateUrl(value) {
  try {
    const parsed = new URL(value);
    return PRIVATE_HOST_RE.test(parsed.hostname);
  } catch (e) {
    return false;
  }
}

function proxyKnownImageUrl(value) {
  if (!isAbsoluteUrl(value)) return '';
  try {
    const parsed = new URL(value);
    const prefix = KNOWN_IMAGE_PROXIES[parsed.hostname];
    if (!prefix) return '';
    return prefix + parsed.pathname.replace(/^\/+/, '') + parsed.search;
  } catch (e) {
    return '';
  }
}

function hostWithSlash(value) {
  const host = trimTrailingSlash(value);
  return host ? host + '/' : '';
}

function configuredHost(options) {
  return hostWithSlash(options && options.imageHost ? options.imageHost : IMAGE_DOMAIN);
}

function getImageHost(source, options) {
  const explicitHost = configuredHost(options);
  if (explicitHost) return explicitHost;

  const value = (source || '').toString().trim();
  if (isAbsoluteUrl(value) && value.indexOf('crmebimage/') !== -1 && !isPrivateUrl(value)) {
    return hostWithSlash(value.split(/\/?crmebimage\//)[0]);
  }

  const requestHost = hostWithSlash(HTTP_REQUEST_URL);
  if (requestHost && !isPrivateUrl(requestHost)) return requestHost;

  return hostWithSlash(runtimeOrigin()) || '/';
}

function normalizeCrmebImagePath(value) {
  const match = value.match(CRMEB_IMAGE_PATH_RE);
  return match ? match[0] : value.replace(/^\/+/, '');
}

function normalizeImageUrl(value, options) {
  if (typeof value !== 'string') return value;
  if (!value || isDataUrl(value)) return value;
  if (value.indexOf('/__image/') !== -1) return value;

  const host = getImageHost(value, options);

  return value
    .replace(CRMEB_IMAGE_RE, function(match) {
      const proxied = proxyKnownImageUrl(match);
      if (proxied) return proxied;
      if (isAbsoluteUrl(match) && !isPrivateUrl(match)) return match;
      const path = normalizeCrmebImagePath(match);
      const normalized = host + path;
      return proxyKnownImageUrl(normalized) || normalized;
    });
}

function normalizeImageTree(value, options) {
  if (!value) return value;
  if (typeof value === 'string') return normalizeImageUrl(value, options);
  if (Array.isArray(value)) {
    value.forEach(function(item, index) {
      value[index] = normalizeImageTree(item, options);
    });
    return value;
  }
  if (typeof value === 'object') {
    Object.keys(value).forEach(function(key) {
      value[key] = normalizeImageTree(value[key], options);
    });
  }
  return value;
}

module.exports = {
  getImageHost,
  normalizeImageTree,
  normalizeImageUrl,
  default: {
    getImageHost,
    normalizeImageTree,
    normalizeImageUrl,
  },
};
