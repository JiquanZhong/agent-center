# DIIT-BPM 项目 Docker Compose 部署指南

本文档提供了使用 Docker Compose 一键部署 DIIT-BPM 项目的详细步骤和说明。

## 部署架构

项目包含三个主要组件：
1. 前端项目（基于 Nginx）
2. 后端项目（基于 JDK8 的 SpringBoot 应用）
3. PostgreSQL 数据库

## 前置条件

- 安装 Docker: 20.10.0 或更高版本
- 安装 Docker Compose: 1.29.0 或更高版本
- 确保服务器能够访问互联网下载 Docker 镜像

## 文件结构

```
diit-bpm-docker/
├── .env                      # 环境变量配置文件
├── docker-compose.yml        # Docker Compose 配置文件
├── system-server/            # 后端服务相关文件
│   ├── Dockerfile            # 后端服务 Dockerfile
│   ├── application.yml       # 后端服务配置文件
│   ├── diit-system-web.jar   # 后端服务 JAR 包
│   └── logback-spring.xml    # 日志配置文件
├── nginx/                    # 前端服务相关文件
│   ├── Dockerfile            # 前端服务 Dockerfile
│   └── nginx.conf            # Nginx 配置文件
├── diit-bpm-vue/             # 前端静态文件
├── volumes/                  # 数据持久化目录
│   └── postgres/             # PostgreSQL数据目录
└── README.md                 # 部署文档
```

## 部署步骤

### 1. 准备工作

1. 创建部署目录并进入:
   ```bash
   mkdir -p diit-bpm-docker
   cd diit-bpm-docker
   ```

2. 创建数据持久化目录:
   ```bash
   mkdir -p volumes/postgres
   ```

3. 复制项目相关文件到对应目录:
   ```bash
   # 将前端文件复制到对应目录
   cp -r /path/to/diit-bpm-vue .
   
   # 确保后端 JAR 包在 system-server 目录中
   ```

### 2. 配置环境变量

编辑 `.env` 文件，根据实际情况修改环境变量:

```
# 数据库配置
POSTGRES_DB=oa
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5433  # 宿主机访问PostgreSQL的端口

# 后端服务配置
BACKEND_PORT=32003
BACKEND_CONTEXT_PATH=diit-system

# 前端服务配置
NGINX_PORT=82
```

### 3. 启动服务

在项目根目录下运行以下命令启动所有服务:

```bash
docker-compose up -d
```

此命令会自动构建镜像并启动容器。首次运行可能需要几分钟时间。

### 4. 查看服务状态

运行以下命令查看各服务的运行状态:

```bash
docker-compose ps
```

所有服务状态应为 `Up`。

## 部署验证

### 验证前端服务

在浏览器中访问:
```
http://服务器IP地址:82/diit-bpm-vue
```

如果前端页面正常显示，说明前端服务部署成功。

### 验证后端服务

可以通过访问 API 接口来验证后端服务是否正常运行:
```
http://服务器IP地址:32003/diit-system/api/health
```

如果返回正常响应，说明后端服务部署成功。

### 验证数据库连接

可以通过查看后端服务日志来验证数据库连接是否正常:

```bash
docker-compose logs backend
```

如果日志中没有数据库连接错误，说明数据库连接正常。

## 常见问题及解决方案

### 1. 无法启动服务

检查 Docker 和 Docker Compose 版本:
```bash
docker --version
docker-compose --version
```

确保端口没有被占用:
```bash
netstat -an | findstr 82     # 检查 Nginx 端口 (Windows)
netstat -an | findstr 32003  # 检查后端端口 (Windows)
netstat -an | findstr 5433   # 检查数据库端口 (Windows)
```

### 2. 数据库连接失败

**重要注意事项**: 在Docker Compose环境中，服务间通信使用服务名称作为主机名，并使用容器内部端口进行连接。PostgreSQL在容器内监听的是默认端口5432，而不是宿主机映射的端口5433。

如果遇到数据库连接错误，请检查：
- 确保docker-compose.yml中后端服务的DB_PORT环境变量设置为5432
- 确保application.yml中的db.port配置正确

检查后端日志:
```bash
docker-compose logs backend
```

### 3. 无法访问前端页面

检查 Nginx 配置和日志:
```bash
docker-compose logs frontend
```

确认浏览器访问的 URL 是否正确，包括端口号。

## 数据持久化

PostgreSQL的数据现在存储在宿主机的`./volumes/postgres`目录中，这样可以确保：
- 容器删除后数据不会丢失
- 方便直接备份数据文件
- 方便迁移和升级

**注意**: 首次启动后，该目录的所有者将更改为容器内PostgreSQL用户。不要随意修改此目录的权限。

## 服务管理命令

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart
```

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs frontend
docker-compose logs backend
docker-compose logs postgres
```

### 更新配置后重新部署
```bash
# 修改配置文件后
docker-compose down
docker-compose up -d --build
```

## 数据备份与恢复

### 方法一：使用PostgreSQL命令

#### 备份数据库
```bash
docker-compose exec postgres pg_dump -U postgres oa > backup.sql
```

#### 恢复数据库
```bash
cat backup.sql | docker-compose exec -T postgres psql -U postgres -d oa
```

### 方法二：直接备份数据目录

#### 备份数据目录
```bash
# 确保数据库未运行或处于一致状态
docker-compose stop postgres
tar -czf postgres_data_backup.tar.gz volumes/postgres
docker-compose start postgres
```

#### 恢复数据目录
```bash
# 停止PostgreSQL容器
docker-compose stop postgres
# 删除旧数据（小心操作！）
rm -rf volumes/postgres/*
# 解压备份文件
tar -xzf postgres_data_backup.tar.gz
# 重启PostgreSQL容器
docker-compose start postgres
``` 