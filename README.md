# 联志玩具礼品（苍梧）有限公司仓库管理系统

本项目为 Spring Boot + Vue 的仓库管理系统，包含仓库、供应商、商品、员工、入库、出库、盘点、库存流水以及系统管理（用户、部门、角色、权限）。

## 一键启动（Docker）

> 仅依赖 Docker/Docker Compose，本地不需要安装 Java/Node/PostgreSQL。

```bash
docker compose up --build
```

启动完成后：
- 前端：`http://localhost:49217`
- 后端：`http://localhost:48123/api-7f3b2`
- 数据库：`localhost:55491`（PostgreSQL）

## 登录信息

- 登录页不再展示默认账号和密码，避免在演示或截图时泄漏；请仅在 README 中查阅。
- 系统管理员：`admin` / `123456`
- 仓库保管员：`keeper` / `123456`
- 业务员工：`staff` / `123456`
- 不提供注册功能，仅通过默认账号登录后访问系统。
- 生产环境请在首次部署后立即修改默认密码。

## 说明

- 前后端以及数据库端口均为冷门端口，避免与常见服务冲突。
- 前端关联对象使用下拉选择（如仓库负责人、供应商、商品、员工、角色、权限等），无需手动输入 ID。
- 入库/出库单创建后会自动生成库存流水记录。
- 员工管理支持 CSV 导出；角色管理支持权限多选分配。

## 常用接口示例

```bash
# 登录
curl -X POST http://localhost:48123/api-7f3b2/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"<用户名>","password":"<密码>"}'

# 仓库列表
curl http://localhost:48123/api-7f3b2/warehouses

# 新增供应商
curl -X POST http://localhost:48123/api-7f3b2/suppliers \
  -H 'Content-Type: application/json' \
  -d '{"code":"S-001","name":"华南供应商","contactName":"刘工","phone":"13800001111"}'
```

## 项目结构

```
.
├── backend        # Spring Boot 服务
├── frontend       # Vue 前端
└── docker-compose.yaml
```
