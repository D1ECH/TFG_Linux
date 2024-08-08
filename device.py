import threading
import paho.mqtt.client as mqtt
import sys
import time
import signal

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

#Ctrl+c
signal.signal(signal.SIGINT, def_handler)

# Variable global para indicar si se ha recibido un mensaje de confianza
message_from_confidence_received = threading.Event()

# Callback para cuando se recibe un mensaje
def on_message(client, userdata, msg):
    global message_from_confidence_received
    print(f"Device {userdata} - Message received on topic {msg.topic}: {msg.payload.decode()}")
    
    message_from_confidence_received.set()
    # Enviar respuesta a confianza
    client.publish("iot/device_{userdata}/to_confianza", "Acknowledged by device {userdata}")

# Función para suscribirse y escuchar mensajes
def listen_to_confidence(client, device_id):
    client.subscribe("iot/confianza/to_devices")
    client.loop_start()  # Start the loop to process received messages

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python device.py <num_devices>")
        sys.exit(1)
    
    num_devices = int(sys.argv[1])

    # Configurar clientes MQTT para múltiples dispositivos
    clients = []
    for device_id in range(num_devices):
        client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, userdata=device_id)
        client.on_message = on_message
        client.connect("localhost", 1883, 60)
        clients.append(client)

    # Crear y lanzar hilos para cada dispositivo
    threads = []
    for device_id, client in enumerate(clients):
        thread = threading.Thread(target=listen_to_confidence, args=(client, device_id))
        threads.append(thread)
        thread.start()

    # Mantener el programa en ejecución hasta que se reciba un mensaje de confianza
    for client in clients:
        message_from_confidence_received.wait()  # Esperar hasta que se reciba un mensaje
        client.loop_stop()
        client.disconnect()
    
    # Esperar a que todos los hilos terminen
    for thread in threads:
        thread.join()

