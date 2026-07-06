# Template Store Admin Web

`admin` 目录是后台管理前端。模板版本中移除了演示店铺展示数据，后台商品列表支持 Excel 批量导入。

## 商品批量导入

入口：商品列表页 `批量导入`。

流程：

1. 下载模板 Excel。
2. 选择本地 Excel 文件。
3. 先执行预览校验。
4. 校验无失败后确认导入。

Excel 规则：

- `商品编码` 相同的多行会合并为同一个商品。
- 每一行代表一个 SKU。
- 图片字段填写后台图片库路径；多张轮播图用英文逗号分隔。
- JSON 导入接口仍保留给高级场景，后台默认入口使用 Excel。

## 编译要求

前端构建必须使用 Docker，不在宿主机直接执行 npm 构建。

```bash
TAG_SUFFIX=template scripts/docker/verify-builds.sh
```
