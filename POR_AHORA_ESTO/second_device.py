import paho.mqtt.client as mqtt
import threading
import subprocess
import sys
import signal
from cryptography.fernet import Fernet

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    client.loop_stop()
    client.disconnect()
    sys.exit(1)

# Ctrl+c
signal.signal(signal.SIGINT, def_handler)

message_from_confidence_received = threading.Event()
ack_from_device_1_received = threading.Event()

def on_log(client, userdata, level, buf):
    print("log: ", buf)

def on_message(client, userdata, msg):
    decrypted_message = cipher.decrypt(msg.payload)
    topic = msg.topic
    message = decrypted_message.decode()

    if topic == "iot/confianza/to_devices":
        filtered_message = filter_message_by_estado(message)
        print("\n" + filtered_message)
        message_from_confidence_received.set()
    elif topic == "iot/device_1/to_device_2":
        print("Mensaje recibido de device_1:", message)
        ack_message = cipher.encrypt("ACK from device 2".encode())
        client.publish("iot/device_2/to_device_1", ack_message)
        ack_from_device_1_received.set()

def listen_to_confidence(client):
    client.subscribe("iot/confianza/to_devices")
    client.subscribe("iot/device_1/to_device_2")
    client.loop_start()

def filter_message_by_estado(message):
    # Ejecutar grep en un subproceso
    process = subprocess.Popen(['grep', '-A', '3', 'Estado:'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    stdout, stderr = process.communicate(input=message)
    if process.returncode != 0:
        return f"Error filtering message: {stderr}"
    return stdout

if __name__ == "__main__":
    if len(sys.argv) != 1:
        print("Usage: python second_device.py")
        sys.exit(1)

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_log = on_log
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    cipher_key = b'WDrevvK8ZrPn8gmiNFjcOp2xovBr40TCwJlZOyI94IY='
    cipher = Fernet(cipher_key)

    listener_thread = threading.Thread(target=listen_to_confidence, args=(client,))
    listener_thread.start()

    message_from_confidence_received.wait()
    
    # Enviar el mensaje recibido a device_1
    message = "Mensaje de prueba desde device_2"
    encrypted_message = cipher.encrypt(message.encode())
    client.publish("iot/device_2/to_device_1", encrypted_message)
    
    ack_from_device_1_received.wait()
    
    # Enviar ACK a confidence después de intercambiar mensajes con device_1
    ack_message = cipher.encrypt("Acknowledged by device 2".encode())
    client.publish("iot/device_2/to_confianza", ack_message)

    client.loop_stop()
    client.disconnect()
    
    listener_thread.join()

