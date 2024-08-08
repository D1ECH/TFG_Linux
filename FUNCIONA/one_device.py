import threading
import paho.mqtt.client as mqtt
import sys
import time
import signal

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

# Ctrl+c
signal.signal(signal.SIGINT, def_handler)

# Variable global para indicar si se ha recibido un mensaje de confianza
message_from_confidence_received = threading.Event()

# Callback para cuando se recibe un mensaje
def on_message(client, userdata, msg):
    topic = msg.topic
    message = msg.payload.decode()
    print(f"Device - Message received on topic {msg.topic}: {msg.payload.decode()}")
    
    if topic == "iot/confianza/to_devices":
        # Enviar respuesta a confianza
        client.publish("iot/device_1/to_confianza", "Acknowledged by device")
        message_from_confidence_received.set()

# Función para suscribirse y escuchar mensajes
def listen_to_confidence(client):
    client.subscribe("iot/confianza/to_devices")
    client.loop_start()  # Start the loop to process received messages

if __name__ == "__main__":
    # Configurar cliente MQTT
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    # Suscribirse y escuchar mensajes de confianza
    listener_thread = threading.Thread(target=listen_to_confidence, args=(client,))
    listener_thread.start()

    # Mantener el programa en ejecución hasta que se reciba un mensaje de confianza
    message_from_confidence_received.wait()  # Esperar hasta que se reciba un mensaje
    client.loop_stop()
    client.disconnect()
    
    listener_thread.join()

