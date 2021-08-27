## About The Project
The Security Analysis Engine (SAE) is one of the main parts of the SyMP framework and it's used to create analyses for and analyse system configurations for security missconfiguration.

### Built With

The SAE is built as a Docker orchestration with the following frameworks:

* [Spring](https://spring.io/)
* [Vaadin](https://vaadin.com/)
* [Fuseki DB](https://jena.apache.org/documentation/fuseki2/)

<!-- GETTING STARTED -->
## Getting Started

This project is dependent on the following projects:

* [SyMP Base](https://gitlab-ext.iosb.fraunhofer.de/symp/symp-docker) 
* [SyMP SME](https://gitlab-ext.iosb.fraunhofer.de/symp/static-processing-controller)
* [SyMP AH](https://gitlab-ext.iosb.fraunhofer.de/symp/analysis-hub)

They must be running locally or externally in order for the SAE function fully. Refer to their GitLab repositories to see how to start them.

### Prerequisites

This project uses Docker to automatically build and setup the created service. Please make sure that you have Docker isntalled or follow the download instructions [here](https://docs.docker.com/docker-for-windows/install/).

Also make sure to make sure that you are using the proper profile for your build:
1. The SAE has a profile for each of the environment it is going to be running in. The are located under `/src/main/resources`
2. The file `application.properties` is the base for the profiles and each of the inside defined variables are used independent of the selected profile
3. Every other profile configuration file has the name `application-__env__.properties` (where `env` is the name of the environment)
4. The available profiles are already configured and it's recommended that they are used as an example for the creation of new profiles.
5. To select a profile use the `--spring.profiles.active=*your_profile_here*` flag when starting Spring (where `*your_profile_here*` is the name of your environment - for example dev)

### Installation

**Option 1: Using Docker**

*Attention:* The dockerized build is already preconfigured and uses the `prod` profile. 

   - One-Liner: Use `docker-compose up --build` to build the containers and start them directly
   - Use `docker-compose build` to build the containers.
   - Use `docker-compose up` to start the containers.


**Option 2: Running in DEV mode locally**

*Attention:* Anlyses running in the SAE are dependent on workers running Docker, so starting an analysis when SAE is running in dev mode outside Docker could in some cases lead to code failure.

To start the Engine outside Docker do the following:

- Run Fuseki DB
    1. Download `apache-jena-fuseki-3.17.0` from [Apache Jena Download](https://jena.apache.org/download/#apache-jena-fuseki)
    2. Extract zip or tar.gz
    3. Move `fuseki/config-tdb2.ttl` from repository to extracted directory
    4. Execute `./fuseki-server --tdb2 --config=config-tdb2.ttl`

- Run SAE with maven
  1. `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

## Usage

* SAE implements a REST API that follows the OpenAPI specification. The documentation of the SAE's API is available in [docs/reference/Analysis-Engine.v1.yaml](docs/reference/Analysis-Engine.v1.yaml). 

* The API is accessable only over `HTTPS` on port `8543` at endpoint `/api/v1` for example:
```
httsp://localhost:8543/api/v1
```

* [SyMP Client](https://gitlab-ext.iosb.fraunhofer.de/symp/symp-client) project wraps around the SAE's API and exposes its functionality in a user friendly GUI.

* When a client app is registered, it must be allowed manually to access the SAE.

* The current front-end has only one use and it's exatcly to allow or disallow (respectively reject) subscribed apps to access the SAE's API. It is available over `HTTPS` on port `8543` at endpoint `/apps`:

```
httsp://localhost:8543/apps
```

## Roadmap

Currenlty the SAE needs:
* Integration of the standardized [Spring Security](https://spring.io/projects/spring-security#overview)
* Frontend refining


See the [open issues](https://gitlab-ext.iosb.fraunhofer.de/symp/security-analysis-engine/-/issues) for an additional list of proposed features (and known issues).

## Additional

* CA Certificates and their validation:
    
    The CA certificates are needed for TLS connection to the engine. To run the service localy, self signed certificates must be generated. For this reason scripts for certificate generation are present for each profile under `/src/main/resources/`.

    One of the main problems when using self signed certificates is their validation. The web browsers automatically reject any webservice using self signed certificates and prints a warning message to the user, making him able to selectively allow ("trust") the website. A proper way to solve this issue is to use a certificated given by a trusted authority (check [Certificate Authority](https://en.wikipedia.org/wiki/Certificate_authority)).

    In our case we will implement the certificate validation by ourselves, making the SAE administration the trusted authority. To do so, we must first understand how Java checks the certificate validation. A good explanation is give in the following articles: [Java Keystore vs. Truststore](https://www.baeldung.com/java-keystore-truststore-difference) and [The Java Developer’s Guide to SSL Certificates](https://medium.com/@codebyamir/the-java-developers-guide-to-ssl-certificates-b78142b3a0fc). 

    Summarized, Java uses a TrustStore that is checked each time for the availability of the certificate from the incoming connection. If the certificate is present, the connection is allowed, if not, the connection is rejected resulting in a CertificateException. A custom TrustStore can be created and used, giving the possibility to add and remove custom certificates that control the allowed connection. There are two scripts for adding and removing certificates to and from the default trust store in the root folder of the project under the names `Add_cert_to_java_truststore.sh` and `Remove_cert_from_java_truststore.sh` using the java keytool. They can be used for better understanding how the process works.

    However, we have a slightly different use case. We want many clients to be able to connect to our engine and simultaniously our engine to be able to connect back to the allowed clients. For this case a Reloadable Trust Manager is needed, which allows importing and deleting client certificates during runtime. In this way, the SAE administration can decide which client connection to trust and which to reject. The implementation of the trust manage is available in the class file `ReloadableX509TrustManager.java`.

    **Knwon issue:**
    
    The certificate validity check is not only done by verifying the certificate existance in a trust store. A valid certificate must include the exact address of the service it is created for, therefore if a service runs in different environments and has different addresses, a different certificate must be present for each of them. To solve this issue each profile for the SAE has it's own certificate and a certificate generation script which will guide you through the needed steps to generate a proper certificate. They can be found under `/src/main/resources/` in the `ssl_env` folders (where `env` is the name of one of the environments). Use the dev environment as an example if you want to create a new one.

* Kube Service configuration

  *Note: When SAE is started in K8S Mode a token for accessing the specified Kubernetes cluster. For the current use case the Fraunhofer's [Rancher Server](https://rancher.k8s.ilt-dmz.iosb.fraunhofer.de/) was being used, so the following steps are describing the setup for it specifically. However, it is possible to set up the Service to work with any Kubernetes server (for more information check the official [Kubernetes Docs](https://kubernetes.io/docs/reference/access-authn-authz/))*

  **How to create an access token in Rancher:**
  
  1. Login in [Rancher's Frontend](https://rancher.k8s.ilt-dmz.iosb.fraunhofer.de/)
  2. Click on your profile avatar in the upper right corner
  3. Select `API & Keys` from the dropdown menu and you will be presented with your current API keys
  4. On the new page click on the `Add Key` button right under your avatar
  5. Give a description to your key and an expiration date. 
      
      **Attention:** Leave the `Scope` option to `no scope`
  6. Click on create and you will generate a new API key
  7. The access token you need for `KubeService` is in the `Bearer Token` field
  8. Make sure you add the token to your <u>**system's**</u> ENV variables.
    
      *Note:* The `rancher.token` env variable in the `application.properties` file refers to a system env variable called `TOKEN`. It's a good security practice to not add the access token directly in the property files, because it could get leaked. Make sure you add a `TOKEN` env variable in your OS with the token you've created in order to access the remote K8S cluster.
      