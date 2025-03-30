@echo off
echo === DIIT-BPM 项目部署初始化 ===

REM 创建数据持久化目录
echo 创建数据持久化目录...
mkdir volumes\postgres 2>nul

REM 复制示例配置文件
echo 复制示例配置文件...
copy .env.example .env 2>nul

echo === 初始化完成 ===
echo 请根据实际情况修改 .env 文件中的配置
echo 然后运行 docker-compose up -d 开始部署 