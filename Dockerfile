# Usar una imagen base de Python
FROM python:3.9-slim

# Establecer el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo de requisitos y el código fuente al contenedor
COPY requirements.txt requirements.txt
COPY iot_device.py .

# Instalar las dependencias necesarias
RUN pip install --no-cache-dir -r requirements.txt

# Ejecutar el script de IoT
CMD ["python", "iot_device.py"]

