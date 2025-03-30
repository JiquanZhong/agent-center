FROM nginx:stable-alpine

# 复制前端文件
COPY ./diit-bpm-vue /usr/share/nginx/html/diit-bpm-vue

# 复制NGINX配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"] 