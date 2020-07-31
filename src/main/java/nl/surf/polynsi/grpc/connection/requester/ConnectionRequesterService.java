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
import org.ogf.nsi.grpc.connection.requester.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.ws.Holder;

import java.util.logging.Logger;

import static nl.surf.polynsi.Converter.toSoap;

@GrpcService
public class ConnectionRequesterService extends ConnectionRequesterGrpc.ConnectionRequesterImplBase {
    @Autowired
    ConnectionRequesterPort connectionRequesterPort;

    @Override
    public void reserveConfirmed(ReserveConfirmedRequest pbReserveConfirmedRequest,
                                 StreamObserver<ReserveConfirmedResponse> responseObserver) {
        try {
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
            connectionRequesterPort
                    .reserveConfirmed(pbReserveConfirmedRequest.getConnectionId(), pbReserveConfirmedRequest
                            .getGlobalReservationId(), pbReserveConfirmedRequest
                            .getGlobalReservationId(), soapReservationConfirmCriteria, soapHeaderHolder);

            responseObserver.onNext(pbReserveConfirmedResponse);
            responseObserver.onCompleted();
        } catch (ConverterException | ServiceException e) {
            throw new ProxyException(Direction.GRPC_TO_SOAP, "Error while handling 'reserveConfirmed' call.", e);
        }
    }
}
