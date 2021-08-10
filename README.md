# SyMP Security Analysis Engine

## 1. Dependencies
The Analysis Engine depends on the following services:

- CyMP Camunda Platform
- SyMP System Model Engine
- SyMP Analysis Hub
- Fuseki DB
- MYSQL DB

## 2. Setup
The Analysis Engine needs to be configured to use the services listed in section 1. Thus the details depend on your target environment. At Fraunhofer IOSB, we host it together with all the other components within a Kubernetes cluster. If you would like to setup the project in your environment and require respective support, please contact us.


## 3. CA Certificates and their validation

The CA certificates are needed for TLS connection to the engine. To run the service localy, self signed certificates must be generated. For this reason scripts for certificate generation are present for each profile under **/src/main/resources/ssl_env**.

One of the main problems when using self signed certificates is their validation. The web browsers automatically reject any webservice using self signed certificates and prints a warning message to the user, making him able to selectively allow ("trust") the website. A proper way to solve this issue is to use a certificated given by a trusted authority (check [Certificate Authority](https://en.wikipedia.org/wiki/Certificate_authority)).

In our case we will implement the certificate validation by ourselves, making the SAE administration the trusted authority. To do so, we must first understand how Java checks the certificate validation. A good explanation is give in the following articles: [Java Keystore vs. Truststore](https://www.baeldung.com/java-keystore-truststore-difference) and [The Java Developer’s Guide to SSL Certificates](https://medium.com/@codebyamir/the-java-developers-guide-to-ssl-certificates-b78142b3a0fc). 

Summarized, Java uses a TrustStore that is checked each time for the availability of the certificate from the incoming connection. If the certificate is present, the connection is allowed, if not, the connection is rejected resulting in a CertificateException. A custom TrustStore can be created and used, giving the possibility to add and remove custom certificates that control the allowed connection. There are two scripts for adding and removing certificates to and from the default trust store in the root folder of the project under the names **Add_cert_to_java_truststore.sh** and **Remove_cert_from_java_truststore.sh** using the java keytool. They can be used for better understanding how the process works.

However, we have a slightly different use case. We want many clients to be able to connect to our engine and simultaniously our engine to be able to connect back to the allowed clients. For this case a Reloadable Trust Manager is needed, which allows importing and deleting client certificates during runtime. In this way, the SAE administration can decide which client connection to trust and which to reject. The implementation of the trust manage is available in the class file **ReloadableX509TrustManager.java**.

**Knwon issue:**
The certificate validity check is not only done by verifying the certificate existance in a trust store. A valid certificate must include the exact address of the service it is created for, therefore if a service runs in different environments and has different addresses, a different certificate must be present for each of them. To solve this issue each profile for the SAE has it's own certificate and a certificate generation script which will guide you through the needed steps to generate a proper certificate. They can be found under **/src/main/resources/** in the **ssl__env** folders (where env is the name of one of the environments). Use the dev environment as an example if you want to create a new one.

## 5. Usage

The Security Analysis Engine implements a REST API that follows the OpenAPI specification. The documentation of the SAE's API is available in [docs/reference/Analysis-Engine.v1.yaml](docs/reference/Analysis-Engine.v1.yaml)

The SyMP Client uses that API and exposes its functionality as a user friendly GUI.

## Contributors
Florian Patzer <florian.patzer@iosb.fraunhofer.de>

Manuel Kloppenburg

Nikolay Penkov <n.penkow@gmail.com>
