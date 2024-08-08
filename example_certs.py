import time
import paho.mqtt.client as mqtt


def message_callback( message_client, userdata, message ):
  print( message.payload.decode( 'utf8' ) )


if __name__ == "__main__":
  mqtts_client = mqtt.Client( client_id = "MQTTS Client ID" )
  mqtts_client.tls_set( ca_certs = "/home/diego/Desktop/TFG/certs/ca.crt", 
                        certfile = "/home/diego/Desktop/TFG/certs/client.crt", 
                        keyfile = "/home/diego/Desktop/TFG/certs/client.key" )
  mqtts_client.on_message = message_callback
  mqtts_client.loop_start()

  mqtts_client.connect( "broker.emqx.io", port = 8883 )
  time.sleep( 2 )

  mqtts_client.subscribe( "test/MqttsTopic" )
  mqtts_client.publish( "test/MqttsTopic", "Simple MQTTS message" )

  count = 0
  while count < 5:
    time.sleep( 1 )
    count += 1
  mqtts_client.unsubscribe( "test/MqttsTopic" )
  mqtts_client.disconnect()
  mqtts_client.loop_stop()
