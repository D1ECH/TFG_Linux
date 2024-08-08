import paho.mqtt.client as mqtt
import threading
import subprocess
import sys
import signal
from cryptography.fernet import Fernet

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

# Ctrl+c
signal.signal(signal.SIGINT, def_handler)

message_from_confidence_received = threading.Event()

def on_log(client, userdata, level, buf):
    print("log: ", buf)

def on_message(client, userdata, msg):
    decrypted_message = cipher.decrypt(msg.payload)
    topic = msg.topic
    message = decrypted_message.decode()
    #print(f"Device - Message received on topic {msg.topic}:\n{message}")
    filtered_message = filter_message_by_estado(message)
    print("\n" + filtered_message)

    if topic == "iot/confianza/to_devices":
        ack_message = cipher.encrypt("Acknowledged by device".encode())
        client.publish("iot/device_1/to_confianza", ack_message)
        message_from_confidence_received.set()

def listen_to_confidence(client):
    client.subscribe("iot/confianza/to_devices")
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
        print("Usage: python one_device.py")
        sys.exit(1)

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_log = on_log
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    #cipher_key = Fernet.generate_key()
    cipher_key=b'WDrevvK8ZrPn8gmiNFjcOp2xovBr40TCwJlZOyI94IY='
    cipher = Fernet(cipher_key)

    listener_thread = threading.Thread(target=listen_to_confidence, args=(client,))
    listener_thread.start()

    message_from_confidence_received.wait()
    client.loop_stop()
    client.disconnect()
    
    listener_thread.join()


