import paho.mqtt.client as mqtt
import os

# Configuración MQTT
BROKER = 'localhost'  # Puedes cambiar esto a tu broker MQTT
PORT = 1883
TOPIC_SUB = 'test/topic/sub'
TOPIC_PUB = 'test/topic/pub'
DEVICE_ID = os.getenv('DEVICE_ID', 'default_device')

# Callback cuando la conexión al broker MQTT es exitosa
def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")
    client.subscribe(TOPIC_SUB)

# Callback cuando un mensaje es recibido en el topic suscrito
def on_message(client, userdata, msg):
    print(f"Message received: {msg.payload.decode()}")
    # Enviar un ACK de respuesta
    ack_message = f"ACK from {DEVICE_ID}"
    client.publish(TOPIC_PUB, ack_message)
    print(f"Sent ACK: {ack_message}")

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect(BROKER, PORT, 60)
client.loop_forever()

