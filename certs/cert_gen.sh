# Generar CA
openssl genrsa -des3 -out ca.key 2048

#The common name es muy importante --> debería ser el el domain name al que querremos conectarnos 
openssl req -x509 -new -key ca.key -days 1024 -out ca.crt

# Generar clave privada del cliente
openssl genrsa -out server.key 2048

# Generar CSR (Certificate Signing Request) para el cliente
openssl req -new -key server.key -out server.csr

# Firmar el CSR con la CA para obtener el certificado del cliente
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 360

