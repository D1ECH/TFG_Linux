#!/bin/bash

#Colours
greenColour="\e[0;32m\033[1m"
endColour="\033[0m\e[0m"
redColour="\e[0;31m\033[1m"
blueColour="\e[0;34m\033[1m"
yellowColour="\e[0;33m\033[1m"
purpleColour="\e[0;35m\033[1m"
turquoiseColour="\e[0;36m\033[1m"
grayColour="\e[0;37m\033[1m"

function ctrl_c(){
  echo -e "\n\n${redColour}[!] Saliendo...${endColour}\n"
  tput cnorm
  exit 1
}

# Ctrl + c
trap ctrl_c INT


# Variables
BROKER="localhost"
PORT="1883"
TOPIC1="topic1"
TOPIC2="topic2"
CLIENT1_ID="client1"
CLIENT2_ID="client2"

# Función para el cliente 1
client1() {
    while true; do
        # Publicar un mensaje en TOPIC1
        echo "Client1: Publicando en $TOPIC1"
        mosquitto_pub -h $BROKER -p $PORT -t $TOPIC1 -m "Mensaje desde $CLIENT1_ID" -i $CLIENT1_ID

        # Escuchar mensajes en TOPIC2
        echo "Client1: Escuchando en $TOPIC2"
        mosquitto_sub -h $BROKER -p $PORT -t $TOPIC2 -i $CLIENT1_ID -C 1
    done
}

# Función para el cliente 2
client2() {
    while true; do
        # Publicar un mensaje en TOPIC2
        echo "Client2: Publicando en $TOPIC2"
        mosquitto_pub -h $BROKER -p $PORT -t $TOPIC2 -m "Mensaje desde $CLIENT2_ID" -i $CLIENT2_ID

        # Escuchar mensajes en TOPIC1
        echo "Client2: Escuchando en $TOPIC1"
        mosquitto_sub -h $BROKER -p $PORT -t $TOPIC1 -i $CLIENT2_ID -C 1
    done
}

# Ejecutar clientes en segundo plano
client1 &
client2 &

# Esperar a que terminen los procesos
wait
