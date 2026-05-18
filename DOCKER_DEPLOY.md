# Docker 部署说明

本仓库按技术栈拆成 5 类容器：

- `mysql`: MySQL 5.7，首次启动自动导入 `crmeb/sql/Crmeb_v1.4.sql`
- `redis`: Redis 6.2
- `crmeb-admin`: Spring Boot 管理端 API，默认端口 `20400`
- `crmeb-front`: Spring Boot 移动端 API，默认端口 `20410`
- `admin-web`: PC 管理端 Vue 静态站点，默认端口 `8080`
- `app-h5`: uni-app H5 静态站点，默认端口 `8082`，需要先生成 H5 构建产物

## 启动后端和 PC 管理端

```bash
cp .env.example .env
./depoly.sh
```

访问：

- PC 管理端：`http://localhost:8080`
- 管理端 API：`http://localhost:20400`
- 移动端 API：`http://localhost:20410`

首次启动 MySQL 会导入初始化 SQL。若已经存在 `mysql_data` 卷，SQL 不会重复导入；需要重建库时先执行：

```bash
docker compose down -v
docker compose up -d --build
```

## 启动移动端 H5

当前 `app` 目录是 HBuilderX/uni-app 项目，仓库没有完整的 npm CLI 构建依赖。先用 HBuilderX 或现有发布流程生成：

```text
app/unpackage/dist/build/h5/index.html
```

再启动 H5 容器：

```bash
./depoly.sh --with-app-h5
```

访问：`http://localhost:8082`

## 配置

默认配置在 `.env.example`。后端运行时会通过环境变量覆盖数据库、Redis、上传目录：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `CRMEB_IMAGE_PATH`

上传文件挂载在 Docker 卷 `upload_data` 下，两个 Java API 容器共享该卷。

## 日常更新

修改任意后端或 PC 管理端代码后，直接执行：

```bash
./depoly.sh
```

部署完成后可以运行接口 smoke tests：

```bash
./scripts/test-interfaces.sh
```

测试会覆盖后台页面、后台 API 代理、登录验证码代理、管理端文档页、移动端公开接口和分类/商品接口。默认地址来自本地 Docker 端口，也可以用环境变量覆盖：

```bash
ADMIN_WEB=http://127.0.0.1:8080 \
ADMIN_API=http://127.0.0.1:20400 \
FRONT_API=http://127.0.0.1:20410 \
./scripts/test-interfaces.sh
```

常用选项：

- `./depoly.sh --with-app-h5`: 同时更新移动端 H5 容器
- `./depoly.sh --no-cache`: 不使用 Docker 缓存重新构建
- `./depoly.sh --pull`: 先拉取 MySQL/Redis 基础镜像
- `./depoly.sh --reset-db`: 删除容器卷并重新初始化数据库，已有数据会被清空

另外保留了同义入口：

```bash
./deploy.sh
```
