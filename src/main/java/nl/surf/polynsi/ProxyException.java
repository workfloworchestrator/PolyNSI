package nl.surf.polynsi;

public class ProxyException extends RuntimeException {
    private String messagePrefix;

    public ProxyException(Direction direction, String message, Throwable cause) {
        super(message, cause);
        switch (direction) {
            case GRPC_TO_SOAP:
                this.messagePrefix = "gRPC->SOAP " ;
                break;
            case SOAP_TO_GRPC:
                this.messagePrefix = "SOAP->gRPC ";
                break;
        }
    }

    @Override
    public String getMessage() {
        return this.messagePrefix + "exception in " + super.getMessage();
    }
}
