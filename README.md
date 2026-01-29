# PolyNSI

A bidirectional SOAP to gRPC translating proxy server for the NSI protocol.

# Introduction and Rationale

NSI is a SOAP based protocol. [SOAP's specifications are unfortunately really
vague and leave a lot of things open for
interpretation](https://python-zeep.readthedocs.io/en/master/index.html#quick-introduction)
(see last paragraph of referenced section). Apart from being under specified,
SOAP is rather complex. The result is that not many programming language
ecosystems have full support for SOAP. In fact, full implementations (both
server and client) can only be found for:

* Java
* .NET (not even .NET Core)
* C/C++

SOAP client implementations can be found for many more programming language
ecosystems. NSA implementations however, are both SOAP servers and clients.

Services that implement the NSI protocol, are called Network Service Agents
(NSAs). To allow convenient development of NSAs in other programming languages
ecosystems than the above three, we have developed a proxy server in Java, that
does nothing more than accepting SOAP requests, that it subsequently translates
to requests using a more modern and widely used protocol and vice versa.

An obvious choice for the 'more modern protocol' would be REST/JSON.  Obvious,
because that is all the hype these days and many people have considerable
experience with it. Downside is that endpoint and message specification using
the OpenAPI Specification is enormously verbose and a pain to read.

Another option would be to use gRPC with Protocol Buffers v3. Writing a
specification using Protocol Buffers is much more succinct and easy to read
than a verbose OpenAPI specification YAML file. Should we want to seek adoption
among other National Research and Education Networks (NRENs) for an alternative
NSI interface, readability will be an important factor. 

Another benefit of gRPC is that it is a 
[Cloud Native Computing Foundation (CNCF)](https://www.cncf.io/) 
project just like Kubernetes is. There is some overlap between the 
[Open Grid Forum](https://www.ogf.org/ogf/doku.php), where NSI originated, and
CNCF. Sticking to similar technologies might invite contributions from people 
already familiar in that space (easy transfer of skill sets)

Hence the choice for gRPC to translate NSI's native SOAP requests and responses
to and from. With the ability to develop NSAs more conveniently in a wider
array of programming languages we hope to improve the uptake of the NSI
protocol. And possibly direct gRPC to gRPC NSA interaction without PolyNSI's
intervention.

## A note on gRPC and its relation to Protocol Buffers

gRPC is an extension of Protocol Buffers. Messages are still defined using
Protocol Buffers v3. The service definitions (=remote procedures) though,
extend the Protocol Buffers syntax. This means that the Protocol Buffers
compiler `protoc` cannot compile all the `.proto` files used for gRPC. The
compiler needs a plugin to do so. 

# Quick Start

* [install (Open)JDK 21](https://openjdk.java.net/)
* [install Apache Maven](http://maven.apache.org/install.html)

```shell
mvn clean generate-sources
mvn test
mvn spring-boot:run
```

And point your browser to `http://localhost:8080/soap`.

# Installation

Normally the Quick Start instructions should be all that's required to get
PolyNSI installed and running with a default configuration. Maven will find
everything it needs and installs it into its local repository. Thus making it
available to PolyNSI. This includes binary dependencies such as the Protocol
Buffers compiler with its gRPC plugin. However, when deploying PolyNSI in
production, a more hardended approach is advised.

## Create and run a JAR

To generate the Java code from the proto files, and compile and package everything
info a JAR, use the following command:

```shell
mvn clean package
```

The resulting JAR is stored in the `target` folder, and can be started with:

```shell
java -jar target/polynsi-x.y.z.jar
```

When the default configuration does not suffice, change the properties as can be found in
`src/main/resources/application.properties` and supply it on the command line:

```shell
java -Dspring.config.additional-location=file:path/to/updated/application.properties -jar target/polynsi-x.y.z.jar
```

## Containers

Pre-build containers based on [Google Distroless Java](https://github.com/GoogleContainerTools/distroless/tree/main/java) can be found at the [GitHub container registry for PolyNSI](https://github.com/workfloworchestrator/PolyNSI/pkgs/container/polynsi), and can be used to deploy to Kubernetes, or run directly with Docker:

```shell
docker run \
	--publish 8080:8080 \
	--publish 9090:9090 \
	--volume `pwd`/config/application.properties:/config/application.properties \
	--interactive \
	--tty \
	ghcr.io/workfloworchestrator/polynsi:latest \
	java -Dspring.config.additional-location=/config/application.properties -jar polynsi.jar
```

# Configuration

All PolyNSI configuration is done by properties. As PolyNSI is a Spring Boot
application it follows Springs Boot's conventions for obtaining 
[those properties from files](https://docs.spring.io/spring-boot/reference/features/external-config.html).  

PolyNSI's default configuration resides in
`src/main/resources/application.properties` where the properiets are documented
as well. Things that you almost certainly will have to configure are the
SOAP server side of PolyNSI including the SSL configuration,
and the gRPC side of PolyNSI that faces the gRPC based NSA:

~~~
        cxf.path
        soap.server.*  +---------+  grp.client.*            +-------+
        server.*       |         |------------------------->| gRPC  |
        -------------->| PolyNSI |                          | based |
                       |         |<-------------------------| NSA   |
                       +---------+  grpc.server.*           +-------+
~~~

A typical `application.properties` contains the following configuration options:

```properties
#
# PolyNSI application properties
#
debug=false
logging.config=file:/usr/local/etc/polynsi/logback-spring.xml
#
# SOAP provider endpoint configuration
#
cxf.path=/soap
soap.server.connection_provider.path=/connection/provider
soap.server.connection_requester.path=/connection/requester
#
# SSL proxy client certificate verification support
#
nl.surf.polynsi.verify-ssl-client-subject-dn=false
nl.surf.polynsi.ssl-client-subject-dn-header=ssl-client-subject-dn
nl.surf.polynsi.client.certificate.distinguished-names[0]=CN=CertA,OU=Dept X,O=Company 1,C=NL
#
# SOAP provider SSL configuration
#
server.port=8443
server.ssl.enabled=true
server.ssl.client-auth=need
server.ssl.key-store=/usr/local/polynsi/polynsi-keystore.jks
server.ssl.key-store-type=jks
server.ssl.key-store-password=secret
server.ssl.trust-store=/usr/local/polynsi/polynsi-truststore.jks
server.ssl.trust-store-type=jks
server.ssl.trust-store-password=secret
#
# gRPC server configuration
#
grpc.server.port=9090
#
# gRPC client configuration
#
grpc.client.connection_provider.address=static://localhost:50051
grpc.client.connection_provider.negotiationType=PLAINTEXT
```

See also:
* [Application Property Files](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/reference/html/spring-boot-features.html#boot-features-external-config-application-property-files)
* [Common (Spring Boot) Application Properties](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/reference/html/appendix-application-properties.html#common-application-properties)

## PolyNSI behind an SSL proxy

Sometimes PolyNSI is deployed behind an SSL proxy that terminates the SSL
session, like a Kubernetes Ingress. If it is not possible to implement per
client access control on the proxy, access can still be controlled by PolyNSI
based on the client certificate distinguished name.  In this case the proxy
will ask the client for a certificate and verifies the issuer against a list of
configured certificate authorities. When the client certificate is valid, the
proxy will add a header with the client distinguished name (DN) to the upstream
HTTP request.  PolyNSI can be configured to look at a certain header and
compare the value against a list of allowed client certificate DN.

~~~
                       +---------+     header: client DN     +---------+              +-------+
            HTTPS      |   SSL   |           HTTP            |         |     HTTP     |       |
        -------------->|  proxy  |-------------------------->| PolyNSI |------------->| gRPC  |
                       +---------+                           |         |              | based |
                                            HTTPS            |         |     HTTP     | NSA   |
        <----------------------------------------------------|         |<-------------|       |
                                                             +---------+              +-------+
~~~

This requires a couple of configuration changes. On PolyNSI, disable SSL for
incoming traffic, specify the name of the header that contains the client DN,
and enable the verification of that DN.  Last but not least, a list of allowed
client DN is configured. Note that, to allow PolyNSI to directly connect back to
the requesting NSA, the trust- and keystore are still needed.

The following section in the `application.properties` is used when the SSL
session is directly terminate on PolyNSI.

```properties
#
# SOAP provider SSL configuration
#
server.port=8443
server.ssl.enabled=true
```

In case of an SSL terminating proxy before PolyNSI, the config can be changed
as shown below.

```properties
#
# SSL proxy client certificate verification support
#
nl.surf.polynsi.verify-ssl-client-subject-dn=true
nl.surf.polynsi.ssl-client-subject-dn-header=X-SSL-Client-DN
#
# SOAP provider SSL configuration
#
server.port=8080
server.ssl.enabled=false
```

The list of allowed client certificate DN can be configured in the
`application.properties` using the indexed property
`nl.surf.polynsi.client.certificate.distinguished-names`. Note that whitespace
is preserved automatically, so surounding quotes are not needed, as a matter of
fact, the quotes will become part of the distinguished name and will therefor
never match any client certificate DN past by the proxy.

```properties
nl.surf.polynsi.client.certificate.distinguished-names[0]=CN=CertA,OU=Dept X,O=Company 1,C=NL
nl.surf.polynsi.client.certificate.distinguished-names[1]=CN=CertB,OU=Dept Y,O=Company 2,C=NL
nl.surf.polynsi.client.certificate.distinguished-names[2]=CN=CertC,OU=Dept Z,O=Company 3,C=NL
```

## Support for PEM encoded private key and certificates

Instead of supplying a Java key- and truststore with the `server.ssl.key-store` and `server.ssl.trust-store`
properties, it is also possible to supply the server private key, certificate and chain, and the trusted
CA bundle, in PEM encoded format using the following properties:

```properties
spring.ssl.bundle.pem.polynsi-bundle.keystore.certificate=file:server-certificate.pem
spring.ssl.bundle.pem.polynsi-bundle.keystore.private-key=file:server-private-key.pem
spring.ssl.bundle.pem.polynsi-bundle.keystore.certificate-chain=file:ca-chain.pem
spring.ssl.bundle.pem.polynsi-bundle.keystore.private-key-password=secret
spring.ssl.bundle.pem.polynsi-bundle.truststore.certificate=file:trusted-bundle.pem
server.ssl.bundle=polynsi-bundle
```

# Debug logging

In case things do no seem to work as expected, debug logging can be enabled with the following properties:

```properties
# Spring Boot
debug=true
# PolyNSI application
logging.level.nl.surf.polynsi=DEBUG
# gRPC server
logging.level.org.springframework.grpc=DEBUG
# SOAP server
logging.level.org.apache.cxf=DEBUG
# gRPC messages
logging.level.io.grpc=DEBUG
```