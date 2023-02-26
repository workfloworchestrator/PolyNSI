package nl.surf.polynsi.grpc.connection.requester;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import net.devh.boot.grpc.server.service.GrpcService;
import nl.surf.polynsi.ConverterException;
import nl.surf.polynsi.Direction;
import nl.surf.polynsi.ProxyException;
import nl.surf.polynsi.soap.connection.requester.ConnectionRequesterPort;
import nl.surf.polynsi.soap.connection.requester.ServiceException;
import nl.surf.polynsi.soap.connection.types.*;
import nl.surf.polynsi.soap.framework.headers.CommonHeaderType;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.ogf.nsi.grpc.connection.requester.*;

import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.logging.Logger;

import static com.google.protobuf.util.Timestamps.EPOCH;
import static nl.surf.polynsi.Converter.*;

@GrpcService
public class ConnectionRequesterService extends ConnectionRequesterGrpc.ConnectionRequesterImplBase {
    private static final Logger LOG = Logger.getLogger(ConnectionRequesterService.class.getName());

    /*
        Catch ProxyException that can be thrown during gRPC -> SOAP processing and
        send a gRPC UNAVAILABLE back with the cause from the exception.
     */
    @GrpcAdvice
    public static class GrpcExceptionAdvice {

        @GrpcExceptionHandler(ProxyException.class)
        public Status handleProxyException(ProxyException e) {
            String description = e.getMessage() + ": " + e.getCause().getMessage();
            LOG.warning(description);
            return Status.UNAVAILABLE.withDescription(description).withCause(e);
        }
    }

    /*
        Create connection requester proxy to send SOAP message.
     */
    private ConnectionRequesterPort connectionRequesterProxy(String replyTo) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(ConnectionRequesterPort.class);
        jaxWsProxyFactoryBean.setAddress(replyTo);
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        loggingFeature.setVerbose(true);
        loggingFeature.setLogMultipart(true);
        jaxWsProxyFactoryBean.getFeatures().add(loggingFeature);
        return (ConnectionRequesterPort) jaxWsProxyFactoryBean.create();
    }

    @Override
    public void reserveConfirmed(ReserveConfirmedRequest pbReserveConfirmedRequest,
                                 StreamObserver<ReserveConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP reserveConfirmed to %s at %s",
                    pbReserveConfirmedRequest.getHeader().getRequesterNsa(),
                    pbReserveConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `reserveConfirmed`:\n" + pbReserveConfirmedRequest.toString());

            ReserveConfirmedResponse pbReserveConfirmedResponse = ReserveConfirmedResponse.newBuilder()
                    .setHeader(pbReserveConfirmedRequest.getHeader()).build();

            ReservationConfirmCriteriaType soapReservationConfirmCriteria = toSoap(pbReserveConfirmedRequest.getCriteria());
            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveConfirmedRequest.getHeader());

            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReserveConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .reserveConfirmed(pbReserveConfirmedRequest.getConnectionId(), pbReserveConfirmedRequest
                            .getGlobalReservationId(), pbReserveConfirmedRequest
                            .getGlobalReservationId(), soapReservationConfirmCriteria, soapHeaderHolder);

            responseObserver.onNext(pbReserveConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "reserveConfirmed", e);
        }
    }

    @Override
    public void reserveFailed(ReserveFailedRequest pbReserveFailedRequest,
                              StreamObserver<ReserveFailedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP reserveFailed to %s at %s",
                    pbReserveFailedRequest.getHeader().getRequesterNsa(),
                    pbReserveFailedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `reserveFailed`:\n" + pbReserveFailedRequest.toString());

            ReserveFailedResponse pbReserveFailedResponse = ReserveFailedResponse.newBuilder()
                    .setHeader(pbReserveFailedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveFailedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReserveFailedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .reserveFailed(pbReserveFailedRequest.getConnectionId(), toSoap(pbReserveFailedRequest
                            .getConnectionStates()), toSoap(pbReserveFailedRequest
                            .getServiceException()), soapHeaderHolder);

            responseObserver.onNext(pbReserveFailedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "reserveFailed", e);
        }
    }

    @Override
    public void reserveAbortConfirmed(ReserveAbortConfirmedRequest pbReserveAbortConfirmedRequest,
                                      StreamObserver<ReserveAbortConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP reserveAbortConfirmed to %s at %s",
                    pbReserveAbortConfirmedRequest.getHeader().getRequesterNsa(),
                    pbReserveAbortConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `reserveAbortConfirmed`:\n" + pbReserveAbortConfirmedRequest.toString());

            ReserveAbortConfirmedResponse pbReserveAbortConfirmedResponse = ReserveAbortConfirmedResponse.newBuilder()
                    .setHeader(pbReserveAbortConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveAbortConfirmedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReserveAbortConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .reserveAbortConfirmed(pbReserveAbortConfirmedRequest.getConnectionId(), soapHeaderHolder);

            responseObserver.onNext(pbReserveAbortConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "reserveAbortConfirmed", e);
        }
    }

    @Override
    public void reserveCommitConfirmed(ReserveCommitConfirmedRequest pbReserveCommitConfirmedRequest,
                                       StreamObserver<ReserveCommitConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP reserveCommitConfirmed to %s at %s",
                    pbReserveCommitConfirmedRequest.getHeader().getRequesterNsa(),
                    pbReserveCommitConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `reserveCommitConfirmed`:\n" + pbReserveCommitConfirmedRequest.toString());

            ReserveCommitConfirmedResponse pbReserveCommitConfirmedResponse = ReserveCommitConfirmedResponse
                    .newBuilder().setHeader(pbReserveCommitConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveCommitConfirmedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReserveCommitConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .reserveCommitConfirmed(pbReserveCommitConfirmedRequest.getConnectionId(), soapHeaderHolder);

            responseObserver.onNext(pbReserveCommitConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "reserveCommitConfirmed", e);
        }
    }

    @Override
    public void reserveCommitFailed(ReserveCommitFailedRequest pbReserveCommitFailedRequest,
                                    StreamObserver<ReserveCommitFailedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP reserveCommitFailed to %s at %s",
                    pbReserveCommitFailedRequest.getHeader().getRequesterNsa(),
                    pbReserveCommitFailedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `reserveCommitFailed`:\n" + pbReserveCommitFailedRequest.toString());

            ReserveCommitFailedResponse pbReserveCommitFailedResponse = ReserveCommitFailedResponse.newBuilder()
                    .setHeader(pbReserveCommitFailedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveCommitFailedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReserveCommitFailedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .reserveCommitFailed(pbReserveCommitFailedRequest
                            .getConnectionId(), toSoap(pbReserveCommitFailedRequest
                            .getConnectionStates()), toSoap(pbReserveCommitFailedRequest
                            .getServiceException()), soapHeaderHolder);

            responseObserver.onNext(pbReserveCommitFailedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "reserveCommitFailed", e);
        }
    }

    @Override
    public void error(ErrorRequest pbErrorRequest,
                      io.grpc.stub.StreamObserver<ErrorResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP error to %s at %s",
                    pbErrorRequest.getHeader().getRequesterNsa(),
                    pbErrorRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `error`:\n" + pbErrorRequest.toString());

            ErrorResponse pbErrorResponse = ErrorResponse.newBuilder()
                    .setHeader(pbErrorRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbErrorRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbErrorRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .error(toSoap(pbErrorRequest
                            .getServiceException()), soapHeaderHolder);

            responseObserver.onNext(pbErrorResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "error", e);
        }
    }

    @Override
    public void errorEvent(ErrorEventRequest pbErrorEventRequest,
                           io.grpc.stub.StreamObserver<ErrorEventResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP errorEvent to %s at %s",
                    pbErrorEventRequest.getHeader().getRequesterNsa(),
                    pbErrorEventRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `errorEvent`:\n" + pbErrorEventRequest.toString());

            ErrorEventResponse pbErrorEventResponse = ErrorEventResponse.newBuilder()
                    .setHeader(pbErrorEventRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbErrorEventRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbErrorEventRequest.getHeader().getReplyTo());
            connectionRequesterProxy.errorEvent(
                            pbErrorEventRequest.getNotification().getConnectionId(),
                            pbErrorEventRequest.getNotification().getNotificationId(),
                            toSoap(pbErrorEventRequest.getNotification().getTimeStamp()),
                            toSoap(pbErrorEventRequest.getEvent()),
                            pbErrorEventRequest.getOriginatingConnectionId(),
                            pbErrorEventRequest.getOriginatingNsa(),
                            toValuePairList(toSoap(pbErrorEventRequest.getAdditionalInfoList())),
                            toSoap(pbErrorEventRequest.getServiceException()),
                            soapHeaderHolder
            );

            responseObserver.onNext(pbErrorEventResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "errorEvent", e);
        }
    }

    @Override
    public void provisionConfirmed(ProvisionConfirmedRequest pbProvisionConfirmedRequest,
                                       StreamObserver<ProvisionConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP provisionConfirmed to %s at %s",
                    pbProvisionConfirmedRequest.getHeader().getRequesterNsa(),
                    pbProvisionConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `provisionConfirmed`:\n" + pbProvisionConfirmedRequest.toString());

            ProvisionConfirmedResponse pbProvisionConfirmedResponse = ProvisionConfirmedResponse
                    .newBuilder().setHeader(pbProvisionConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbProvisionConfirmedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbProvisionConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .provisionConfirmed(
                            pbProvisionConfirmedRequest.getConnectionId(),
                            soapHeaderHolder);

            responseObserver.onNext(pbProvisionConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "ProvisionConfirmed", e);
        }
    }

    @Override
    public void releaseConfirmed(ReleaseConfirmedRequest pbReleaseConfirmedRequest,
                                       StreamObserver<ReleaseConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP releaseConfirmed to %s at %s",
                    pbReleaseConfirmedRequest.getHeader().getRequesterNsa(),
                    pbReleaseConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `releaseConfirmed`:\n" + pbReleaseConfirmedRequest.toString());

            ReleaseConfirmedResponse pbReleaseConfirmedResponse = ReleaseConfirmedResponse
                    .newBuilder().setHeader(pbReleaseConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReleaseConfirmedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReleaseConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .releaseConfirmed(
                            pbReleaseConfirmedRequest.getConnectionId(),
                            soapHeaderHolder);

            responseObserver.onNext(pbReleaseConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "ReleaseConfirmed", e);
        }
    }


    @Override
    public void terminateConfirmed(TerminateConfirmedRequest pbTerminateConfirmedRequest,
                                       StreamObserver<TerminateConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP terminateConfirmed to %s at %s",
                    pbTerminateConfirmedRequest.getHeader().getRequesterNsa(),
                    pbTerminateConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `terminateConfirmed`:\n" + pbTerminateConfirmedRequest.toString());

            TerminateConfirmedResponse pbTerminateConfirmedResponse = TerminateConfirmedResponse
                    .newBuilder().setHeader(pbTerminateConfirmedRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbTerminateConfirmedRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbTerminateConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .terminateConfirmed(
                            pbTerminateConfirmedRequest.getConnectionId(),
                            soapHeaderHolder);

            responseObserver.onNext(pbTerminateConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "TerminateConfirmed", e);
        }
    }

    @Override
    public void dataPlaneStateChange(DataPlaneStateChangeRequest pbDataPlaneStateChangeRequest,
                                    StreamObserver<DataPlaneStateChangeResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP dataPlaneStateChange to %s at %s",
                    pbDataPlaneStateChangeRequest.getHeader().getRequesterNsa(),
                    pbDataPlaneStateChangeRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `dataPlaneStateChange`:\n" + pbDataPlaneStateChangeRequest.toString());

            DataPlaneStateChangeResponse pbDataPlaneStateChangeResponse = DataPlaneStateChangeResponse.newBuilder()
                    .setHeader(pbDataPlaneStateChangeRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbDataPlaneStateChangeRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbDataPlaneStateChangeRequest.getHeader().getReplyTo());
            connectionRequesterProxy
                    .dataPlaneStateChange(
                            pbDataPlaneStateChangeRequest.getNotification().getConnectionId(),
                            pbDataPlaneStateChangeRequest.getNotification().getNotificationId(),
                            toSoap(pbDataPlaneStateChangeRequest.getNotification().getTimeStamp()),
                            toSoap(pbDataPlaneStateChangeRequest.getDataPlaneStatus()),
                            soapHeaderHolder);

            responseObserver.onNext(pbDataPlaneStateChangeResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "datePlaneStateChange", e);
        }
    }

    @Override
    public void reserveTimeout(ReserveTimeoutRequest pbReserveTimeoutRequest,
                                    StreamObserver<ReserveTimeoutResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP reserveTimeout to %s at %s",
                    pbReserveTimeoutRequest.getHeader().getRequesterNsa(),
                    pbReserveTimeoutRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `reserveTimeout`:\n" + pbReserveTimeoutRequest.toString());

            ReserveTimeoutResponse pbReserveTimeoutResponse = ReserveTimeoutResponse.newBuilder()
                    .setHeader(pbReserveTimeoutRequest.getHeader()).build();

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbReserveTimeoutRequest.getHeader());
            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbReserveTimeoutRequest.getHeader().getReplyTo());
            connectionRequesterProxy
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
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "reserveTimeout", e);
        }
    }

    @Override
    public void querySummaryConfirmed(QueryConfirmedRequest pbQueryConfirmedRequest,
                                      StreamObserver<QueryConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP querySummaryConfirmed to %s at %s",
                    pbQueryConfirmedRequest.getHeader().getRequesterNsa(),
                    pbQueryConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `queryConfirmed`:\n" + pbQueryConfirmedRequest.toString());

            QueryConfirmedResponse pbQueryConfirmedResponse = QueryConfirmedResponse.newBuilder()
                    .setHeader(pbQueryConfirmedRequest.getHeader()).build();

            List<QuerySummaryResultType> soapReservations = toSoap(pbQueryConfirmedRequest);

            OffsetDateTime lastModified = null;
            if (!pbQueryConfirmedRequest.getLastModified().equals(EPOCH)) {
                lastModified = toSoap(pbQueryConfirmedRequest.getLastModified());
            }

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbQueryConfirmedRequest.getHeader());

            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbQueryConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy.querySummaryConfirmed(soapReservations, lastModified, soapHeaderHolder);

            responseObserver.onNext(pbQueryConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "querySummaryConfirmed", e);
        }
    }

    @Override
    public void queryRecursiveConfirmed(QueryConfirmedRequest pbQueryConfirmedRequest,
                                        StreamObserver<QueryConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP queryRecursiveConfirmed to %s at %s",
                    pbQueryConfirmedRequest.getHeader().getRequesterNsa(),
                    pbQueryConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `queryConfirmed`:\n" + pbQueryConfirmedRequest.toString());

            QueryConfirmedResponse pbQueryConfirmedResponse = QueryConfirmedResponse.newBuilder()
                    .setHeader(pbQueryConfirmedRequest.getHeader()).build();

            List<QueryRecursiveResultType> soapReservations = toSoapQueryRecursiveResult(pbQueryConfirmedRequest);

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbQueryConfirmedRequest.getHeader());

            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbQueryConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy.queryRecursiveConfirmed(soapReservations, soapHeaderHolder);

            responseObserver.onNext(pbQueryConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "querySummaryConfirmed", e);
        }
    }

    @Override
    public void queryNotificationConfirmed(QueryNotificationConfirmedRequest pbQueryNotificationConfirmedRequest,
                                           StreamObserver<QueryNotificationConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP queryNotificationConfirmed to %s at %s",
                    pbQueryNotificationConfirmedRequest.getHeader().getRequesterNsa(),
                    pbQueryNotificationConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `queryNotificationConfirmed`:\n" + pbQueryNotificationConfirmedRequest.toString());

            QueryNotificationConfirmedResponse pbQueryNotificationConfirmedResponse = QueryNotificationConfirmedResponse.newBuilder()
                    .setHeader(pbQueryNotificationConfirmedRequest.getHeader()).build();

            QueryNotificationConfirmedType soapQueryNotificationConfirmed = toSoap(pbQueryNotificationConfirmedRequest);

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbQueryNotificationConfirmedRequest.getHeader());

            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbQueryNotificationConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy.queryNotificationConfirmed(soapQueryNotificationConfirmed, soapHeaderHolder);

            responseObserver.onNext(pbQueryNotificationConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "querySummaryConfirmed", e);
        }
    }

    public void queryResultConfirmed(QueryResultConfirmedRequest pbQueryResultConfirmedRequest,
                                     StreamObserver<org.ogf.nsi.grpc.connection.requester.QueryResultConfirmedResponse> responseObserver) {
        try {
            LOG.info(String.format("gRPC->SOAP queryResultConfirmed to %s at %s",
                    pbQueryResultConfirmedRequest.getHeader().getRequesterNsa(),
                    pbQueryResultConfirmedRequest.getHeader().getReplyTo()));
            LOG.finer("Received protobuf message `queryResultConfirmed`:\n" + pbQueryResultConfirmedRequest);

            QueryResultConfirmedResponse pbQueryResultConfirmedResponse = QueryResultConfirmedResponse.newBuilder()
                    .setHeader(pbQueryResultConfirmedRequest.getHeader()).build();

            List<QueryResultResponseType> soapResultResponses = toSoap(pbQueryResultConfirmedRequest);

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbQueryResultConfirmedRequest.getHeader());

            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbQueryResultConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy.queryResultConfirmed(soapResultResponses, soapHeaderHolder);

            responseObserver.onNext(pbQueryResultConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException | WebServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "queryResultConfirmed", e);
        }
    }
}
