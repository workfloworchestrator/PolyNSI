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
header files needed to built the gRPC compiler plugin. For Maven to use the
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
system is unaware of those locations. Hence we need to tell it about them:

    $ export CXXFLAGS="-I/usr/local/include" LDFLAGS="-L/usr/local/lib"
    
Yes, the `protoc` compiler is a C++ application and so is the gRPC
plugin for Java!
 
We also need to tell the `grpc-java` build system where to find the
`protoc` compiler. Furthermore, we are not interested in building things
for Android. So we create a file `grpc-java/gradle.properties` and add
the following lines to it:
 
    skipAndroid=true
    protoc=/usr/local/bin/protoc
    
Before we build the plugin we need to do one last thing; patch a
Bash script that sanity checks the build artifact. Currently, it only
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
less then trivial to set a value for the `protoc` property if it needs to be
shared by all submodules. However with a `settings.xml` file in the `~/.m2`
directory we will be able to set property values that will be picked up by any
POM whether it is a submodule or not.

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

The version of the `protobuf-java` jars we want to built need to match the
version of the protoc compiler:

    $ protoc --version
    libprotoc 3.12.2

This shows we need to checkout the v3.12.2 release:

    $ cd protobuf
    $ git checkout v3.12.2
    
And finally build the `protobuf-java` jars:

    $ cd java
    $ mvn install

