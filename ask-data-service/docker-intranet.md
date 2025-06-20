# Ask Data AI - 内网环境Docker构建指南

## 🔒 内网环境构建方案

针对不同的内网环境，我们提供了多种Docker构建方案：

### 📋 方案对比

| 方案 | 适用场景 | 网络要求 | 难度 | 推荐度 |
|------|----------|----------|------|--------|
| **代理构建** | 有HTTP代理 | 需要代理配置 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **离线构建** | 完全内网 | 无网络连接 | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **私有镜像仓库** | 企业环境 | 内网仓库 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **多阶段构建** | 混合环境 | 部分网络 | ⭐⭐⭐ | ⭐⭐⭐ |

---

## 🔥 方案一：代理构建（推荐）

### 适用场景
- 内网有HTTP/HTTPS代理
- 可以通过代理访问外网

### 构建步骤

```bash
# 设置代理环境变量
export HTTP_PROXY=http://proxy.company.com:8080
export HTTPS_PROXY=http://proxy.company.com:8080
export NO_PROXY=localhost,127.0.0.1,.company.com

# 使用代理构建
docker build \
  --build-arg HTTP_PROXY=${HTTP_PROXY} \
  --build-arg HTTPS_PROXY=${HTTPS_PROXY} \
  --build-arg NO_PROXY=${NO_PROXY} \
  -f Dockerfile.proxy \
  -t ask-data-ai:latest .
```

### Docker daemon代理配置

创建或编辑 `/etc/docker/daemon.json`：

```json
{
  "proxies": {
    "default": {
      "httpProxy": "http://proxy.company.com:8080",
      "httpsProxy": "http://proxy.company.com:8080",
      "noProxy": "localhost,127.0.0.1,.company.com"
    }
  }
}
```

重启Docker服务：
```bash
sudo systemctl restart docker
```

---

## 📦 方案二：离线构建

### 适用场景
- 完全无外网访问
- 安全要求极高的环境

### 准备阶段（在有网络的环境中）

```bash
# 1. 下载Python依赖包
chmod +x scripts/prepare-offline.sh
./scripts/prepare-offline.sh

# 2. 保存基础镜像
docker pull python:3.11-slim
docker save python:3.11-slim > python-3.11-slim.tar

# 3. 打包整个项目
tar -czf ask-data-offline.tar.gz . \
    --exclude='.git' \
    --exclude='*.log' \
    --exclude='__pycache__'
```

### 传输到内网环境

```bash
# 传输文件到内网服务器
scp ask-data-offline.tar.gz user@intranet-server:/tmp/
scp python-3.11-slim.tar user@intranet-server:/tmp/
```

### 内网环境构建

```bash
# 1. 解压项目文件
cd /opt/ask-data-ai
tar -xzf /tmp/ask-data-offline.tar.gz

# 2. 加载基础镜像
docker load < /tmp/python-3.11-slim.tar

# 3. 离线构建
docker build -f Dockerfile.offline -t ask-data-ai:latest .
```

---

## 🏢 方案三：私有镜像仓库

### 适用场景
- 企业有私有Docker仓库
- 有内网镜像同步机制

### Harbor私有仓库示例

```bash
# 1. 登录私有仓库
docker login harbor.company.com

# 2. 使用私有基础镜像
# 修改Dockerfile第一行：
# FROM harbor.company.com/library/python:3.11-slim

# 3. 构建并推送
docker build -t harbor.company.com/ask-data/ask-data-ai:latest .
docker push harbor.company.com/ask-data/ask-data-ai:latest
```

### Nexus私有仓库示例

```bash
# 1. 配置Docker使用Nexus
# /etc/docker/daemon.json
{
  "registry-mirrors": ["http://nexus.company.com:8082"],
  "insecure-registries": ["nexus.company.com:8082"]
}

# 2. 构建
docker build -t nexus.company.com:8082/ask-data-ai:latest .
```

---

## 🔧 方案四：多阶段构建

### 适用场景
- 构建机有网络，运行环境无网络
- 需要最小化镜像大小

### 多阶段Dockerfile示例

```dockerfile
# 构建阶段（需要网络）
FROM python:3.11-slim as builder

WORKDIR /app
COPY requirements.txt .

# 下载并安装依赖到临时目录
RUN pip install --user -r requirements.txt \
    -i https://pypi.tuna.tsinghua.edu.cn/simple

# 运行阶段（无需网络）
FROM python:3.11-slim

WORKDIR /app

# 从构建阶段复制已安装的包
COPY --from=builder /root/.local /root/.local

# 复制应用代码
COPY . .

# 设置PATH
ENV PATH=/root/.local/bin:$PATH

CMD ["python", "app.py"]
```

---

## 🛠️ 通用优化技巧

### 1. 减少镜像层数

```dockerfile
# ❌ 多个RUN命令
RUN apt-get update
RUN apt-get install -y gcc
RUN apt-get install -y g++

# ✅ 合并RUN命令
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*
```

### 2. 使用.dockerignore

创建 `.dockerignore` 文件：

```
.git
*.log
__pycache__
*.pyc
.pytest_cache
node_modules
.env
*.md
```

### 3. 缓存优化

```dockerfile
# ✅ 先复制requirements.txt，利用缓存
COPY requirements.txt .
RUN pip install -r requirements.txt

# 再复制其他文件
COPY . .
```

---

## 🔍 故障排除

### 常见问题及解决方案

#### 1. 代理认证失败
```bash
# 如果代理需要用户名密码
HTTP_PROXY=http://username:password@proxy.company.com:8080
```

#### 2. SSL证书问题
```bash
# 跳过SSL验证（仅用于内网环境）
pip install --trusted-host pypi.org --trusted-host pypi.python.org
```

#### 3. DNS解析问题
```bash
# 使用IP地址代替域名
docker build --add-host=pypi.org:151.101.1.223 .
```

#### 4. 基础镜像无法拉取
```bash
# 使用内网镜像仓库
docker pull internal-registry.com/python:3.11-slim
docker tag internal-registry.com/python:3.11-slim python:3.11-slim
```

---

## 📊 性能对比

| 方案 | 构建时间 | 镜像大小 | 维护成本 | 安全性 |
|------|----------|----------|----------|---------|
| 代理构建 | 5-10分钟 | 正常 | 低 | 中 |
| 离线构建 | 2-5分钟 | 较大 | 中 | 高 |
| 私有仓库 | 3-8分钟 | 正常 | 高 | 高 |
| 多阶段构建 | 8-15分钟 | 最小 | 中 | 中 |

---

## 🎯 推荐方案选择

- **有代理环境** → 使用方案一（代理构建）
- **完全离线** → 使用方案二（离线构建）
- **企业环境** → 使用方案三（私有仓库）
- **对镜像大小敏感** → 使用方案四（多阶段构建） 