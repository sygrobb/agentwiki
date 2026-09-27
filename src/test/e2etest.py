import os
import subprocess
import json
from google import genai
from google.genai import types

# 1. Ініціалізація клієнта
client = genai.Client(api_key=os.environ.get("GEMINI_API_KEY"))

# 2. Опис нашої навички як Function Calling інструменту для Gemini
wiki_tool = types.FunctionDeclaration(
    name="wikipedia_analytics",
    description="Аналіз трендів переглядів Вікіпедії з генерацією PDF-звіту",
    parameters={
        "type": "OBJECT",
        "properties": {
            "articles": {"type": "STRING", "description": "Статті у форматі lang:Title"},
            "period_months": {"type": "INTEGER", "description": "Період в місяцях"},
            "output_pdf": {"type": "STRING", "description": "Шлях для збереження PDF"}
        },
        "required": ["articles"]
    }
)

tools = types.Tool(function_declarations=[wiki_tool])

# 3. Відправка запиту до Gemini
print("🚀 Відправляємо запит до Gemini 2.5 Flash...")
response = client.models.generate_content(
    model="gemini-2.5-flash",
    contents="Проаналізуй тренд статті uk:Астрономія за останні 12 місяців і збережи PDF у report.pdf",
    config=types.GenerateContentConfig(tools=[tools])
)

# 4. Перевіряємо, чи вирішила модель викликати наш CLI
if response.candidates[0].content.parts[0].function_call:
    call = response.candidates[0].content.parts[0].function_call
    print(f"✅ Gemini вирішив викликати інструмент: {call.name}")
    print(f"📦 Аргументи: {call.args}")

    # Запускаємо наш Java CLI через run.sh / docker
    cmd = [
        "./run.sh",
        "--articles", str(call.args.get("articles")),
        "--period-months", str(int(call.args.get("period_months", 24))),
        "--json"
    ]
    if "output_pdf" in call.args:
        cmd.extend(["--output-pdf", str(call.args["output_pdf"])])

    print("⚙️ Виконуємо Java CLI...")
    result = subprocess.run(cmd, capture_output=True, text=True)
    print("📊 Результат виконання (JSON):")
    print(result.stdout)
else:
    print("❌ Модель не викликала function call:", response.text)