FROM openjdk:8-jdk-alpine

WORKDIR /app

# 复制JAR文件
COPY diit-system-web.jar /app/
COPY application.yml /app/
COPY logback-spring.xml /app/

# 暴露端口
EXPOSE ${OPERATION_BACKEND_PORT}

# 启动命令
ENTRYPOINT ["java", "-jar", "diit-system-web.jar"] 