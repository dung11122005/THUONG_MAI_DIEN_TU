# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy pom.xml và source code
COPY pom.xml .
COPY src ./src

# ✅ Copy thêm thư mục uploads nếu có ảnh tĩnh cần hiển thị
COPY uploads ./uploads

# Build ứng dụng
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy file jar từ stage build
COPY --from=build /app/target/*.jar app.jar

# ✅ Copy lại thư mục uploads sang stage chạy
COPY --from=build /app/uploads ./uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
