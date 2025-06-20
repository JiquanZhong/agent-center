# Ask Data AI - Docker 部署指南

## 📦 镜像构建

### 1. 构建Docker镜像

```bash
# 构建镜像（使用中国镜像源加速）
docker build -t ask-data-ai:latest .

# 或者指定版本号
docker build -t ask-data-ai:1.0.0 .

# 如果网络较慢，可以增加超时时间
docker build --network=host -t ask-data-ai:latest .
```

### 🚀 一键构建脚本（推荐）

我们提供了智能构建脚本，会自动测试镜像源并选择最佳构建方式：

**Linux/macOS:**
```bash
# 赋予执行权限
chmod +x build-docker.sh

# 运行构建脚本
./build-docker.sh

# 自定义镜像名和标签
./build-docker.sh -n myapp -t v1.0.0
```

**Windows PowerShell:**
```powershell
# 直接运行
bash build-docker.sh

# 或使用Git Bash
./build-docker.sh
```

### 🔄 如果构建失败，尝试备用镜像源

如果主Dockerfile构建失败，可以尝试使用备用Dockerfile：

```bash
# 使用备用Dockerfile（不同的中国镜像源）
docker build -f Dockerfile.china-alt -t ask-data-ai:latest .

# 如果仍然失败，可以尝试禁用缓存
docker build --no-cache -t ask-data-ai:latest .
```

### 🇨🇳 中国镜像源说明

Dockerfile已配置使用中国镜像源以加速下载：

- **apt源**: 阿里云镜像 (`mirrors.aliyun.com`)
- **pip源**: 清华大学镜像 (`pypi.tuna.tsinghua.edu.cn`)

#### 其他可用的国内镜像源：

**pip镜像源选项：**
```bash
# 清华大学（推荐）
https://pypi.tuna.tsinghua.edu.cn/simple

# 阿里云
https://mirrors.aliyun.com/pypi/simple

# 中科大
https://pypi.mirrors.ustc.edu.cn/simple

# 豆瓣
https://pypi.douban.com/simple
```

**Docker Hub镜像源：**
如果拉取基础镜像慢，可以配置Docker daemon使用国内镜像源：

```json
# /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://registry.docker-cn.com",
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ]
}
```

### 2. 查看构建结果

```bash
# 查看镜像列表
docker images

# 查看镜像详细信息
docker inspect ask-data-ai:latest
```

## 🚀 容器运行

### 基本运行

```bash
# 基本运行（使用默认端口8000）
docker run -d \
  --name ask-data-ai \
  -p 8000:8000 \
  ask-data-ai:latest

# 运行并查看日志
docker run --name ask-data-ai \
  -p 8000:8000 \
  ask-data-ai:latest
```

### 高级配置运行

```bash
# 使用自定义配置运行
docker run -d \
  --name ask-data-ai \
  -p 8080:8000 \
  -e DOCKER_HOST_IP=0.0.0.0 \
  -e ASK_DATA_PORT=8000 \
  -e RELOAD=false \
  -e POSTGRES_HOST=your-db-host \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=your-db-name \
  -e POSTGRES_USER=your-db-user \
  -e POSTGRES_PASSWORD=your-db-password \
  -e OPENAI_API_KEY=your-openai-key \
  -v /path/to/uploads:/app/uploads \
  -v /path/to/exports:/app/exports \
  ask-data-ai:latest
```

### 使用docker-compose（推荐）

创建 `docker-compose.yml` 文件：

```yaml
version: '3.8'

services:
  ask-data-ai:
    image: ask-data-ai:latest
    container_name: ask-data-ai
    ports:
      - "8000:8000"
    environment:
      - DOCKER_HOST_IP=0.0.0.0
      - ASK_DATA_PORT=8000
      - RELOAD=false
      - POSTGRES_HOST=postgres
      - POSTGRES_PORT=5432
      - POSTGRES_DB=askdata
      - POSTGRES_USER=askdata
      - POSTGRES_PASSWORD=password123
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./uploads:/app/uploads
      - ./exports:/app/exports
    depends_on:
      - postgres
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/api/ask-data/system/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  postgres:
    image: postgres:15-alpine
    container_name: ask-data-postgres
    environment:
      - POSTGRES_DB=askdata
      - POSTGRES_USER=askdata
      - POSTGRES_PASSWORD=password123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    restart: unless-stopped

volumes:
  postgres_data:
```

运行：

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f ask-data-ai

# 停止服务
docker-compose down
```

## 🔧 环境变量配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DOCKER_HOST_IP` | `0.0.0.0` | 服务绑定IP |
| `ASK_DATA_PORT` | `8000` | 服务端口 |
| `RELOAD` | `false` | 开发模式热重载 |
| `POSTGRES_HOST` | - | PostgreSQL主机地址 |
| `POSTGRES_PORT` | `5432` | PostgreSQL端口 |
| `POSTGRES_DB` | - | 数据库名称 |
| `POSTGRES_USER` | - | 数据库用户名 |
| `POSTGRES_PASSWORD` | - | 数据库密码 |
| `OPENAI_API_KEY` | - | OpenAI API密钥 |

## 📊 健康检查

访问健康检查端点：

```bash
# 检查服务状态
curl http://localhost:8000/api/ask-data/system/health

# 检查系统详细状态
curl http://localhost:8000/api/ask-data/system/status
```

## 📊 查看运行日志

### 实时查看日志
```bash
# 查看实时日志（推荐）
docker logs -f ask-data-ai

# 查看最近100行日志
docker logs --tail 100 ask-data-ai

# 查看特定时间段的日志
docker logs --since="2024-01-01T00:00:00" ask-data-ai
```

### 使用docker-compose查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 只查看ask-data-ai服务日志
docker-compose logs -f ask-data-ai

# 查看最近的日志条目
docker-compose logs --tail=50 ask-data-ai
```

### 日志级别和格式

应用程序日志包含以下信息：
- **时间戳**: 精确到秒的时间
- **日志级别**: DEBUG/INFO/WARNING/ERROR/CRITICAL
- **模块名称**: 日志来源模块
- **日志消息**: 具体的日志内容

示例日志输出：
```
14:32:45 | INFO     | ask_data_ai.startup  | 🚀 Ask Data AI 服务启动中...
14:32:45 | INFO     | ask_data_ai.startup  | ✅ 配置加载成功
14:32:45 | INFO     | ask_data_ai.startup  | 📊 数据库连接: localhost:5432/askdata
14:32:45 | INFO     | ask_data_ai.startup  | ✅ 数据库连接测试成功
14:32:45 | INFO     | ask_data_ai.startup  | ✅ Ask Data AI 服务启动完成
14:32:45 | INFO     | ask_data_ai.server   | 🌐 启动Ask Data AI Web服务...
```

## 🐛 故障排除

### 查看容器状态
```bash
# 查看运行状态
docker ps

# 查看容器详细信息
docker inspect ask-data-ai

# 进入容器调试
docker exec -it ask-data-ai /bin/bash

# 查看容器资源使用情况
docker stats ask-data-ai
```

### 常见问题

1. **端口冲突**：修改映射端口 `-p 8080:8000`
2. **数据库连接失败**：检查数据库配置环境变量
3. **权限问题**：确保挂载目录有正确权限
4. **内存不足**：增加Docker可用内存

## 📖 API文档

服务启动后，访问以下地址：

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **健康检查**: http://localhost:8000/api/ask-data/system/health

## 🔐 安全建议

1. 使用非root用户运行容器
2. 限制容器资源使用
3. 定期更新基础镜像
4. 使用secrets管理敏感信息
5. 配置防火墙规则 