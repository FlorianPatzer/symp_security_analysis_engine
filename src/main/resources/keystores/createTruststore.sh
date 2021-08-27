echo "How long should the certificate be valid? (in days)"
read cert_validity

keytool -genkey -v -keystore custom.jks -keysize 2048 -validity $cert_validity