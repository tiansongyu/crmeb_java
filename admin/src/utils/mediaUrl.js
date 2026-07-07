const CRMEB_MEDIA_RE = /((?:https?:)?\/\/[^'"(),\s]+\/)?(?:undefined)?\/?crmebimage\/[^'"(),\s]+/g;
const CRMEB_MEDIA_PATH_RE = /crmebimage\/.*/;
const FILE_EXTENSION_RE = /\.([^.\\/]+)$/;
const PRIVATE_HOST_RE = /^(localhost|127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)/;

function isAbsoluteMediaUrl(value) {
  return /^(https?:)?\/\//i.test(value) || /^data:/i.test(value) || /^blob:/i.test(value);
}

function isPrivateCrmebMediaUrl(value) {
  if (!/^(https?:)?\/\//i.test(value) || value.indexOf('crmebimage/') === -1) return false;
  try {
    const parseableUrl = value.indexOf('//') === 0 ? `http:${value}` : value;
    return PRIVATE_HOST_RE.test(new URL(parseableUrl).hostname);
  } catch (e) {
    return false;
  }
}

export function normalizeCrmebMediaUrl(value) {
  if (typeof value !== 'string') return value;
  const trimmed = value.trim();
  if (!trimmed || /^data:/i.test(trimmed) || /^blob:/i.test(trimmed) || trimmed.indexOf('/__image/') === 0) {
    return trimmed;
  }
  if (trimmed.indexOf('crmebimage/') === -1) return trimmed;

  return trimmed.replace(CRMEB_MEDIA_RE, (match) => {
    if (isAbsoluteMediaUrl(match) && !isPrivateCrmebMediaUrl(match)) return match;

    const pathMatch = match.match(CRMEB_MEDIA_PATH_RE);
    if (!pathMatch) return match;
    return `/${pathMatch[0].replace(/^\/+/, '')}`;
  });
}

export function normalizeAttachmentMedia(item) {
  if (!item || typeof item !== 'object') return item;
  return {
    ...item,
    sattDir: normalizeCrmebMediaUrl(item.sattDir),
  };
}

export function normalizeAttachmentMediaList(list) {
  if (!Array.isArray(list)) return [];
  return list.map((item) => normalizeAttachmentMedia(item));
}

export function normalizeCrmebMediaTree(value) {
  if (!value) return value;
  if (typeof value === 'string') return normalizeCrmebMediaUrl(value);
  if (Array.isArray(value)) {
    value.forEach((item, index) => {
      value[index] = normalizeCrmebMediaTree(item);
    });
    return value;
  }
  if (typeof value === 'object') {
    Object.keys(value).forEach((key) => {
      value[key] = normalizeCrmebMediaTree(value[key]);
    });
  }
  return value;
}

export function sanitizeUploadFileName(value) {
  return (value || '')
    .toString()
    .split(/[\\/]/)
    .pop()
    .replace(/[\\/:*?"<>|]+/g, '_')
    .replace(/\s+/g, ' ')
    .trim();
}

function fileExtension(fileName, fallbackExt) {
  const match = sanitizeUploadFileName(fileName).match(FILE_EXTENSION_RE);
  return match ? match[1] : fallbackExt;
}

function fileBaseName(fileName) {
  return sanitizeUploadFileName(fileName).replace(FILE_EXTENSION_RE, '');
}

export function buildUploadFileName(customName, originalName, fallbackExt = 'png') {
  const cleanOriginal = sanitizeUploadFileName(originalName) || `upload.${fallbackExt}`;
  const cleanCustom = sanitizeUploadFileName(customName);
  if (!cleanCustom) return cleanOriginal;

  const ext = fileExtension(cleanCustom, '') || fileExtension(cleanOriginal, fallbackExt);
  const baseName = fileBaseName(cleanCustom) || fileBaseName(cleanOriginal) || 'upload';
  return `${baseName}.${ext}`;
}
