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
        print(f"Confidence - Message received from Anomalies: \n\n{message}\n\n")
        anomaly_message_received.set()

        json_data = message
        encoded_json = subprocess.run(f'echo -n \'{json_data}\' | base64 | xargs | tr -d " "', shell=True, capture_output=True, text=True).stdout.strip()
    
        command = f'cd /home/diego/Desktop/diego/TFG/TFG/FUNCIONA/mqttx; mvn exec:java -Dexec.mainClass="io.broker.gestor_confianza" -Dexec.args={encoded_json}'
        output = run_command(command)
        
        # Filtrar el output aquí
        filtered_output = filter_output(output)
        print(f"Output del comando (filtrado):\n{filtered_output}")

        # Obtener la última línea del output filtrado
        last_line = filtered_output.strip().split('\n')[-36]
        
        # Enviar la última línea a Anomalías
        ack_message = cipher.encrypt(last_line.encode())
        client.publish("iot/confianza/to_anomalia", ack_message)
        
        # Enviar el mensaje procesado a la otra cola
        encrypted_device_message = cipher.encrypt(filtered_output.encode())
        client.publish("iot/confianza/to_devices", encrypted_device_message)
    elif topic.startswith("iot/device_") and topic.endswith("/to_confianza"):
        print(f"Confidence - Message received from Device: {message}")
        device_messages_received.set()

def listen_to_anomalies(client):
    client.subscribe("iot/anomalia/to_confianza")
    client.loop_start()

def listen_to_devices(client):
    client.subscribe("iot/device_1/to_confianza")
    client.subscribe("iot/device_2/to_confianza")
    client.loop_start()

def run_command(command):
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.stdout

def filter_output(output):
    lines = output.split('\n')
    filtered_lines = []
    warning_found = False

    for line in lines:
        if '[WARNING]' in line:
            warning_found = True
        if not warning_found:
            filtered_lines.append(line)

    return '\n'.join(filtered_lines)

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
    
    listener_devices_thread = threading.Thread(target=listen_to_devices, args=(client,))
    listener_devices_thread.start()
    
    anomaly_message_received.wait()

    device_messages_received.wait()

    listener_anomalies_thread.join()
    listener_devices_thread.join()
    client.loop_stop()
    client.disconnect()

