# Lowcode BI Platform

面向企业的多租户低代码BI报表与自助数据分析平台。

## 功能特性

### 数据源管理
- 支持多种数据库连接：MySQL、PostgreSQL、ClickHouse
- CSV文件上传（最大100MB，自动推断列类型）
- 连接池独立隔离，租户间互不影响
- 密码加密存储，管理界面不回显明文

### 数据建模
- 表关联关系定义（一对一/一对多/多对多）
- 计算字段（四则运算、条件判断、日期函数、字符串处理）
- 度量定义（SUM/AVG/COUNT/COUNTDISTINCT/MAX/MIN）
- 维度层级（年→季度→月→日，省→市→区）

### 报表设计器
- 拖拽式画布设计
- 支持多种图表组件：折线图、柱状图、饼图、散点图、表格、指标卡、漏斗图、地图
- 组件间联动筛选
- 网格对齐，自由调整大小位置

### 交互式分析
- 维度下钻
- 全局/局部筛选器
- 时间对比（同比/环比）
- 点击排序

### 仪表板管理
- 多Tab页支持
- 全屏展示模式
- 深色/浅色主题切换
- 仪表板复制、模板功能
- 自动刷新配置

### 定时刷新与推送
- Cron表达式定时任务
- Headless浏览器截图
- 邮件推送
- 超时处理机制

### 权限控制
- 租户完全隔离
- 角色权限：管理员、编辑者、查看者
- 行级数据权限
- 仪表板级权限

### SQL查询模式
- 语法高亮
- 表名/字段名自动补全
- 参数化查询
- 安全限制（仅允许SELECT）

### 嵌入式集成
- iframe嵌入
- JWT Token鉴权
- 可配置隐藏元素
- 行级权限参数携带

### 性能优化
- 查询缓存（用户级粒度）
- 预聚合
- 查询超时控制
- 前端大数据量处理

## 技术栈

### 后端
- Java 17
- Spring Boot 3.x
- PostgreSQL 16
- ClickHouse 23
- Redis 7
- Flyway（数据库迁移）
- Quartz（定时任务）
- Selenium（截图渲染）

### 前端
- React 18
- TypeScript
- Ant Design 5
- ECharts 5
- DnD-Kit（拖拽交互）
- Monaco Editor（SQL编辑器）
- Vite

### 部署
- Docker
- Docker Compose
- Nginx

## 快速开始

### 环境要求
- Docker 20.10+
- Docker Compose 2.0+

### 部署步骤

1. 克隆项目
```bash
git clone <repository-url>
cd lowcode-bi
```

2. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 文件，修改密码等配置
```

3. 启动服务
```bash
docker-compose up -d
```

4. 访问应用
- 前端：http://localhost
- 后端API：http://localhost/api
- 默认账号：admin / admin123

### 开发模式

#### 后端开发
```bash
cd backend
mvn spring-boot:run
```

#### 前端开发
```bash
cd frontend
npm install
npm run dev
```

## 项目结构

```
lowcode-bi/
├── backend/                 # 后端Spring Boot项目
│   ├── src/
│   │   └── main/
│   │       ├── java/com/lowcode/bi/
│   │       └── resources/
│   └── pom.xml
├── frontend/               # 前端React项目
│   ├── src/
│   └── package.json
├── deploy/                 # 部署配置
│   ├── sql/               # 数据库初始化脚本
│   └── nginx.conf        # Nginx配置
├── docker-compose.yml      # Docker Compose配置
├── .env.example           # 环境变量示例
└── README.md
```

## 业务规则

### 安全规则
- 计算字段表达式采用安全沙箱，仅允许白名单函数
- 行级权限使用参数化查询注入，防止SQL注入
- SQL查询禁止DDL/DML语句，仅允许SELECT

### 性能规则
- 每个租户最多同时占用20个数据库连接
- CSV文件超过500列拒绝导入
- 表格超过10000行自动分页
- 图表数据点超过5000自动采样

### 容错规则
- 连续3次刷新失败自动暂停并通知管理员
- 查询超时60秒自动终止
- 数据源查询超时30秒标记为超时

## License

MIT License
