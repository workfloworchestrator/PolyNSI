package nl.surf.polynsi;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProxyExceptionTest {

    @Test
    void grpcToSoapMessageFormat() {
        ProxyException ex =
                new ProxyException(Direction.GRPC_TO_SOAP, "reserveConfirmed", new RuntimeException("test cause"));

        assertEquals("gRPC->SOAP exception in reserveConfirmed", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("test cause", ex.getCause().getMessage());
    }

    @Test
    void soapToGrpcMessageFormat() {
        ProxyException ex =
                new ProxyException(Direction.SOAP_TO_GRPC, "provision", new RuntimeException("connection error"));

        assertEquals("SOAP->gRPC exception in provision", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("connection error", ex.getCause().getMessage());
    }

    @Test
    void isRuntimeException() {
        ProxyException ex = new ProxyException(Direction.GRPC_TO_SOAP, "test", new RuntimeException());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
