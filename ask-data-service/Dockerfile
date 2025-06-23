# Ask Data AI - Docker部署文件
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONPATH=/app
ENV PYTHONUNBUFFERED=1
ENV DOCKER_HOST_IP=0.0.0.0
ENV ASK_DATA_PORT=8000
# 确保日志立即输出，不使用缓冲
ENV PYTHONIOENCODING=UTF-8

# 使用中国镜像源 - 替换apt源
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources && \
    sed -i 's/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources

# 安装系统依赖和中文字体
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    curl \
    # 中文字体支持
    fonts-wqy-microhei \
    fonts-wqy-zenhei \
    fonts-noto-cjk \
    fonts-dejavu-core \
    fontconfig \
    && fc-cache -f -v \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 使用中国pip镜像源安装Python依赖
RUN pip install --no-cache-dir -r requirements.txt \
    -i https://pypi.tuna.tsinghua.edu.cn/simple \
    --trusted-host pypi.tuna.tsinghua.edu.cn

# 复制项目文件
COPY . .

# 创建必要的目录
RUN mkdir -p uploads exports logs

# 设置日志目录权限
RUN chmod 755 logs

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8000/api/ask-data/system/health || exit 1

# 启动命令 - 使用环境变量而不是命令行参数
CMD ["python", "app.py"]