# ==========================================
# Stage 1: Build Java Application
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Кешування залежностей Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Збірка Fat JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /skill

# Встановлення ttf-dejavu для підтримки кирилиці в графіках та PDF
RUN apk add --no-cache ttf-dejavu bash

# Налаштування системних змінних середовища для UTF-8 та Headless графіки
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Djava.awt.headless=true"

# Копіювання зібраного Fat JAR та CLI-скрипта
COPY --from=builder /build/target/wiki-analytics-1.0.0.jar ./app.jar
COPY run.sh ./run.sh

# Створення папок для кешу та звітів + права на виконання
RUN mkdir -p .cache/wiki reports && chmod +x run.sh

ENTRYPOINT ["./run.sh"]