# Releasing

目标：每次版本迭代都清晰记录三件事：
1) 增加了什么（What）
2) 为什么要增加（Why）
3) 解决了什么问题（Problem Solved）

## 约定
- 每次发布都要更新 `CHANGELOG.md`（摘要）与 `docs/releases/<version>.md`（详细说明）。
- Git tag 使用 `vX.Y.Z`（例如 `v0.4.0`）。

## 发布步骤（建议）
1) 代码变更合并到 `main`
2) 更新版本号（`pom.xml` 的 `<version>`）
3) 更新变更记录
   - `CHANGELOG.md`
   - `docs/releases/<version>.md`（从 `docs/releases/TEMPLATE.md` 复制）
4) 本地构建验证
   - `mvn -q -DskipTests package`
5) 提交与打标签
   - `git add -A`
   - `git commit -m "vX.Y.Z: <summary>"`
   - `git tag -a vX.Y.Z -m "vX.Y.Z"`
6) 推送
   - `git push origin main`
   - `git push origin --tags`

