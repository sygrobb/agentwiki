#!/usr/bin/env bash
set -e

# Переконуємось у використанні UTF-8
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

# Перевірка наявності артефакту
JAR_PATH="./app.jar"
if [ ! -f "$JAR_PATH" ]; then
  # Пошук у локальній директорії target, якщо запуск іде поза Docker
  JAR_PATH="./target/wiki-analytics-1.0-SNAPSHOT-jar-with-dependencies.jar"
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "Error: Fat JAR not found. Please build the project first using 'mvn clean package'." >&2
  exit 1
fi

# Створення директорій для кешу та звітів
mkdir -p .cache/wiki reports

# Передаємо всі аргументи CLI прямо в Java-додаток
exec java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -jar "$JAR_PATH" "$@"