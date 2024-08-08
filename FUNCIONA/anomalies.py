import threading
import paho.mqtt.client as mqtt
import sys
import time
import subprocess
import signal

def def_handler(sig, frame):
    print("\n\n[!] Saliendo...\n")
    sys.exit(1)

#Ctrl+c
signal.signal(signal.SIGINT, def_handler)

def run_command(command):
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.stdout

# Comando que quieres ejecutari
command = 'cd /home/diego/Desktop/TFG/FUNCIONA/mqttx; mvn exec:java -Dexec.mainClass="io.broker.gestor_anomalias" | grep -i -A 6 "anom"'

# Ejecutar el comando y capturar la salida
output = run_command(command)

# Convertir la salida a string y mostrarla
output_string = output.strip()
#print(f"Output as string:\n{output_string}")


# Variable global para indicar si se ha recibido un mensaje
message_received = False

# Callback para cuando se recibe un mensaje
def on_message(client, userdata, msg):
    global message_received
    print(f"Anomalies - Message received on topic {msg.topic}: {msg.payload.decode()}")
    message_received = True

# Función para suscribirse y escuchar mensajes
def listen_to_confidence(client):
    client.subscribe("iot/confianza/to_anomalia")
    client.loop_start()  # Start the loop to process received messages

# Función para publicar el mensaje recibido como argumento
def publish_to_confidence(client, message):
    time.sleep(5)  # Simular tiempo antes de enviar el mensaje
    client.publish("iot/anomalia/to_confianza", message)

if __name__ == "__main__":
    #if len(sys.argv) != 2:
        #print("Usage: python anomalies.py '<message>'")
        #sys.exit(1)
    
    message = output_string

    # Configurar cliente MQTT
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_message = on_message
    client.connect("localhost", 1883, 60)

    # Crear y lanzar hilos
    listener_thread = threading.Thread(target=listen_to_confidence, args=(client,))
    publisher_thread = threading.Thread(target=publish_to_confidence, args=(client, message))

    listener_thread.start()
    publisher_thread.start()

    # Mantener el programa en ejecución hasta que se reciba un mensaje
    while not message_received:
        time.sleep(1)

    listener_thread.join()
    publisher_thread.join()
    client.loop_stop()
    client.disconnect()

