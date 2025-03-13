# 聊天机器人部署指南

## 1. 环境准备

### 1.1 所需镜像
请确保已经预先导入以下 Docker 镜像：
```bash
# 预先导入基础镜像
docker load -i ./images/nginx:alpine
docker load -i ./images/microservice-ds:1.0
```

## 2. 配置文件设置

### 2.1 修改 .env 文件
在项目根目录创建修改 `.env` 文件，配置以下环境变量：

```bash
# 自定义后端服务端口（必填）
BACKEND_PORT=9091

# 自定义前端服务端口（必填）
FRONTEND_PORT=80

# Java 服务相关配置（可选）
JAVA_OPTS=-Xmx512m -Xms512m

# Dify API配置（必填，Dify的API服务器，不要带v1和/）
DIFY_URL=http://your-dify-server:8080

# Docker主机IP（必填，当前服务器的ip填上去）
DOCKER_HOST_IP=192.168.11.205
```

## 3. 启动服务

### 3.1 基本启动命令
```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

## 4. 验证部署

1. 访问前端页面：
   - 打开浏览器，访问 `http://localhost:${FRONTEND_PORT}`
   
2. 验证后端服务：
   - 访问 `http://localhost:${BACKEND_PORT}`

## 5. 常见问题

1. 如果遇到端口冲突，请修改 .env 文件中的 FRONTEND_PORT 或 BACKEND_PORT
2. 如果前端无法连接后端，请检查 DOCKER_HOST_IP 配置是否正确
3. 日志文件位置：
   - 后端日志：`./log/`
   - Nginx日志：`./log/nginx/`

## 6. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除所有数据（谨慎使用）
docker-compose down -v
```

