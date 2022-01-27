package nl.surf.polynsi.grpc.connection.requester;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import nl.surf.polynsi.ConverterException;
import nl.surf.polynsi.Direction;
import nl.surf.polynsi.ProxyException;
import nl.surf.polynsi.soap.connection.requester.ConnectionRequesterPort;
import nl.surf.polynsi.soap.connection.requester.ServiceException;
import nl.surf.polynsi.soap.connection.types.ObjectFactory;
import nl.surf.polynsi.soap.connection.types.ReservationConfirmCriteriaType;
import nl.surf.polynsi.soap.framework.headers.CommonHeaderType;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.ogf.nsi.grpc.connection.requester.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.xml.ws.Holder;
import java.util.logging.Logger;

import static nl.surf.polynsi.Converter.toSoap;

@GrpcService
public class ConnectionRequesterService extends ConnectionRequesterGrpc.ConnectionRequesterImplBase {
    private static final Logger LOG = Logger.getLogger(ConnectionRequesterService.class.getName());

//     @Autowired
//     ConnectionRequesterPort connectionRequesterPort;

    @Value("${soap.client.connection_requester.address}")
    private String connectionRequesterAddress;

    private ConnectionRequesterPort getSoapClientProxy(String replyTo) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(ConnectionRequesterPort.class);
        jaxWsProxyFactoryBean.setAddress(replyTo);
        return (ConnectionRequesterPort) jaxWsProxyFactoryBean.create();
    }

    @Override
    public void reserveConfirmed(ReserveConfirmedRequest pbReserveConfirmedRequest,
                                 StreamObserver<ReserveConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveConfirmed`.");
            ReserveConfirmedResponse pbReserveConfirmedResponse = ReserveConfirmedResponse.newBuilder()
                    .setHeader(pbReserveConfirmedRequest.getHeader()).build();

            ObjectFactory objectFactory = new ObjectFactory();

            ReservationConfirmCriteriaType soapReservationConfirmCriteria = objectFactory
                    .createReservationConfirmCriteriaType();
            soapReservationConfirmCriteria.setVersion(pbReserveConfirmedRequest.getCriteria().getVersion());
            soapReservationConfirmCriteria.setSchedule(toSoap(pbReserveConfirmedRequest.getCriteria().getSchedule()));
            soapReservationConfirmCriteria.setServiceType(pbReserveConfirmedRequest.getCriteria().getServiceType());

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveConfirmedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .reserveConfirmed(pbReserveConfirmedRequest.getConnectionId(), pbReserveConfirmedRequest
                            .getGlobalReservationId(), pbReserveConfirmedRequest
                            .getGlobalReservationId(), soapReservationConfirmCriteria, soapHeaderHolder);

            responseObserver.onNext(pbReserveConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handling `reserveConfirmed` call.", e);
        }
    }

    @Override
    public void reserveFailed(ReserveFailedRequest pbReserveFailedRequest,
                              StreamObserver<ReserveFailedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveFailed`.");
            ReserveFailedResponse pbReserveFailedResponse = ReserveFailedResponse.newBuilder()
                    .setHeader(pbReserveFailedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveFailedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .reserveFailed(pbReserveFailedRequest.getConnectionId(), toSoap(pbReserveFailedRequest
                            .getConnectionStates()), toSoap(pbReserveFailedRequest
                            .getServiceException()), soapHeaderHolder);

            responseObserver.onNext(pbReserveFailedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveFailed` call.", e);
        }
    }

    @Override
    public void reserveAbortConfirmed(ReserveAbortConfirmedRequest pbReserveAbortConfirmedRequest,
                                      StreamObserver<ReserveAbortConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveAbortConfirmed");
            ReserveAbortConfirmedResponse pbReserveAbortConfirmedResponse = ReserveAbortConfirmedResponse.newBuilder()
                    .setHeader(pbReserveAbortConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveAbortConfirmedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .reserveAbortConfirmed(pbReserveAbortConfirmedRequest.getConnectionId(), soapHeaderHolder);

            responseObserver.onNext(pbReserveAbortConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveAbortConfirmed` call.", e);
        }
    }

    @Override
    public void reserveCommitConfirmed(ReserveCommitConfirmedRequest pbReserveCommitConfirmedRequest,
                                       StreamObserver<ReserveCommitConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveCommitConfirmed`.");
            ReserveCommitConfirmedResponse pbReserveCommitConfirmedResponse = ReserveCommitConfirmedResponse
                    .newBuilder().setHeader(pbReserveCommitConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveCommitConfirmedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .reserveCommitConfirmed(pbReserveCommitConfirmedRequest.getConnectionId(), soapHeaderHolder);

            responseObserver.onNext(pbReserveCommitConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveCommitConfirmed` call.", e);
        }
    }

    @Override
    public void reserveCommitFailed(ReserveCommitFailedRequest pbReserveCommitFailedRequest,
                                    StreamObserver<ReserveCommitFailedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveCommitFailed`.");
            ReserveCommitFailedResponse pbReserveCommitFailedResponse = ReserveCommitFailedResponse.newBuilder()
                    .setHeader(pbReserveCommitFailedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveCommitFailedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .reserveCommitFailed(pbReserveCommitFailedRequest
                            .getConnectionId(), toSoap(pbReserveCommitFailedRequest
                            .getConnectionStates()), toSoap(pbReserveCommitFailedRequest
                            .getServiceException()), soapHeaderHolder);

            responseObserver.onNext(pbReserveCommitFailedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveCommitFailed` call.", e);
        }
    }

    @Override
    public void error(ErrorRequest pbErrorRequest,
        io.grpc.stub.StreamObserver<ErrorResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `error`.");
            LOG.info("Received protobuf message `error`:\n" + pbErrorRequest.toString());
            ErrorResponse pbErrorResponse = ErrorResponse.newBuilder()
                    .setHeader(pbErrorRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbErrorRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .error(toSoap(pbErrorRequest
                            .getServiceException()), soapHeaderHolder);

            responseObserver.onNext(pbErrorResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `error` call.", e);
        }
    }

    @Override
    public void provisionConfirmed(ProvisionConfirmedRequest pbProvisionConfirmedRequest,
                                       StreamObserver<ProvisionConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `ProvisionConfirmed`.");
            ProvisionConfirmedResponse pbProvisionConfirmedResponse = ProvisionConfirmedResponse
                    .newBuilder().setHeader(pbProvisionConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbProvisionConfirmedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .provisionConfirmed(
                            pbProvisionConfirmedRequest.getConnectionId(),
                            soapHeaderHolder);

            responseObserver.onNext(pbProvisionConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `ProvisionConfirmed` call.", e);
        }
    }

    @Override
    public void releaseConfirmed(ReleaseConfirmedRequest pbReleaseConfirmedRequest,
                                       StreamObserver<ReleaseConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `ReleaseConfirmed`.");
            ReleaseConfirmedResponse pbReleaseConfirmedResponse = ReleaseConfirmedResponse
                    .newBuilder().setHeader(pbReleaseConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReleaseConfirmedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .releaseConfirmed(
                            pbReleaseConfirmedRequest.getConnectionId(),
                            soapHeaderHolder);

            responseObserver.onNext(pbReleaseConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `ReleaseConfirmed` call.", e);
        }
    }


    @Override
    public void terminateConfirmed(TerminateConfirmedRequest pbTerminateConfirmedRequest,
                                       StreamObserver<TerminateConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `TerminateConfirmed`.");
            TerminateConfirmedResponse pbTerminateConfirmedResponse = TerminateConfirmedResponse
                    .newBuilder().setHeader(pbTerminateConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbTerminateConfirmedRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .terminateConfirmed(
                            pbTerminateConfirmedRequest.getConnectionId(),
                            soapHeaderHolder);

            responseObserver.onNext(pbTerminateConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `TerminateConfirmed` call.", e);
        }
    }

    @Override
    public void dataPlaneStateChange(DataPlaneStateChangeRequest pbDataPlaneStateChangeRequest,
                                    StreamObserver<DataPlaneStateChangeResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `dataPlaneStateChange`.");
            DataPlaneStateChangeResponse pbDataPlaneStateChangeResponse = DataPlaneStateChangeResponse.newBuilder()
                    .setHeader(pbDataPlaneStateChangeRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbDataPlaneStateChangeRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .dataPlaneStateChange(
                            pbDataPlaneStateChangeRequest.getNotification().getConnectionId(),
                            pbDataPlaneStateChangeRequest.getNotification().getNotificationId(),
                            toSoap(pbDataPlaneStateChangeRequest.getNotification().getTimeStamp()),
                            toSoap(pbDataPlaneStateChangeRequest.getDataPlaneStatus()),
                            soapHeaderHolder);

            responseObserver.onNext(pbDataPlaneStateChangeResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `datePlaneStateChange` call.", e);
        }
    }

    @Override
    public void reserveTimeout(ReserveTimeoutRequest pbReserveTimeoutRequest,
                                    StreamObserver<ReserveTimeoutResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveTimeout`.");
            ReserveTimeoutResponse pbReserveTimeoutResponse = ReserveTimeoutResponse.newBuilder()
                    .setHeader(pbReserveTimeoutRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveTimeoutRequest.getHeader());
            getSoapClientProxy(connectionRequesterAddress)
                    .reserveTimeout(
                            pbReserveTimeoutRequest.getNotification().getConnectionId(),
                            pbReserveTimeoutRequest.getNotification().getNotificationId(),
                            toSoap(pbReserveTimeoutRequest.getNotification().getTimeStamp()),
                            pbReserveTimeoutRequest.getTimeoutValue(),
                            pbReserveTimeoutRequest.getOriginatingConnectionId(),
                            pbReserveTimeoutRequest.getOriginatingNsa(),
                            soapHeaderHolder);

            responseObserver.onNext(pbReserveTimeoutResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveTimeout` call.", e);
        }
    }
}

