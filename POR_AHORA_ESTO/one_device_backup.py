import threading
import paho.mqtt.client as mqtt
import sys
import time
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
    print(f"Device - Message received on topic {msg.topic}:\n{message}")
    
    if topic == "iot/confianza/to_devices":
        ack_message = cipher.encrypt("Acknowledged by device".encode())
        client.publish("iot/device_1/to_confianza", ack_message)
        message_from_confidence_received.set()

def listen_to_confidence(client):
    client.subscribe("iot/confianza/to_devices")
    client.loop_start()

if __name__ == "__main__":
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

