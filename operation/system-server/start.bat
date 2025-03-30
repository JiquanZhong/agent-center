rem 控制台utf-8编码
chcp 65001
title system服务
echo 启动服务...
java -Xms500m -Xmx500m -Dfile.encoding=UTF-8 -jar diit-system-web.jar
