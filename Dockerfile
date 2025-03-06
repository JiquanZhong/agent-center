# 使用Java 17 JRE作为基础镜像
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 创建配置和日志目录
RUN mkdir -p /app/config /app/log

# 设置时区为亚洲/上海
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制已经构建好的JAR文件
# 注意：请确保在构建Docker镜像前，JAR文件已经存在于target目录
COPY target/microservice-ds-1.0.jar /app/app.jar

# 复制配置文件到配置目录
COPY src/main/resources/ /app/config/

# 暴露9091端口
EXPOSE 9091

# 设置卷挂载点
VOLUME ["/app/config", "/app/log"]

# 设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget --quiet --tries=1 --spider http://localhost:9091/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=9091 --spring.config.location=/app/config/ --logging.file.path=/app/log/"] 