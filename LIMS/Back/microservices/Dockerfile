# 基础镜像
FROM openjdk:17-jdk-alpine

# 维护者信息
MAINTAINER bookmanagement

# 设置工作目录
WORKDIR /app

# 复制jar包到工作目录
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]