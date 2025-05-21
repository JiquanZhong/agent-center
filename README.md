# 聊天机器人部署指南

## 1. 配置文件设置

### 1.1 修改 .env 文件
在项目根目录创建修改 `.env` 文件，配置以下环境变量：

```bash
# 端口配置
BACKEND_PORT=9091 # 后端端口
FRONTEND_PORT=8088 # 前端端口
OPERATION_FRONTEND_PORT=32001 # 运维前端端口
OPERATION_BACKEND_PORT=32003 # 运维后端端口

OPERATION_BACKEND_CONTEXT_PATH=diit-system # 运维后端API前缀

# 宿主机IP
DOCKER_HOST_IP=192.168.80.60 # docker compose启动的宿主机IP

# 服务URL配置
DIFY_URL=http://192.168.11.205 # Dify服务地址
DIFY_CHAT_AGENT_TOKEN=Bearer app-Ug8kHQck7eFWEQ9I3WFcNk7J # Dify聊天机器人token
DIFY_APP_ID=00dde580-5faa-4345-b351-af6531253aa0 # Dify应用ID，暂时没用上
DIFY_SIGNATURE=sk-9f73s3ljTXVcMT3Blb3ljTqtsKiGHXVcMT3BlbkFJLK7U # Dify签名，暂时没用上

# RAGFlow配置
RAGFLOW_URL=http://192.9.100.11:8000 # RAGFlow服务地址
RAGFLOW_API_KEY=Bearer ragflow-lkODFmN2Y0MGFjYTExZjBiYjE2MDI0Mm Y2YjE3ZDY # RAGFlow API Key

# DIOS服务地址
DIOS_KDB_URL=http://wuhan.diit.cn:8000 # 武汉的DIOS的服务地址

# 其他配置
JAVA_OPTS=-Xms256m -Xmx512m # Java内存配置

# 数据库配置
POSTGRES_HOST=${DOCKER_HOST_IP} 
POSTGRES_PORT=5433 # PostgreSQL启动端口，后土和运维的数据都在这个数据库中，数据文件持久化在volumes里
POSTGRES_OPERATION_DB=oa # 运维数据库
POSTGRES_HOUTU_DB=houtu # 后土数据库
POSTGRES_USER=postgres 
POSTGRES_PASSWORD=postgres

# Dify数据库配置
DIFY_DB_HOST=192.168.11.205 # Dify数据库地址
DIFY_DB_PORT=5432 # Dify数据库端口
DIFY_DB_NAME=dify # Dify数据库名称
DIFY_DB_USER=postgres # Dify数据库用户名
DIFY_DB_PASSWORD=difyai123456 # Dify数据库密码

# RAGFlow数据库配置
RAGFLOW_DB_HOST=192.9.100.11 # RAGFlow数据库地址
RAGFLOW_DB_PORT=5455 # RAGFlow数据库端口
RAGFLOW_DB_NAME=rag_flow # RAGFlow数据库名称
RAGFLOW_DB_USER=root # RAGFlow数据库用户名
RAGFLOW_DB_PASSWORD=infini_rag_flow # RAGFlow数据库密码

# 国土空间信息平台SSO地址
AUTH_TYPE=JWT # 默认是JWT，如果是在国土空间平台则改为SSO 
SSO_VALIDATION_URL=http://192.168.164.45:8021/datahub/usercenter/userinfo/validation

```

## 2. 启动服务

### 2.1 基本启动命令
```bash
# 启动所有服务，启动之前需要确保service/ds-starter/target/ds-starter-1.0.jar存在，否则无法启动
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

## 3. 验证部署

1. 访问前端页面：
   - 打开浏览器，访问 `http://localhost:${FRONTEND_PORT}/ai_center`
   - 默认用户名和密码为 `admin` / `Abc@Diit!123`
   
2. 验证后端服务：
   - 访问 `http://localhost:${BACKEND_PORT}/doc.html`

3. 运维前端：
   - 访问 `http://localhost:${OPERATION_FRONTEND_PORT}/diit-bpm-vue`
   - 默认用户名和密码为 `admin` / `admin`，这里只做了前端页面的鉴权，没查数据库

## 4. 常见问题

1. 如果遇到端口冲突，请修改 .env 文件中的 FRONTEND_PORT 或 BACKEND_PORT
2. 如果前端无法连接后端，请检查 DOCKER_HOST_IP 配置是否正确
3. 日志文件位置：
   - 后端日志：`./log/`
   - Nginx日志：`./log/nginx/`

## 5. 停止服务

```bash
# 停止所有服务
docker-compose down
```

