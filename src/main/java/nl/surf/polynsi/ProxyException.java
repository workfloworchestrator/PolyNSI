package nl.surf.polynsi;

public class ProxyException extends RuntimeException {
    private String messagePostfix;

    public ProxyException(Direction direction, String message, Throwable cause) {
        super(message, cause);
        switch (direction) {
            case GRPC_TO_SOAP:
                this.messagePostfix = " (gRPC -> SOAP)";
                break;
            case SOAP_TO_GRPC:
                this.messagePostfix = " (SOAP -> gRPC)";
                break;
        }
    }

    @Override
    public String getMessage() {
        return super.getMessage() + this.messagePostfix;
    }
}
