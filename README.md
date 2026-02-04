# PolyNSI

A bidirectional SOAP tot gRPC translating proxy server for the NSI protocol.

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
production, a more hardened approach is advised.

## Create and run a JAR

To generate the Java code from the proto files, and compile and package
everything info a JAR, use the following command:

```shell
mvn clean package
```

The resulting JAR is stored in the `target` folder, and can be started with:

```shell
java -jar target/polynsi-x.y.z.jar
```

When the default configuration does not suffice, change the properties as can
be found in `src/main/resources/application.properties` and supply it on the
command line:

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

All PolyNSI configuration is done through properties. As PolyNSI is a Spring Boot
application it follows Springs Boot's conventions for obtaining 
[those properties from files](https://docs.spring.io/spring-boot/reference/features/external-config.html).  

PolyNSI's default configuration resides in
`src/main/resources/application.properties` where the properties are documented
as well. Things that you almost certainly will have to configure are the SOAP
server side of PolyNSI including the SSL configuration, and the gRPC side of
PolyNSI that faces the gRPC based NSA:

~~~
        spring.ssl.bundle.*
        cxf.path
        soap.server.*
        server.*             +---------+  spring.grp.client.*         +-------+
        -------------------->|         |----------------------------->| gRPC  |
                             | PolyNSI |                              | based |
        <--------------------|         |<-----------------------------| NSA   |
        spring.ssl.bundle.*  +---------+  spring.grpc.server.*        +-------+
~~~

> [!IMPORTANT]
> In PolyNSI version 0.4.0, the `grpc.*` properties are renamed to
> `spring.grpc.*`.

## PolyNSI without SSL

The `application.properties` for PolyNSI without SSL, where also the outgoing
SOAP messages do not require SSL, looks as follows:

```properties
#
# SOAP server configuration
#
cxf.path=/soap
soap.server.connection_provider.path=/connection/provider
soap.server.connection_requester.path=/connection/requester
server.port=8080
server.ssl.enabled=false
server.ssl.client-auth=none
nl.surf.polynsi.client.certificate.authorize-dn=no
#
# gRPC server and client configuration
#
spring.grpc.server.port=9090
spring.grpc.client.channels.connection-provider.address=static://localhost:50051
spring.grpc.client.channels.connection-provider.negotiation-type=plaintext
```

## PolyNSI with SSL

When PolyNSI is used in an production environment where SSL is enabled, and
mTLS is used to authorize the clients, two separate SSL bundles are used. The
SOAP server uses a SSL bundle with name `nsi-soap-server`, the keystore part of
the bundle contains the the server certificate and key, and the truststore part
contains the Certificate Authorities to verify that the presented client
certificate is valid. Access is authorized when the client certificate DN is
found in the list of allowed client `distinguished-names`. A second bundle with
name `nsi-soap-client` is used for the SOAP client, the keystore part contains
the certificate and key used by this server to identify itself, through mTLS,
to other Network Server Agents, and the truststore contains CA certificates to
verify the other NSA certificate.  In the example below, the certificate and key of
the SOAP server and client are the same, this is the most common case. 

The list of allowed client certificate DN can be configured in the
`application.properties` using the indexed property
`nl.surf.polynsi.client.certificate.distinguished-names`. Note that whitespace
is preserved automatically, so surrounding quotes are not needed, as a matter
of fact, the quotes will become part of the distinguished name and will
therefor never match any client certificate DN past by the proxy.

The `application.properties` for PolyNSI with SSL looks as follows:

```properties
#
# SOAP server configuration
#
cxf.path=/soap
soap.server.connection_provider.path=/connection/provider
soap.server.connection_requester.path=/connection/requester
server.port=8443
server.ssl.enabled=true
server.ssl.client-auth=need
nl.surf.polynsi.client.certificate.authorize-dn=certificate
nl.surf.polynsi.client.certificate.distinguished-names[0]=CN=CertA,OU=Dept X,O=Company 1,C=NL
nl.surf.polynsi.client.certificate.distinguished-names[1]=CN=CertB,OU=Dept Y,O=Company 2,C=NL
nl.surf.polynsi.client.certificate.distinguished-names[2]=CN=CertC,OU=Dept Z,O=Company 3,C=NL
spring.ssl.bundle.pem.nsi-soap-server.keystore.certificate=file:server-certificate.pem
spring.ssl.bundle.pem.nsi-soap-server.keystore.private-key=file:server-private-key.pem
spring.ssl.bundle.pem.nsi-soap-server.key.password=secret
spring.ssl.bundle.pem.nsi-soap-server.truststore.certificate=file:client-nsa-trusted-bundle.pem
spring.ssl.bundle.pem.nsi-soap-client.keystore.certificate=file:server-certificate.pem
spring.ssl.bundle.pem.nsi-soap-client.keystore.private-key=file:server-private-key.pem
spring.ssl.bundle.pem.nsi-soap-client.key.password=secret
spring.ssl.bundle.pem.nsi-soap-client.truststore.certificate=file:other-nsa-trusted-bundle.pem
#
# gRPC server and client configuration
#
spring.grpc.server.port=9090
spring.grpc.client.channels.connection-provider.address=static://localhost:50051
spring.grpc.client.channels.connection-provider.negotiation-type=plaintext
```

Both the SOAP server and client bundle can be configured with Java key- and truststores as well, with
support for both JKS and PKCS12, as is shown below for the `nsi-soap-server` bundle:

```properties
spring.ssl.bundle.jks.nsi-soap-server.keystore.location=file:keystore.jks
spring.ssl.bundle.jks.nsi-soap-server.keystore.password=secret
spring.ssl.bundle.jks.nsi-soap-server.keystore.type=JKS
spring.ssl.bundle.jks.nsi-soap-server.key.password=secret
spring.ssl.bundle.jks.nsi-soap-server.key.alias=my-server
spring.ssl.bundle.jks.nsi-soap-server.truststore.location=file:truststore.jks
spring.ssl.bundle.jks.nsi-soap-server.truststore.password=secret
spring.ssl.bundle.jks.nsi-soap-server.truststore.type=PKCS12
```

SSL hot reloading, both for JKS and PEM bundles, is supported as well:

```properties
spring.ssl.bundle.jks.nsi-soap-server.reload-on-update=true
spring.ssl.bundle.pem.nsi-soap-cleint.reload-on-update=true
```

> [!IMPORTANT]
> Support for the `javax.net.ssl.` properties is removed in PolyNSI version 0.4.0

> [!NOTE]
> The use of `server.ssl.*` is deprecated in favour of `spring.ssl.bundle.*`,
> but can still be used to configure the SOAP server.

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
and enable the authorization of that DN.  Last but not least, a list of allowed
client DN is configured. Note that, to allow PolyNSI to directly connect back
to the requesting NSA, the trust- and keystore are still needed.

The `application.properties` for PolyNSI behind an SSL SSL looks as follows:

```properties
#
# SOAP server configuration
#
cxf.path=/soap
soap.server.connection_provider.path=/connection/provider
soap.server.connection_requester.path=/connection/requester
server.port=8080
server.ssl.enabled=false
server.ssl.client-auth=none
nl.surf.polynsi.client.certificate.authorize-dn=header
nl.surf.polynsi.client.certificate.ssl-client-subject-dn-header=ssl-client-subject-dn
nl.surf.polynsi.client.certificate.distinguished-names[0]=CN=CertA,OU=Dept X,O=Company 1,C=NL
nl.surf.polynsi.client.certificate.distinguished-names[1]=CN=CertB,OU=Dept Y,O=Company 2,C=NL
nl.surf.polynsi.client.certificate.distinguished-names[2]=CN=CertC,OU=Dept Z,O=Company 3,C=NL
spring.ssl.bundle.pem.nsi-soap-client.keystore.certificate=file:server-certificate.pem
spring.ssl.bundle.pem.nsi-soap-client.keystore.private-key=file:server-private-key.pem
spring.ssl.bundle.pem.nsi-soap-client.key.password=secret
spring.ssl.bundle.pem.nsi-soap-client.truststore.certificate=file:other-nsa-trusted-bundle.pem
#
# gRPC server and client configuration
#
spring.grpc.server.port=9090
spring.grpc.client.channels.connection-provider.address=static://localhost:50051
spring.grpc.client.channels.connection-provider.negotiation-type=plaintext
```
