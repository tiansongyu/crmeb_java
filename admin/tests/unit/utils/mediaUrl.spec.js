import { buildUploadFileName, normalizeCrmebMediaTree, normalizeCrmebMediaUrl } from '@/utils/mediaUrl';

describe('Utils:mediaUrl', () => {
  it('normalizes local crmeb image paths for browser rendering', () => {
    expect(normalizeCrmebMediaUrl('crmebimage/public/order/proof.png')).toBe('/crmebimage/public/order/proof.png');
    expect(normalizeCrmebMediaUrl('/crmebimage/public/order/proof.png')).toBe('/crmebimage/public/order/proof.png');
    expect(normalizeCrmebMediaUrl('undefinedcrmebimage/public/order/proof.png')).toBe(
      '/crmebimage/public/order/proof.png',
    );
    expect(normalizeCrmebMediaUrl('undefined/crmebimage/public/order/proof.png')).toBe(
      '/crmebimage/public/order/proof.png',
    );
    expect(normalizeCrmebMediaUrl('http://localhost:20400/crmebimage/public/order/proof.png')).toBe(
      '/crmebimage/public/order/proof.png',
    );
    expect(normalizeCrmebMediaUrl('http://127.0.0.1:20400/crmebimage/public/order/proof.png')).toBe(
      '/crmebimage/public/order/proof.png',
    );
  });

  it('keeps already resolvable urls untouched', () => {
    expect(normalizeCrmebMediaUrl('https://cdn.example.com/crmebimage/public/order/proof.png')).toBe(
      'https://cdn.example.com/crmebimage/public/order/proof.png',
    );
    expect(normalizeCrmebMediaUrl('data:image/png;base64,abc')).toBe('data:image/png;base64,abc');
    expect(normalizeCrmebMediaUrl('blob:http://example.com/file')).toBe('blob:http://example.com/file');
    expect(normalizeCrmebMediaUrl('/__image/apia/crmebimage/public/order/proof.png')).toBe(
      '/__image/apia/crmebimage/public/order/proof.png',
    );
  });

  it('normalizes nested API responses without breaking non-image text', () => {
    const data = {
      product: {
        image: 'http://localhost:20400/crmebimage/public/product/main.png',
        sliderImages: ['crmebimage/public/product/slider.png'],
      },
      order: {
        offlinePayVoucher: 'undefinedcrmebimage/public/order/proof.png',
      },
      html: '<img src="crmebimage/public/content/detail.png">',
      title: '商品详情',
    };

    normalizeCrmebMediaTree(data);

    expect(data.product.image).toBe('/crmebimage/public/product/main.png');
    expect(data.product.sliderImages[0]).toBe('/crmebimage/public/product/slider.png');
    expect(data.order.offlinePayVoucher).toBe('/crmebimage/public/order/proof.png');
    expect(data.html).toBe('<img src="/crmebimage/public/content/detail.png">');
    expect(data.title).toBe('商品详情');
  });

  it('builds upload file names from an optional custom name', () => {
    expect(buildUploadFileName('', 'original.jpg', 'png')).toBe('original.jpg');
    expect(buildUploadFileName('payment-code', 'original.jpg', 'png')).toBe('payment-code.jpg');
    expect(buildUploadFileName('payment-code.png', 'original.jpg', 'png')).toBe('payment-code.png');
    expect(buildUploadFileName('../bad:name', 'original.jpg', 'png')).toBe('bad_name.jpg');
  });
});
