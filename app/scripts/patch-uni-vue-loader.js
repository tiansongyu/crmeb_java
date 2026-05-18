const fs = require('fs')
const path = require('path')

const loaderPath = path.join(
  __dirname,
  '..',
  'node_modules',
  '@dcloudio',
  'vue-cli-plugin-uni',
  'packages',
  'vue-loader',
  'lib',
  'loaders',
  'templateLoader.js'
)

if (!fs.existsSync(loaderPath)) {
  throw new Error(`uni vue-loader template loader not found: ${loaderPath}`)
}

const source = fs.readFileSync(loaderPath, 'utf8')
const current = 'return code + `\\nexport { render, staticRenderFns, recyclableRender, components }`'
const patched = [
  'if (!/\\\\b(recyclableRender|components)\\\\b/.test(code)) {',
  '    return code + `\\nvar recyclableRender, components\\nexport { render, staticRenderFns, recyclableRender, components }`',
  '  }',
  '  return code + `\\nexport { render, staticRenderFns, recyclableRender, components }`'
].join('\n  ')

if (source.includes(patched)) {
  console.log('uni vue-loader template loader already patched')
} else if (source.includes(current)) {
  fs.writeFileSync(loaderPath, source.replace(current, patched))
  console.log('patched uni vue-loader template loader')
} else {
  throw new Error('unexpected uni vue-loader template loader content')
}
