# 商城模板

这是一个用于二次开发的商城模板项目。仓库保留商品、订单、会员、营销活动、后台管理、H5 前端、手动扫码收款和可切换支付模式等核心能力，但初始化数据已清理为中性模板。

## 模板约定

- 初始化库不包含示例商品、示例订单、示例用户或真实公司展示信息。
- 默认保留三个占位商品分类：`模板分类一`、`模板分类二`、`模板分类三`。
- 默认支付模式为微信扫码转账人工确认，后台仍保留微信在线支付接口与一键切换能力。
- 后台可通过商品列表页的 `批量导入` 上传 JSON 文件快速创建商品。

## Docker 编译要求

本仓库的 Maven、npm、前端构建和完整验证必须在 Docker 中执行，不在宿主机直接编译。

常用验证命令：

```bash
scripts/docker/verify-template-sql.sh
TAG_SUFFIX=template scripts/docker/verify-builds.sh
```

本地启动可使用 Docker Compose：

```bash
docker compose up -d --build
```
