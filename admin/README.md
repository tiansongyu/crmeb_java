# Template Store Admin Web

`admin` 目录是后台管理前端。模板版本中移除了演示店铺展示数据，后台商品列表支持 JSON 批量导入。

## 商品批量导入

入口：商品列表页 `批量导入`。

流程：

1. 下载模板 JSON。
2. 选择本地 JSON 文件。
3. 先执行预览校验。
4. 校验无失败后确认导入。

## 编译要求

前端构建必须使用 Docker，不在宿主机直接执行 npm 构建。

```bash
TAG_SUFFIX=template scripts/docker/verify-builds.sh
```
