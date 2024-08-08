import subprocess
import sys
import threading
import paho.mqtt.client as mqtt
import signal
from cryptography.fernet import Fernet

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

# Ctrl+c
signal.signal(signal.SIGINT, def_handler)

anomaly_message_received = threading.Event()
device_messages_received = threading.Event()

def on_log(client, userdata, level, buf):
    print("log: ", buf)

def on_message(client, userdata, msg):
    decrypted_message = cipher.decrypt(msg.payload)
    topic = msg.topic
    message = decrypted_message.decode()
    if topic == "iot/anomalia/to_confianza":
        print(f"Confidence - Message received from Anomalies: \n{message}")
        anomaly_message_received.set()

        # Ejecutar el comando en el sistema
        command = f'cd /home/diego/Desktop/TFG/FUNCIONA/mqttx; mvn exec:java -Dexec.mainClass="io.broker.gestor_confianza"'
        output = run_command(command)
        
        # Procesar el output aquí
        print(f"Output del comando:\n{output}")

        ack_message = cipher.encrypt("ACK from Confidence".encode())
        client.publish("iot/confianza/to_anomalia", ack_message)
        
        # Enviar el mensaje procesado a la otra cola
        encrypted_device_message = cipher.encrypt(output.encode())
        client.publish("iot/confianza/to_devices", encrypted_device_message)
    elif topic.startswith("iot/device_") and topic.endswith("/to_confianza"):
        print(f"Confidence - Message received from Device: {message}")
        device_messages_received.set()

def listen_to_anomalies(client):
    client.subscribe("iot/anomalia/to_confianza")
    client.loop_start()

def listen_to_devices(client):
    client.subscribe("iot/device_1/to_confianza")
    client.loop_start()

def run_command(command):
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.stdout

if __name__ == "__main__":
    if len(sys.argv) != 1:
        print("Usage: python confidence.py")
        sys.exit(1)

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_log = on_log
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    # Replace with your actual encryption key
    cipher_key = b'WDrevvK8ZrPn8gmiNFjcOp2xovBr40TCwJlZOyI94IY='
    cipher = Fernet(cipher_key)

    listener_anomalies_thread = threading.Thread(target=listen_to_anomalies, args=(client,))
    listener_anomalies_thread.start()

    anomaly_message_received.wait()

    listener_devices_thread = threading.Thread(target=listen_to_devices, args=(client,))
    listener_devices_thread.start()

    device_messages_received.wait()

    listener_anomalies_thread.join()
    listener_devices_thread.join()
    client.loop_stop()
    client.disconnect()

