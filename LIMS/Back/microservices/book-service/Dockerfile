# 基础镜像
FROM openjdk:17-jdk-alpine

# 维护者信息
MAINTAINER book-service

# 设置工作目录
WORKDIR /app

# 复制jar包到工作目录
COPY target/book-service-1.0.0.jar app.jar

# 暴露端口
EXPOSE 9003

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]