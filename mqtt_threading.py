import threading
import time
import paho.mqtt.client as mqtt
from queue import Queue

# Función para manejar la conexión
def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))
    # Suscribirse a un topic en la conexión
    client.subscribe("example/topic")

# Función para manejar mensajes recibidos
def on_message(client, userdata, msg):
    print(msg.topic + " " + str(msg.payload))

# Función para suscribirse a un topic
def subscribe_to_topic(topic):
    client = mqtt.Client(protocol=mqtt.MQTTv311)  # Especificar el protocolo si es necesario
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect("mqtt.eclipseprojects.io", 1883, 60)
    client.subscribe(topic)
    client.loop_forever()

# Función para manejar la publicación de mensajes desde la cola
def publish_messages_from_queue(topic, queue):
    client = mqtt.Client(protocol=mqtt.MQTTv311)  # Especificar el protocolo si es necesario
    client.connect("mqtt.eclipseprojects.io", 1883, 60)
    while True:
        message = queue.get()
        if message is None:
            break
        client.publish(topic, message)
        queue.task_done()
        time.sleep(1)  # Ajustar el intervalo de tiempo según sea necesario

# Crear colas de mensajes para cada topic
queues = { "topic/1": Queue(), "topic/2": Queue(), "topic/3": Queue() }

# Crear y lanzar hilos para suscribirse a diferentes topics
subscribe_threads = []
topics = ["topic/1", "topic/2", "topic/3"]

for topic in topics:
    t = threading.Thread(target=subscribe_to_topic, args=(topic,))
    t.start()
    subscribe_threads.append(t)

# Crear y lanzar hilos para manejar la publicación de mensajes desde la cola
publish_threads = []

for topic in topics:
    t = threading.Thread(target=publish_messages_from_queue, args=(topic, queues[topic]))
    t.start()
    publish_threads.append(t)

# Añadir mensajes a las colas en orden
messages = [("topic/1", "Message 1"), ("topic/2", "Message 2"), ("topic/3", "Message 3"), 
            ("topic/1", "Message 4"), ("topic/2", "Message 5"), ("topic/3", "Message 6")]

for topic, message in messages:
    queues[topic].put(message)

# Esperar a que todas las colas se vacíen
for queue in queues.values():
    queue.join()

# Señalar a los hilos de publicación que terminen
for queue in queues.values():
    queue.put(None)

# Esperar a que los hilos terminen
for t in subscribe_threads:
    t.join()

for t in publish_threads:
    t.join()

