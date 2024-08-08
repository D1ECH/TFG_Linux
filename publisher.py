import paho.mqtt.publish as publish

# Send a message
publish.single("a/b", "payload", hostname="broker.emqx.io")
# Or send multiple messages at once
msgs = [{'topic':"a/b", 'payload':"multiple 1"}, ("a/b", "multiple 2", 0, False)]
publish.multiple(msgs, hostname="broker.emqx.io")

