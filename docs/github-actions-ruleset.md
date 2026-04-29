# GitHub Actions 与 Ruleset 配置说明

这个仓库已经补好了基础 CI，提交后只需要在 GitHub 仓库设置里把 ruleset 绑定到对应检查项即可。

## 已添加的自动化检查

- Workflow 文件：`.github/workflows/ci.yml`
- 必过后端检查名：`backend-tests`
- 必过前端检查名：`frontend-tests`

CI 触发时机：

- 所有 `pull_request`
- 所有分支的 `push`
- 手动触发 `workflow_dispatch`

检查内容：

- `backend-tests`
  - JDK `17`
  - 在 `backend/` 目录执行 `mvn -B -ntp test`
- `frontend-tests`
  - Node.js `20`
  - 在 `frontend/` 目录执行 `npm ci`
  - 执行 `npm test`
  - 执行 `npm run build`

## 为什么这样配更适合 ruleset

这个 workflow 没有使用 `paths` 级别的过滤。

原因是如果把 workflow 本身配置成“只在某些目录改动时才运行”，而 ruleset 又要求这个检查必须通过，那么在未命中路径过滤时，GitHub 很容易把 required check 留在 `Pending`，反而卡住合并。

现在这套配置的思路是：

- PR 一打开就稳定地产生同名检查
- ruleset 只绑定固定的两个检查名
- 不需要额外猜测“这次改动要不要跑 CI”

## GitHub Ruleset 推荐配置

在 GitHub 仓库中打开：

- `Settings`
- `Rules`
- `Rulesets`
- `New ruleset`
- 选择 `Branch ruleset`

推荐这样配置：

1. `Target branches`
   - 选择默认分支，通常是 `main`
2. `Require a pull request before merging`
   - 开启
3. `Require status checks to pass`
   - 开启
   - 勾选 `backend-tests`
   - 勾选 `frontend-tests`
4. `Require branches to be up to date before merging`
   - 建议开启
5. `Require conversation resolution before merging`
   - 建议开启
6. `Require approvals`
   - 按团队习惯决定，个人项目可不开，多人协作建议至少 `1`

## 实际启用顺序

首次配置 ruleset 时，建议按这个顺序做：

1. 先把这次修改提交并推到 GitHub。
2. 开一个 PR，让 `CI` workflow 至少完整跑过一次。
3. 等 GitHub 识别出 `backend-tests` 和 `frontend-tests` 两个检查名。
4. 再去 ruleset 里把这两个检查设置为 required。

这样可以避免 ruleset 页面里暂时选不到新检查名。

## 本地复现命令

后端：

```bash
cd backend
mvn -B -ntp test
```

前端：

```bash
cd frontend
npm ci
npm test
npm run build
```

## 后续维护注意事项

- 如果以后修改了 job 名称，ruleset 里的 required checks 也要同步改。
- 如果以后拆成多个 workflow，也要继续保证 required check 名称稳定。
- 后端测试当前本地实测约 `2` 分钟，前端测试与构建约 `30` 秒左右，适合作为 PR 合并前的基础门禁。
