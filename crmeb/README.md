# Template Store Backend

`crmeb` 目录包含后台 API、前台 API、公共模型和服务层代码。

## 说明

- `crmeb-admin`：后台管理 API。
- `crmeb-front`：H5/小程序前台 API。
- `crmeb-common`：公共模型、请求、响应和工具。
- `crmeb-service`：业务服务、DAO 和单元测试。
- `sql`：基础建库脚本和模板化清理脚本。

## 验证

所有 Maven 命令必须通过 Docker 运行。推荐使用仓库脚本：

```bash
scripts/docker/verify-builds.sh
```
