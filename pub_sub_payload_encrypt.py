import time
import paho.mqtt.client as paho
from cryptography.fernet import Fernet
broker="localhost"
#broker="192.168.1.206"
#define callback
def on_log(client, userdata, level, buf):
    print("log: ",buf)
def on_message(client, userdata, message):
   #time.sleep(1)
   print("receive payload ",message.payload)
   if message.payload==encrypted_message:
      print("\npublished and received messages are the same")
   decrypted_message = cipher.decrypt(message.payload)   #decrypted_message = cipher.decrypt(encrypted_message)
   print("\nreceived message =",str(decrypted_message.decode("utf-8")))


client= paho.Client(paho.CallbackAPIVersion.VERSION2, "client-001")  #create client object client1.on_publish = on_publish                          #assign function to callback client1.connect(broker,port)                                 #establish connection client1.publish("house/bulb1","on")  
client.on_log=on_log
######
client.on_message=on_message
#####encryption
cipher_key = Fernet.generate_key()
cipher = Fernet(cipher_key)
message = b'on'
#message = b'the quick brown fox jumps over the lazy dog'
encrypted_message = cipher.encrypt(message)
out_message=encrypted_message.decode()# turn it into a string to send
##
print("connecting to broker ",broker)
client.connect(broker)#connect
client.loop_start() #start loop to process received messages
print("subscribing ")
client.subscribe("house/bulb1")#subscribe
time.sleep(2)
print("publishing encrypted message ",encrypted_message)
#out_message="on"
client.publish("house/bulb1",out_message)#publish
time.sleep(4)
client.disconnect() #disconnect
client.loop_stop() #stop loop

