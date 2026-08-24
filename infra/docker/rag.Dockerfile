FROM python:3.11-slim

WORKDIR /app

COPY rag-service/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY rag-service .

EXPOSE 8001

CMD ["python", "main.py"]