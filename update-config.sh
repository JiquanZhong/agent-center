#!/bin/bash

# 获取宿主机IP（优先使用环境变量中的IP）
HOST_IP=${DOCKER_HOST_IP:-$(ip route show default | awk '/default/ {print $3}')}
BACKEND_PORT=${BACKEND_PORT:-9091}
CONFIG_FILE="/usr/share/nginx/html/config/serveConfig.js"

# 检查配置文件是否存在
if [ ! -f "$CONFIG_FILE" ]; then
    echo "错误：配置文件 $CONFIG_FILE 不存在"
    exit 1
fi

# 检查文件内容是否包含要修改的配置
if ! grep -q "houtuServeConfig" "$CONFIG_FILE"; then
    echo "错误：配置文件格式不正确"
    exit 1
fi

# 备份原始配置文件
cp "$CONFIG_FILE" "${CONFIG_FILE}.bak" 2>/dev/null || true

# 使用sed替换url配置
sed -i "s|url: 'http://[^']*'|url: 'http://${HOST_IP}:${BACKEND_PORT}'|" "$CONFIG_FILE"

echo "配置文件已更新: http://${HOST_IP}:${BACKEND_PORT}"

# 显示更新后的配置文件内容
echo "更新后的配置文件内容："
cat "$CONFIG_FILE"