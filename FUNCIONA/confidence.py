import threading
import paho.mqtt.client as mqtt
import subprocess
import sys
import time
import signal

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

# Ctrl+c
signal.signal(signal.SIGINT, def_handler)

# Variables globales para indicar si se ha recibido un mensaje
anomaly_message_received = threading.Event()
device_messages_received = threading.Event()

# Callback para cuando se recibe un mensaje
def on_message(client, userdata, msg):
    topic = msg.topic
    message = msg.payload.decode()
    if topic == "iot/anomalia/to_confianza":
        print(f"Confidence - Message received from Anomalies: \n{message}")
        anomaly_message_received.set()
        # Enviar ACK a Anomalies
        client.publish("iot/confianza/to_anomalia", "ACK from Confidence")
        
        # Ejecutar el comando y obtener la salida
        result = subprocess.run(['echo', 'hola'], stdout=subprocess.PIPE)
        device_message = result.stdout.decode()
        # Publicar el mensaje a todos los dispositivos
        client.publish("iot/confianza/to_devices", device_message)
    elif topic.startswith("iot/device_") and topic.endswith("/to_confianza"):
        print(f"Confidence - Message received from Device: {message}")
        device_messages_received.set()

# Función para suscribirse y escuchar mensajes de anomalías
def listen_to_anomalies(client):
    client.subscribe("iot/anomalia/to_confianza")
    client.loop_start()  # Start the loop to process received messages

# Función para suscribirse y escuchar mensajes de dispositivos
def listen_to_devices(client):
    client.subscribe("iot/device_1/to_confianza")
    client.loop_start()  # Start the loop to process received messages

if __name__ == "__main__":
    if len(sys.argv) != 1:
        print("Usage: python confidence.py")
        sys.exit(1)

    # Configurar cliente MQTT
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    # Crear y lanzar hilo para escuchar anomalías
    listener_anomalies_thread = threading.Thread(target=listen_to_anomalies, args=(client,))
    listener_anomalies_thread.start()

    # Esperar a recibir el mensaje de anomalías
    anomaly_message_received.wait()  # Esperar hasta que se reciba un mensaje de anomalías

    # Crear y lanzar hilo para escuchar dispositivos
    listener_devices_thread = threading.Thread(target=listen_to_devices, args=(client,))
    listener_devices_thread.start()

    # Mantener el programa en ejecución hasta que se reciba un mensaje del dispositivo
    device_messages_received.wait()  # Esperar hasta que se reciba un mensaje del dispositivo

    listener_anomalies_thread.join()
    listener_devices_thread.join()
    client.loop_stop()
    client.disconnect()

