# 一站式成长平台（One-Stop Future）

面向学生成长服务的课程设计项目，聚合首页、社区、岗位、资料、搜索、趋势、测评、通知、个人中心和基础管理端能力。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Security、MyBatis-Plus、H2/MySQL、MinIO、Redis
- 前端：Vue 3、Pinia、Vue Router、Axios、Vite、Vitest

## 项目结构

- `backend/`：后端服务
- `frontend/`：前端应用
- `docs/`：需求、设计、计划和项目详细说明
- `scripts/`：辅助脚本
- `docker-compose.yml`：可选容器化运行脚手架

## 本地运行

后端：

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

前端：

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

常用地址：

- 前端：`http://127.0.0.1:5173`
- 后端：`http://127.0.0.1:8080`

## 测试与构建

后端测试：

```bash
cd backend
mvn -q test
```

前端测试：

```bash
cd frontend
npm run test
```

前端构建：

```bash
cd frontend
npm run build
```

## 本地演示账号

使用 `local` profile 启动后端时可用：

- 管理员：`13800000000`
- 普通用户：`13800000001`
- 已认证用户：`13800000002`

登录方式：进入 `/login` 请求手机验证码；`local` profile 下后端会返回可直接使用的调试验证码。

## 可选配置

Redis 缓存默认关闭，需要演示缓存时设置：

```bash
REDIS_ENABLED=true
REDIS_CACHE_TTL_SECONDS=60
```

更多环境变量、MinIO、预览产物、迁移流程、权限说明和验收清单见详细文档。

## 文档索引

- 项目详细说明：[docs/project-details.md](docs/project-details.md)
- 产品规划与技术方案：[docs/校园一站式信息平台_产品规划与技术方案.md](docs/校园一站式信息平台_产品规划与技术方案.md)
- 需求与实施资料：[docs/superpowers/](docs/superpowers/)
- GitHub Actions 规则说明：[docs/github-actions-ruleset.md](docs/github-actions-ruleset.md)
