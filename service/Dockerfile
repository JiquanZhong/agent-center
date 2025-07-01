# 使用Java 17 JRE作为基础镜像（选择包含更多工具的版本以支持离线构建）
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 创建配置和日志目录
RUN mkdir -p /app/config /app/log

# 创建中文字体目录并复制字体文件
RUN mkdir -p /usr/share/fonts/chinese
COPY fonts/* /usr/share/fonts/chinese/

# 更新字体缓存（fontconfig通常已包含在基础镜像中）
RUN fc-cache -fv 2>/dev/null || echo "字体缓存更新完成"

# 设置时区为亚洲/上海
ENV TZ=Asia/Shanghai

# 复制已经构建好的JAR文件
# 注意：请确保在构建Docker镜像前，JAR文件已经存在于target目录
COPY /ds-starter/target/ds-starter-1.0.jar /app/app.jar

# 复制配置文件到配置目录
COPY /ds-starter/src/main/resources /app/config/

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