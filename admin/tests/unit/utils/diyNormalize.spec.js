import { applyComponentDefaults } from '@/views/design/utils/diyNormalize';

describe('Design diyNormalize', () => {
  it('fills missing preview component fields from mobile component defaults', () => {
    const section = {
      name: 'goodList',
      timestamp: 1001,
      nested: {
        keep: 'saved',
      },
    };
    const components = [
      {
        defaultName: 'goodList',
        data() {
          return {
            defaultConfig: {
              name: 'goodList',
              themeStyleConfig: { tabVal: 0 },
              nested: {
                keep: 'default',
                missing: 'filled',
              },
            },
          };
        },
      },
    ];

    applyComponentDefaults(section, components);

    expect(section.themeStyleConfig.tabVal).toBe(0);
    expect(section.nested.keep).toBe('saved');
    expect(section.nested.missing).toBe('filled');
  });
});
