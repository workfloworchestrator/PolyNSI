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

* [install (Open)JDK 11](https://openjdk.java.net/)
* [install Apache Maven](http://maven.apache.org/install.html)
* mvn clean generate-sources
* mvn test
* mvn spring-boot:run
* point your browser at: http://localhost:8080/soap

# Installation

Normally the Quick Start instructions should be all that's required to get
PolyNSI installed and running with a default configuration. Maven will find
everything it needs and installs it into its local repository. Thus making it
available to PolyNSI. This includes binary dependencies such as the Protocol
Buffers compiler with its gRPC plugin. However, for some less common, but
otherwise equally relevant OSes in the networkin space, such as FreeBSD, a bit
more work is required. See the section 
[Advanced Installation](#Advanced-Installation) for more details.  

# Configuration

All PolyNSI configuration is done by properties. As PolyNSI is a Spring Boot
application it follows Springs Boot's conventions for obtaining 
[those properties from files](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/reference/html/spring-boot-features.html#boot-features-external-config-application-property-files).  

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

See also:
* [Application Property Files](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/reference/html/spring-boot-features.html#boot-features-external-config-application-property-files)
* [Common (Spring Boot) Application Properties](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/reference/html/appendix-application-properties.html#common-application-properties)

# Advanced Installation

As detailed in the section [Installation](#Installation) some OSes, such as
FreeBSD, require a bit more work to install PolyNSI on. The reason for this is
that the Protocol Buffers and gRPC projects do not provide precompiled versions
of their compiler and plugin for them. Hence we need to compile these
ourselves. In some cases the package manager of the OS in question might help us
with that. That happens to be the case with FreeBSD as we will shortly see. 


## Building the gRPC protoc plugin on FreeBSD

First install the `protobuf` package:

    $ sudo pkg protobuf
    
This not only installs the `protoc` compiler, but also all the relevant
header files needed, to build the gRPC compiler plugin. For Maven to use the
`protoc` compiler we need to tell it where to find it:

    $ mvn install:install-file \
        -DgroupId=com.google.protobuf \
        -DartifactId=protoc \
        -Dversion=3.12.2 \
        -Dclassifier=freebsd-x86_64 \
        -Dpackaging=exe \
        -Dfile=/usr/local/bin/protoc

Next checkout the 1.29 branch of the `grpc-java` project. This project
contains a `protoc` plugin that generates Java files.

    $ git clone -b v1.29.0 https://github.com/grpc/grpc-java
    
On FreeBSD, third party libraries and headers files are located in `/usr
/local/lib` and `/urs/local/include` by default. The `grpc-java` build
system is unaware of those locations. Hence, we need to tell it about them:

    $ export CXXFLAGS="-I/usr/local/include" LDFLAGS="-L/usr/local/lib"
    
Yes, the `protoc` compiler is a C++ application and so is the gRPC
plugin for Java!
 
We also need to tell the `grpc-java` build system where to find the
`protoc` compiler. Furthermore, we are not interested in building things
for Android. So we create a file `grpc-java/gradle.properties` and add
the following lines to it:
 
    skipAndroid=true
    protoc=/usr/local/bin/protoc
    
Before we build the plugin, we need to do one last thing; patch a
Bash script that sanity checks the build artifact. Currently, it only
recognizes artifacts (the plugin) built on Linux, MacOS and
Windows. The patch teaches it about FreeBSD:

    $ cd grpc-java/compiler
    $ git apply <...>/polynsi/grpc-java-compiler.patch
 
Building the plugin should now be a simple matter of (while still being
in the directory `grpc-java/compile`):

    $ ../gradlew clean java_pluginExecutable test publishToMavenLocal

To test whether the plugin now actually resides in your local Maven 
repository, execute:

    $ ls -l ~/.m2/repository/io/grpc/protoc-gen-grpc-java/1.29.0/
    
That directory should have a file called.
`protoc-gen-grpc-java-1.29.0-freebsd-x86_64.exe`

## Building protobuf-java jars on FreeBSD

Generally, the required `protobuf-java` jars are available from the Maven Central
Repository and will be downloaded by Maven automatically. However sometimes we
want to build them from source if we need a specific version that's not yet
available from the Maven Central Repository. 

Building the `protobuf-java` jars requires the `protoc` compiler, that we
already have installed, but in a different location from where the build
configuration expects it. Contrary to `grpc-java` that uses the Gradle build
system, `protobuf-java` uses Maven. As such, specifying the location of the
`protoc` compiler is a little different. Even more so as protobuf-java uses an
aggregate POM. With an aggregrate POM, properties specified on the command line
using the `-D` parameter, are not passed on to submodules (POMs). That makes it
less than trivial to set a value for the `protoc` property if it needs to be
shared by all submodules. However, with a `settings.xml` file in the `~/.m2`
directory we will be able to set property values that will be picked up by any
POM, whether it is a submodule or not.

So create a file `~/.m2/settings.xml` with the following contents:

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
        <profiles>
            <profile>
                <id>freebsd</id>
                <activation>
                    <activeByDefault>true</activeByDefault>
                </activation>
                <properties>
                    <protoc>/usr/local/bin/protoc</protoc>
                </properties>
            </profile>
        </profiles>
    </settings>

With that out of the way we now need to clone the `protobuf` repository:

    $ git clone git@github.com:protocolbuffers/protobuf.git

The version of the `protobuf-java` jars we want to build, need to match the
version of the protoc compiler:

    $ protoc --version
    libprotoc 3.12.2

This shows that we need to checkout the v3.12.2 release:

    $ cd protobuf
    $ git checkout v3.12.2
    
And, finally, build the `protobuf-java` jars:

    $ cd java
    $ mvn install

