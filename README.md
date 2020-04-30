# PolyNSI

A SOAP to gRPC translating proxy server for the NSI protocol.

# Quick Start

* [install (Open)JDK 11](https://openjdk.java.net/)
* [install Apache Maven](http://maven.apache.org/install.html})
* mvn clean generate-sources
* mvn test
* mvn spring-boot:run
* point your browser at: http://localhost:8080/soap

# gRPC

gRPC is an extension of Protocol Buffers. Messages are still defined using
Protocol Buffers v3. The service definitions though, extend the Protocol
Buffers syntax. This means that the Protocol Buffers compiler `protoc`
cannot compile all the `.proto` files used for gRPC. The compiler needs a
plugin to do so. For most common platforms (Windows, MacOS and Linux)
pre-built binaries are available. For FreeBSD the plugin needs to be
built locally.

## Building the gRPC protoc plugin on FreeBSD

First install the `protobuf` package:

    $ sudo pkg protobuf
    
This not only installs the `protoc` compiler, but also all the relevant
header files needed to built the gRPC compiler plugin.

Next checkout the 1 branch of the `grpc-java` project. This project
contains a `protoc` plugin that generates Java files.

    $ git clone -b v1.29.0 https://github.com/grpc/grpc-java
    
On FreeBSD, third party libraries and headers files are located in `/usr
/local/lib` and `/urs/local/include` by default. The `grpc-java` build
system is unaware of those locations. Hence we need to tell it about them:

    $ export CXXFLAGS="-I/usr/local/include" LDFLAGS="-L/usr/local/lib"
    
Yes, the `protoc` compiler is a C++ application and so is the gRPC
plugin for Java!
 
We also need to tell the `grpc-java` build system where to find the
`protoc` compiler. Furthermore we are not interested in building things
for Android. So we create a file `grpc-java/gradle.properties` and add
the following lines to it:
 
    skipAndroid=true
    protoc=/usr/local/bin/protoc
    
Before we build the plugin we need to do one last thing; patch a
Bash script that sanity checks the build artifact. Currently it only
recognizes artifacts (the plugin) build on Linux, MacOS and
Windows. The patch teaches it about FreeBSD:

    $ cd grpc-java/compiler
    $ git apply <...>/polynsi/grpc-java-compiler.patch
 
Building the plugin should now be a simple matter of (while still being
in the directory `grpc-java/compile`):

    $ ../gradlew clean java_pluginExecutable test publishToMavenLocal

To test whether the plugin now actually resides in your local Maven 
repository execute:

    $ ls -l ~/.m2/repository/io/grpc/protoc-gen-grpc-java/1.29.0/
    
That directory should have a file called.
`protoc-gen-grpc-java-1.29.0-freebsd-x86_64.exe`

## Installing protoc into the Maven repository

Although we have installed the FreeBSD `protoc` package, Maven still
doesn't know where to look for it. To that end execute:

   $ mvn install:install-file \ 
        -DgroupId=com.google.protobuf \ 
        -DartifactId=protoc -Dversion=3.11.4 \ 
        -Dclassifier=freebsd-x86_64 \
        -Dpackaging=exe -Dfile=/usr/local/bin/protoc

For Linux, MacOS and Windows this won't be necessary.
