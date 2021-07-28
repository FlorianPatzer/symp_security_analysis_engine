#!/bin/bash
echo Location of client\s cert.crt file:
read certFile
sudo keytool -delete -noprompt -alias SympClient -cacerts -storepass changeit
sudo keytool -import -cacerts -storepass changeit -noprompt -alias SympClient -file $certFile
