package nl.surf.polynsi.grpc.connection.requester;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import nl.surf.polynsi.ConverterException;
import nl.surf.polynsi.Direction;
import nl.surf.polynsi.ProxyException;
import nl.surf.polynsi.soap.connection.requester.ConnectionRequesterPort;
import nl.surf.polynsi.soap.connection.requester.ServiceException;
import nl.surf.polynsi.soap.connection.types.ObjectFactory;
import nl.surf.polynsi.soap.connection.types.QuerySummaryResultCriteriaType;
import nl.surf.polynsi.soap.connection.types.QuerySummaryResultType;
import nl.surf.polynsi.soap.connection.types.ReservationConfirmCriteriaType;
import nl.surf.polynsi.soap.framework.headers.CommonHeaderType;
import nl.surf.polynsi.soap.services.p2p.P2PServiceBaseType;
import nl.surf.polynsi.soap.services.types.DirectionalityType;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.ogf.nsi.grpc.connection.requester.*;
import org.ogf.nsi.grpc.services.Directionality;
import org.ogf.nsi.grpc.services.PointToPointService;

import javax.xml.ws.Holder;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.google.protobuf.util.Timestamps.EPOCH;
import static nl.surf.polynsi.Converter.toSoap;

@GrpcService
public class ConnectionRequesterService extends ConnectionRequesterGrpc.ConnectionRequesterImplBase {
    private static final Logger LOG = Logger.getLogger(ConnectionRequesterService.class.getName());

//    @Autowired
//    ConnectionRequesterPort connectionRequesterProxy;

    static ConnectionRequesterPort connectionRequesterProxy(String address) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(ConnectionRequesterPort.class);
        jaxWsProxyFactoryBean.setAddress(address);
        return (ConnectionRequesterPort) jaxWsProxyFactoryBean.create();
    }

    @Override
    public void reserveConfirmed(ReserveConfirmedRequest pbReserveConfirmedRequest,
                                 StreamObserver<ReserveConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveConfirmed` to "
                    + pbReserveConfirmedRequest.getHeader().getReplyTo());

            ReserveConfirmedResponse pbReserveConfirmedResponse = ReserveConfirmedResponse.newBuilder()
                    .setHeader(pbReserveConfirmedRequest.getHeader()).build();

            ObjectFactory objectFactory = new ObjectFactory();

            ReservationConfirmCriteriaType soapReservationConfirmCriteria = objectFactory
                    .createReservationConfirmCriteriaType();
            ReservationConfirmCriteria pbCriteria = pbReserveConfirmedRequest.getCriteria();
            soapReservationConfirmCriteria.setVersion(pbCriteria.getVersion());
            soapReservationConfirmCriteria.setSchedule(toSoap(pbCriteria.getSchedule()));
            soapReservationConfirmCriteria.setServiceType(pbCriteria.getServiceType());
            nl.surf.polynsi.soap.services.p2p.ObjectFactory servicesObjFactory =
                    new nl.surf.polynsi.soap.services.p2p.ObjectFactory();
            P2PServiceBaseType soapP2PServiceType = servicesObjFactory.createP2PServiceBaseType();
            PointToPointService pbPtps = pbCriteria.getPtps();
            Directionality pbDirectionality = pbPtps.getDirectionality();
            if (pbDirectionality == Directionality.UNI_DIRECTIONAL)
                soapP2PServiceType.setDirectionality(DirectionalityType.UNIDIRECTIONAL);
            else
                soapP2PServiceType.setDirectionality(DirectionalityType.BIDIRECTIONAL);
            soapP2PServiceType.setCapacity(pbPtps.getCapacity());
            soapP2PServiceType.setSymmetricPath(pbPtps.getSymmetricPath());
            soapP2PServiceType.setSourceSTP(pbPtps.getSourceStp());
            soapP2PServiceType.setDestSTP(pbPtps.getDestStp());
            soapReservationConfirmCriteria.getAny().add(servicesObjFactory.createP2Ps(soapP2PServiceType));
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handling `reserveConfirmed` call.", e);
        }
    }

    @Override
    public void reserveFailed(ReserveFailedRequest pbReserveFailedRequest,
                              StreamObserver<ReserveFailedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveFailed` to "
                    + pbReserveFailedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveFailed` call.", e);
        }
    }

    @Override
    public void reserveAbortConfirmed(ReserveAbortConfirmedRequest pbReserveAbortConfirmedRequest,
                                      StreamObserver<ReserveAbortConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveAbortConfirmed` to "
                    + pbReserveAbortConfirmedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveAbortConfirmed` call.", e);
        }
    }

    @Override
    public void reserveCommitConfirmed(ReserveCommitConfirmedRequest pbReserveCommitConfirmedRequest,
                                       StreamObserver<ReserveCommitConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveCommitConfirmed` to "
                    + pbReserveCommitConfirmedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveCommitConfirmed` call.", e);
        }
    }

    @Override
    public void reserveCommitFailed(ReserveCommitFailedRequest pbReserveCommitFailedRequest,
                                    StreamObserver<ReserveCommitFailedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveCommitFailed` to "
                    + pbReserveCommitFailedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveCommitFailed` call.", e);
        }
    }

    @Override
    public void error(ErrorRequest pbErrorRequest,
        io.grpc.stub.StreamObserver<ErrorResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `error` to "
                    + pbErrorRequest.getHeader().getReplyTo());
            LOG.info("Received protobuf message `error`:\n" + pbErrorRequest.toString());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `error` call.", e);
        }
    }

    @Override
    public void provisionConfirmed(ProvisionConfirmedRequest pbProvisionConfirmedRequest,
                                       StreamObserver<ProvisionConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `ProvisionConfirmed` to "
                    + pbProvisionConfirmedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `ProvisionConfirmed` call.", e);
        }
    }

    @Override
    public void releaseConfirmed(ReleaseConfirmedRequest pbReleaseConfirmedRequest,
                                       StreamObserver<ReleaseConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `ReleaseConfirmed` to "
                    + pbReleaseConfirmedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `ReleaseConfirmed` call.", e);
        }
    }


    @Override
    public void terminateConfirmed(TerminateConfirmedRequest pbTerminateConfirmedRequest,
                                       StreamObserver<TerminateConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `TerminateConfirmed` to "
                    + pbTerminateConfirmedRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `TerminateConfirmed` call.", e);
        }
    }

    @Override
    public void dataPlaneStateChange(DataPlaneStateChangeRequest pbDataPlaneStateChangeRequest,
                                    StreamObserver<DataPlaneStateChangeResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `dataPlaneStateChange` to "
                    + pbDataPlaneStateChangeRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `datePlaneStateChange` call.", e);
        }
    }

    @Override
    public void reserveTimeout(ReserveTimeoutRequest pbReserveTimeoutRequest,
                                    StreamObserver<ReserveTimeoutResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `reserveTimeout` to "
                    + pbReserveTimeoutRequest.getHeader().getReplyTo());
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
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `reserveTimeout` call.", e);
        }
    }

    public static List<QuerySummaryResultType> soapQuerySummaryConfirmed(org.ogf.nsi.grpc.connection.requester.QuerySummaryConfirmedRequest pbQuerySummaryConfirmedRequest) throws ConverterException {

        // For some reason the cxf-codegen-plugin wsdl2java does not generate the setCriteria method.
        class myQuerySummaryResultType extends QuerySummaryResultType
        {
            public void setCriteria(List<QuerySummaryResultCriteriaType> value) {
                this.criteria = value;
            }
        }

        ObjectFactory objectFactory = new ObjectFactory();
        List<QuerySummaryResultType> soapReservations = new ArrayList<>();
        for (QuerySummaryResult pbReservation : pbQuerySummaryConfirmedRequest.getReservationList()) {
            // QuerySummaryResultType soapReservation = objectFactory.createQuerySummaryResultType();
            myQuerySummaryResultType soapReservation = new myQuerySummaryResultType();
            soapReservation.setConnectionId(pbReservation.getConnectionId());
            soapReservation.setRequesterNSA(pbReservation.getRequesterNsa());
            soapReservation.setConnectionStates(toSoap(pbReservation.getConnectionStates()));
            if (pbReservation.getGlobalReservationId().length() > 0) {
                soapReservation.setGlobalReservationId(pbReservation.getGlobalReservationId());
            }
            soapReservation.setDescription(pbReservation.getDescription());
            QuerySummaryResultCriteriaType soapQuerySummaryResultCriteriaType = objectFactory
                    .createQuerySummaryResultCriteriaType();
            // NOTE: a ReservationConfirmCriteria message is used instead of QuerySummaryResultCriteria
            //       as we only support uPA's, and uPA's do not have child reservations
            // TODO: when Modify Reservation is implemented, add all criteria
            ReservationConfirmCriteria pbCriteria = pbReservation.getCriteria(0);
            soapQuerySummaryResultCriteriaType.setVersion(pbCriteria.getVersion());
            soapQuerySummaryResultCriteriaType.setSchedule(toSoap(pbCriteria.getSchedule()));
            soapQuerySummaryResultCriteriaType.setServiceType(pbCriteria.getServiceType());
            nl.surf.polynsi.soap.services.p2p.ObjectFactory servicesObjFactory =
                    new nl.surf.polynsi.soap.services.p2p.ObjectFactory();
            P2PServiceBaseType soapP2PServiceType = servicesObjFactory.createP2PServiceBaseType();
            PointToPointService pbPtps = pbCriteria.getPtps();
            Directionality pbDirectionality = pbPtps.getDirectionality();
            if (pbDirectionality == Directionality.UNI_DIRECTIONAL)
                soapP2PServiceType.setDirectionality(DirectionalityType.UNIDIRECTIONAL);
            else
                soapP2PServiceType.setDirectionality(DirectionalityType.BIDIRECTIONAL);
            soapP2PServiceType.setCapacity(pbPtps.getCapacity());
            soapP2PServiceType.setSymmetricPath(pbPtps.getSymmetricPath());
            soapP2PServiceType.setSourceSTP(pbPtps.getSourceStp());
            soapP2PServiceType.setDestSTP(pbPtps.getDestStp());
            soapQuerySummaryResultCriteriaType.getAny().add(servicesObjFactory.createP2Ps(soapP2PServiceType));
            List<QuerySummaryResultCriteriaType> soapCriteria = new ArrayList<>();
            soapCriteria.add(soapQuerySummaryResultCriteriaType);
            soapReservation.setCriteria(soapCriteria);
            soapReservations.add(soapReservation);
        }
        return soapReservations;
    }

    @Override
    public void querySummaryConfirmed(org.ogf.nsi.grpc.connection.requester.QuerySummaryConfirmedRequest pbQuerySummaryConfirmedRequest,
                                      StreamObserver<QuerySummaryConfirmedResponse> responseObserver) {
        try {
            LOG.info("Executing gRPC service `querySummaryConfirmed` to "
                    + pbQuerySummaryConfirmedRequest.getHeader().getReplyTo());
            QuerySummaryConfirmedResponse pbQuerySummaryConfirmedResponse = QuerySummaryConfirmedResponse.newBuilder()
                    .setHeader(pbQuerySummaryConfirmedRequest.getHeader()).build();

            List<QuerySummaryResultType> soapReservations = soapQuerySummaryConfirmed(pbQuerySummaryConfirmedRequest);

            OffsetDateTime lastModified = null;
            if (!pbQuerySummaryConfirmedRequest.getLastModified().equals(EPOCH)) {
                lastModified = toSoap(pbQuerySummaryConfirmedRequest.getLastModified());
            }

            Holder<CommonHeaderType> soapHeaderHolder = new Holder<>();
            soapHeaderHolder.value = toSoap(pbQuerySummaryConfirmedRequest.getHeader());

            ConnectionRequesterPort connectionRequesterProxy =
                    connectionRequesterProxy(pbQuerySummaryConfirmedRequest.getHeader().getReplyTo());
            connectionRequesterProxy.querySummaryConfirmed(soapReservations, lastModified, soapHeaderHolder);

            responseObserver.onNext(pbQuerySummaryConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handing `querySummaryConfirmed` call.", e);
        }
    }
}
