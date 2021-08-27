#!/bin/bash
server_address=symp-sae
echo "Using $server_address as default server address"
echo "What's the organization unit of your server?"
read server_ou

echo "What's the organization name of your server?"
read server_o

echo "What's the country of your server?"
read server_c

echo "How long should the certificate be valid? (in days)"
read cert_validity

echo "Enter a password for the keystore: (min 6 characters)"
read -s key_store_pass

echo "(Optional) Enter additional dns names (or IPs) for which the certificate should be valid. (Example: IP:127.0.0.1,DNS:my.custom.dns,DNS:my2.custom.dns,IP:123.123.123.123)"
read custom_addresses

keytool -genkeypair -keyalg RSA -keysize 2048 -alias AnalysisEngine -dname "CN=$server_address,OU=$server_ou,O=$server_o,C=$server_c" -ext "SAN=DNS:$server_address,$custom_addresses" -validity $cert_validity -keystore keystore.jks -storepass $key_store_pass -keypass $key_store_pass -deststoretype pkcs12

keytool -importkeystore -srckeystore keystore.jks -srcstorepass $key_store_pass -destkeystore engine.p12 -deststorepass $key_store_pass -deststoretype pkcs12

openssl pkcs12 -in engine.p12 -nodes -nocerts -out private.pem -password pass:$key_store_pass
openssl pkcs12 -in engine.p12 -out cert.crt -nokeys -password pass:$key_store_pass

