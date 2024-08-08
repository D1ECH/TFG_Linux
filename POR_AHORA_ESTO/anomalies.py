import threading
import paho.mqtt.client as mqtt
import sys
import time
import subprocess
import signal
from cryptography.fernet import Fernet

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

# Ctrl+c
signal.signal(signal.SIGINT, def_handler)

def run_command(command):
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.stdout

# Comando que quieres ejecutari
command = 'cd /home/diego/Desktop/diego/TFG/TFG/FUNCIONA/mqttx; mvn exec:java -Dexec.mainClass="io.broker.gestor_anomalias" | grep -i -A 6 "thingID"'
output = run_command(command)
output_string = output.strip()

message_received = threading.Event()

# Callback para logs
def on_log(client, userdata, level, buf):
    print("log: ", buf)

# Callback para cuando se recibe un mensaje
def on_message(client, userdata, msg):
    decrypted_message = cipher.decrypt(msg.payload)
    print(f"Anomalies - Message received on topic {msg.topic}: {decrypted_message.decode()}")
    message_received.set()

# Función para suscribirse y escuchar mensajes
def listen_to_confidence(client):
    client.subscribe("iot/confianza/to_anomalia")
    client.loop_start()

# Función para publicar el mensaje recibido como argumento
def publish_to_confidence(client, message):
    time.sleep(5)
    encrypted_message = cipher.encrypt(message.encode())
    client.publish("iot/anomalia/to_confianza", encrypted_message)

if __name__ == "__main__":
    message = output_string

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_log = on_log
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    #cipher_key = Fernet.generate_key()
    cipher_key=b'WDrevvK8ZrPn8gmiNFjcOp2xovBr40TCwJlZOyI94IY='
    cipher = Fernet(cipher_key)

    listener_thread = threading.Thread(target=listen_to_confidence, args=(client,))
    publisher_thread = threading.Thread(target=publish_to_confidence, args=(client, message))

    listener_thread.start()
    publisher_thread.start()

    message_received.wait()
    listener_thread.join()
    publisher_thread.join()
    client.loop_stop()
    client.disconnect()

