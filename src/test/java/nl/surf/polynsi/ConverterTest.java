package nl.surf.polynsi;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import nl.surf.polynsi.soap.connection.types.*;
import nl.surf.polynsi.soap.framework.headers.SessionSecurityAttrType;
import nl.surf.polynsi.soap.framework.types.ServiceExceptionType;
import nl.surf.polynsi.soap.framework.types.TypeValuePairType;
import nl.surf.polynsi.soap.framework.types.VariablesType;
import nl.surf.polynsi.soap.services.p2p.P2PServiceBaseType;
import nl.surf.polynsi.soap.services.types.DirectionalityType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.oasis.saml.AttributeType;
import org.ogf.nsi.grpc.connection.common.*;
import org.ogf.nsi.grpc.connection.requester.*;
import org.ogf.nsi.grpc.services.Directionality;
import org.ogf.nsi.grpc.services.PointToPointService;

class ConverterTest {
    public static final String DT_WITH_TZ = "2013-07-24T16:50:00.333+02:00";
    public static final String DT_EPOCH = "1970-01-01T00:00:00Z";

    @Nested
    class TimestampConversions {
        @Test
        void toProtobufOffsetDatetime() {
            OffsetDateTime odtWithTz = OffsetDateTime.parse(DT_WITH_TZ, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Timestamp tsFromOdtWithTz = Converter.toProtobuf(odtWithTz);
            assertEquals(odtWithTz.atZoneSameInstant(ZoneOffset.UTC).toString(), Timestamps.toString(tsFromOdtWithTz));

            OffsetDateTime odtEpoch = OffsetDateTime.parse(DT_EPOCH, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Timestamp tsFromOdtEpoch = Converter.toProtobuf(odtEpoch);
            assertEquals(ZoneOffset.UTC, odtEpoch.getOffset());

            assertEquals(odtEpoch.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), Timestamps.toString(tsFromOdtEpoch));
        }

        @Test
        void toSoapTimestamp() throws ParseException {
            Timestamp tsFromDtWithTz = Timestamps.parse(DT_WITH_TZ);
            OffsetDateTime odtWithTz = OffsetDateTime.parse(DT_WITH_TZ, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            assertEquals(
                    odtWithTz.atZoneSameInstant(ZoneOffset.UTC).toString(),
                    Converter.toSoap(tsFromDtWithTz).toString());

            Timestamp tsFromDtEpoch = Timestamps.parse(DT_EPOCH);
            OffsetDateTime odtEpoch = OffsetDateTime.parse(DT_EPOCH, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            assertEquals(odtEpoch.toString(), Converter.toSoap(tsFromDtEpoch).toString());
        }

        @Test
        void roundTripTimestamp() {
            OffsetDateTime original = OffsetDateTime.parse("2024-06-15T10:30:00.123456789Z");
            Timestamp pbTs = Converter.toProtobuf(original);
            OffsetDateTime roundTripped = Converter.toSoap(pbTs);
            assertEquals(original.toInstant(), roundTripped.toInstant());
        }
    }

    @Nested
    class SessionSecurityAttrConversions {
        @Test
        void fromSessionSecurityAttr() {
            var xmlSSA = "<sessionSecurityAttr><ns4:Attribute Name=\"user\" xmlns:ns4=\"urn:oasis:names:tc:SAML:2"
                    + ".0:assertion\"><ns4:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xsi:type=\"xs:string\">urn:collab:person:surfnet"
                    + ".nl:hanst</ns4:AttributeValue></ns4:Attribute><ns4:Attribute Name=\"token\" "
                    + "xmlns:ns4=\"urn:oasis:names:tc:SAML:2.0:assertion\"><ns4:AttributeValue xmlns:xs=\"http://www.w3"
                    + ".org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xsi:type=\"xs:string\">2506fc3c-56fd-4efa-ba5b-01cb73bba316</ns4:AttributeValue></ns4:Attribute"
                    + "></sessionSecurityAttr>";
            assertDoesNotThrow(() -> {
                ArrayList<SessionSecurityAttrType> ssas = Converter.toSessionSecurityAttr(xmlSSA);
                assertEquals(1, ssas.size());
                assertAll("AttributeOrEncryptedAttribute", () -> {
                    List<AttributeType> attrTypes = ssas.get(0).getAttributeOrEncryptedAttribute().stream()
                            .map(e -> (AttributeType) e)
                            .collect(Collectors.toList());
                    assertAll(
                            "AttributeTypes",
                            () -> assertEquals("user", attrTypes.get(0).getName()),
                            () -> assertEquals("token", attrTypes.get(1).getName()));
                });
            });
        }

        @Test
        void toSessionSecurityAttrWithInvalidXmlThrows() {
            assertThrows(ConverterException.class, () -> Converter.toSessionSecurityAttr("<invalid><<<"));
        }
    }

    @Nested
    class ScheduleConversions {
        @Test
        void toProtobufNullScheduleReturnsDefault() {
            Schedule result = Converter.toProtobuf((ScheduleType) null);
            assertEquals(Schedule.getDefaultInstance(), result);
        }

        @Test
        void toProtobufScheduleWithStartAndEndTime() {
            OffsetDateTime start = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            OffsetDateTime end = OffsetDateTime.parse("2024-12-31T23:59:59Z");

            ScheduleType soapSchedule = new ScheduleType();
            soapSchedule.setStartTime(start);
            soapSchedule.setEndTime(end);

            Schedule pbSchedule = Converter.toProtobuf(soapSchedule);

            assertEquals(
                    start.toInstant().getEpochSecond(),
                    pbSchedule.getStartTime().getSeconds());
            assertEquals(
                    end.toInstant().getEpochSecond(), pbSchedule.getEndTime().getSeconds());
        }

        @Test
        void toProtobufScheduleWithOnlyStartTime() {
            OffsetDateTime start = OffsetDateTime.parse("2024-06-15T10:00:00Z");

            ScheduleType soapSchedule = new ScheduleType();
            soapSchedule.setStartTime(start);

            Schedule pbSchedule = Converter.toProtobuf(soapSchedule);

            assertEquals(
                    start.toInstant().getEpochSecond(),
                    pbSchedule.getStartTime().getSeconds());
            assertEquals(Timestamp.getDefaultInstance(), pbSchedule.getEndTime());
        }

        @Test
        void toSoapScheduleWithStartAndEndTime() {
            Timestamp pbStart = Timestamp.newBuilder().setSeconds(1704067200).build();
            Timestamp pbEnd = Timestamp.newBuilder().setSeconds(1735689599).build();
            Schedule pbSchedule = Schedule.newBuilder()
                    .setStartTime(pbStart)
                    .setEndTime(pbEnd)
                    .build();

            ScheduleType soapSchedule = Converter.toSoap(pbSchedule);

            assertNotNull(soapSchedule);
            assertEquals(1704067200, soapSchedule.getStartTime().toInstant().getEpochSecond());
            assertEquals(1735689599, soapSchedule.getEndTime().toInstant().getEpochSecond());
        }
    }

    @Nested
    class ReservationStateConversions {
        @Test
        void allReservationStatesToSoap() throws ConverterException {
            assertEquals(ReservationStateEnumType.RESERVE_START, Converter.toSoap(ReservationState.RESERVE_START));
            assertEquals(
                    ReservationStateEnumType.RESERVE_CHECKING, Converter.toSoap(ReservationState.RESERVE_CHECKING));
            assertEquals(ReservationStateEnumType.RESERVE_FAILED, Converter.toSoap(ReservationState.RESERVE_FAILED));
            assertEquals(
                    ReservationStateEnumType.RESERVE_ABORTING, Converter.toSoap(ReservationState.RESERVE_ABORTING));
            assertEquals(ReservationStateEnumType.RESERVE_HELD, Converter.toSoap(ReservationState.RESERVE_HELD));
            assertEquals(
                    ReservationStateEnumType.RESERVE_COMMITTING, Converter.toSoap(ReservationState.RESERVE_COMMITTING));
            assertEquals(ReservationStateEnumType.RESERVE_TIMEOUT, Converter.toSoap(ReservationState.RESERVE_TIMEOUT));
        }

        @Test
        void unrecognizedReservationStateThrows() {
            assertThrows(ConverterException.class, () -> Converter.toSoap(ReservationState.UNRECOGNIZED));
        }
    }

    @Nested
    class ProvisionStateConversions {
        @Test
        void allProvisionStatesToSoap() throws ConverterException {
            assertEquals(ProvisionStateEnumType.RELEASED, Converter.toSoap(ProvisionState.RELEASED));
            assertEquals(ProvisionStateEnumType.PROVISIONING, Converter.toSoap(ProvisionState.PROVISIONING));
            assertEquals(ProvisionStateEnumType.PROVISIONED, Converter.toSoap(ProvisionState.PROVISIONED));
            assertEquals(ProvisionStateEnumType.RELEASING, Converter.toSoap(ProvisionState.RELEASING));
        }

        @Test
        void unrecognizedProvisionStateThrows() {
            assertThrows(ConverterException.class, () -> Converter.toSoap(ProvisionState.UNRECOGNIZED));
        }
    }

    @Nested
    class LifecycleStateConversions {
        @Test
        void allLifecycleStatesToSoap() throws ConverterException {
            assertEquals(LifecycleStateEnumType.CREATED, Converter.toSoap(LifecycleState.CREATED));
            assertEquals(LifecycleStateEnumType.FAILED, Converter.toSoap(LifecycleState.FAILED));
            assertEquals(LifecycleStateEnumType.PASSED_END_TIME, Converter.toSoap(LifecycleState.PASSED_END_TIME));
            assertEquals(LifecycleStateEnumType.TERMINATING, Converter.toSoap(LifecycleState.TERMINATING));
            assertEquals(LifecycleStateEnumType.TERMINATED, Converter.toSoap(LifecycleState.TERMINATED));
        }

        @Test
        void unrecognizedLifecycleStateThrows() {
            assertThrows(ConverterException.class, () -> Converter.toSoap(LifecycleState.UNRECOGNIZED));
        }
    }

    @Nested
    class EventTypeConversions {
        @Test
        void allEventTypesToSoap() throws ConverterException {
            assertEquals(EventEnumType.ACTIVATE_FAILED, Converter.toSoap(EventType.ACTIVATE_FAILED));
            assertEquals(EventEnumType.DEACTIVATE_FAILED, Converter.toSoap(EventType.DEACTIVATE_FAILED));
            assertEquals(EventEnumType.DATAPLANE_ERROR, Converter.toSoap(EventType.DATAPLANE_ERROR));
            assertEquals(EventEnumType.FORCED_END, Converter.toSoap(EventType.FORCED_END));
        }

        @Test
        void unrecognizedEventTypeThrows() {
            assertThrows(ConverterException.class, () -> Converter.toSoap(EventType.UNRECOGNIZED));
        }
    }

    @Nested
    class DataPlaneStatusConversions {
        @Test
        void toSoapDataPlaneStatus() {
            DataPlaneStatus pbStatus = DataPlaneStatus.newBuilder()
                    .setActive(true)
                    .setVersion(3)
                    .setVersionConsistent(true)
                    .build();

            DataPlaneStatusType soapStatus = Converter.toSoap(pbStatus);

            assertTrue(soapStatus.isActive());
            assertEquals(3, soapStatus.getVersion());
            assertTrue(soapStatus.isVersionConsistent());
        }

        @Test
        void toSoapDataPlaneStatusInactive() {
            DataPlaneStatus pbStatus = DataPlaneStatus.newBuilder()
                    .setActive(false)
                    .setVersion(1)
                    .setVersionConsistent(false)
                    .build();

            DataPlaneStatusType soapStatus = Converter.toSoap(pbStatus);

            assertFalse(soapStatus.isActive());
            assertEquals(1, soapStatus.getVersion());
            assertFalse(soapStatus.isVersionConsistent());
        }
    }

    @Nested
    class ConnectionStatesConversions {
        @Test
        void toSoapConnectionStates() throws ConverterException {
            ConnectionStates pbStates = ConnectionStates.newBuilder()
                    .setReservationState(ReservationState.RESERVE_HELD)
                    .setProvisionState(ProvisionState.PROVISIONED)
                    .setLifecycleState(LifecycleState.CREATED)
                    .setDataPlaneStatus(DataPlaneStatus.newBuilder()
                            .setActive(true)
                            .setVersion(1)
                            .setVersionConsistent(true)
                            .build())
                    .build();

            ConnectionStatesType soapStates = Converter.toSoap(pbStates);

            assertNotNull(soapStates);
            assertEquals(ReservationStateEnumType.RESERVE_HELD, soapStates.getReservationState());
            assertEquals(ProvisionStateEnumType.PROVISIONED, soapStates.getProvisionState());
            assertEquals(LifecycleStateEnumType.CREATED, soapStates.getLifecycleState());
            assertTrue(soapStates.getDataPlaneStatus().isActive());
        }
    }

    @Nested
    class VariablesConversions {
        @Test
        void toSoapVariables() {
            List<TypeValuePair> pbVariables = List.of(
                    TypeValuePair.newBuilder()
                            .setType("type1")
                            .setNamespace("ns1")
                            .setValue("value1")
                            .build(),
                    TypeValuePair.newBuilder()
                            .setType("type2")
                            .setNamespace("ns2")
                            .setValue("value2")
                            .build());

            VariablesType soapVariables = Converter.toSoap(pbVariables);

            assertEquals(2, soapVariables.getVariable().size());
            TypeValuePairType first = soapVariables.getVariable().get(0);
            assertEquals("type1", first.getType());
            assertEquals("ns1", first.getNamespace());
            assertEquals("value1", first.getValue().get(0));

            TypeValuePairType second = soapVariables.getVariable().get(1);
            assertEquals("type2", second.getType());
            assertEquals("ns2", second.getNamespace());
            assertEquals("value2", second.getValue().get(0));
        }

        @Test
        void toSoapEmptyVariables() {
            VariablesType soapVariables = Converter.toSoap(List.of());
            assertNotNull(soapVariables);
            assertTrue(soapVariables.getVariable().isEmpty());
        }
    }

    @Nested
    class ServiceExceptionConversions {
        @Test
        void toSoapServiceException() {
            ServiceException pbException = ServiceException.newBuilder()
                    .setNsaId("nsa-id-1")
                    .setConnectionId("conn-123")
                    .setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE")
                    .setErrorId("00100")
                    .setText("Something went wrong")
                    .build();

            ServiceExceptionType soapException = Converter.toSoap(pbException);

            assertEquals("nsa-id-1", soapException.getNsaId());
            assertEquals("conn-123", soapException.getConnectionId());
            assertEquals(
                    "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE", soapException.getServiceType());
            assertEquals("00100", soapException.getErrorId());
            assertEquals("Something went wrong", soapException.getText());
        }

        @Test
        void toSoapServiceExceptionWithVariables() {
            ServiceException pbException = ServiceException.newBuilder()
                    .setNsaId("nsa-id-1")
                    .setConnectionId("conn-123")
                    .setServiceType("serviceType")
                    .setErrorId("00200")
                    .setText("error text")
                    .addVariables(TypeValuePair.newBuilder()
                            .setType("varType")
                            .setNamespace("varNs")
                            .setValue("varValue")
                            .build())
                    .build();

            ServiceExceptionType soapException = Converter.toSoap(pbException);

            assertNotNull(soapException.getVariables());
            assertEquals(1, soapException.getVariables().getVariable().size());
            assertEquals(
                    "varType", soapException.getVariables().getVariable().get(0).getType());
        }

        @Test
        void toSoapServiceExceptionWithChildExceptions() {
            ServiceException childException = ServiceException.newBuilder()
                    .setNsaId("child-nsa")
                    .setConnectionId("child-conn")
                    .setServiceType("child-service")
                    .setErrorId("00300")
                    .setText("Child error")
                    .build();

            ServiceException pbException = ServiceException.newBuilder()
                    .setNsaId("parent-nsa")
                    .setConnectionId("parent-conn")
                    .setServiceType("parent-service")
                    .setErrorId("00100")
                    .setText("Parent error")
                    .addChildException(childException)
                    .build();

            ServiceExceptionType soapException = Converter.toSoap(pbException);

            assertEquals(1, soapException.getChildException().size());
            ServiceExceptionType soapChild = soapException.getChildException().get(0);
            assertEquals("child-nsa", soapChild.getNsaId());
            assertEquals("child-conn", soapChild.getConnectionId());
            assertEquals("Child error", soapChild.getText());
        }
    }

    @Nested
    class PointToPointServiceConversions {
        @Test
        void toSoapBidirectional() {
            PointToPointService pbPtps = PointToPointService.newBuilder()
                    .setDirectionality(Directionality.BI_DIRECTIONAL)
                    .setCapacity(1000)
                    .setSymmetricPath(true)
                    .setSourceStp("urn:ogf:network:example.net:2024:source")
                    .setDestStp("urn:ogf:network:example.net:2024:dest")
                    .build();

            P2PServiceBaseType soapPtps = Converter.toSoap(pbPtps).getValue();

            assertEquals(DirectionalityType.BIDIRECTIONAL, soapPtps.getDirectionality());
            assertEquals(1000, soapPtps.getCapacity());
            assertTrue(soapPtps.isSymmetricPath());
            assertEquals("urn:ogf:network:example.net:2024:source", soapPtps.getSourceSTP());
            assertEquals("urn:ogf:network:example.net:2024:dest", soapPtps.getDestSTP());
        }

        @Test
        void toSoapUnidirectional() {
            PointToPointService pbPtps = PointToPointService.newBuilder()
                    .setDirectionality(Directionality.UNI_DIRECTIONAL)
                    .setCapacity(500)
                    .setSymmetricPath(false)
                    .setSourceStp("source-stp")
                    .setDestStp("dest-stp")
                    .build();

            P2PServiceBaseType soapPtps = Converter.toSoap(pbPtps).getValue();

            assertEquals(DirectionalityType.UNIDIRECTIONAL, soapPtps.getDirectionality());
            assertEquals(500, soapPtps.getCapacity());
            assertFalse(soapPtps.isSymmetricPath());
        }
    }

    @Nested
    class GenericConversions {
        @Test
        void toSoapGenericConfirmed() {
            GenericConfirmedRequest pbConfirmed = GenericConfirmedRequest.newBuilder()
                    .setConnectionId("conn-abc-123")
                    .build();

            GenericConfirmedType soapConfirmed = Converter.toSoap(pbConfirmed);

            assertEquals("conn-abc-123", soapConfirmed.getConnectionId());
        }

        @Test
        void toSoapGenericFailed() throws ConverterException {
            GenericFailedRequest pbFailed = GenericFailedRequest.newBuilder()
                    .setConnectionId("conn-fail-456")
                    .setConnectionStates(ConnectionStates.newBuilder()
                            .setReservationState(ReservationState.RESERVE_FAILED)
                            .setProvisionState(ProvisionState.RELEASED)
                            .setLifecycleState(LifecycleState.CREATED)
                            .setDataPlaneStatus(DataPlaneStatus.newBuilder()
                                    .setActive(false)
                                    .setVersion(0)
                                    .setVersionConsistent(true)
                                    .build())
                            .build())
                    .setServiceException(ServiceException.newBuilder()
                            .setNsaId("nsa-1")
                            .setConnectionId("conn-fail-456")
                            .setServiceType("service")
                            .setErrorId("00100")
                            .setText("Failed")
                            .build())
                    .build();

            GenericFailedType soapFailed = Converter.toSoap(pbFailed);

            assertEquals("conn-fail-456", soapFailed.getConnectionId());
            assertEquals(
                    ReservationStateEnumType.RESERVE_FAILED,
                    soapFailed.getConnectionStates().getReservationState());
            assertEquals("00100", soapFailed.getServiceException().getErrorId());
        }

        @Test
        void toSoapGenericError() {
            ErrorRequest pbError = ErrorRequest.newBuilder()
                    .setServiceException(ServiceException.newBuilder()
                            .setNsaId("nsa-err")
                            .setConnectionId("conn-err")
                            .setServiceType("service")
                            .setErrorId("00500")
                            .setText("Internal error")
                            .build())
                    .build();

            GenericErrorType soapError = Converter.toSoap(pbError);

            assertEquals("00500", soapError.getServiceException().getErrorId());
            assertEquals("Internal error", soapError.getServiceException().getText());
        }
    }

    @Nested
    class ReserveConfirmedConversions {
        @Test
        void toSoapReserveConfirmed() {
            ReserveConfirmedRequest pbReserveConfirmed = ReserveConfirmedRequest.newBuilder()
                    .setConnectionId("conn-reserve-1")
                    .setGlobalReservationId("global-1")
                    .setDescription("Test reservation")
                    .setCriteria(ReservationConfirmCriteria.newBuilder()
                            .setVersion(1)
                            .setSchedule(Schedule.newBuilder()
                                    .setStartTime(Timestamp.newBuilder()
                                            .setSeconds(1704067200)
                                            .build())
                                    .build())
                            .setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE")
                            .setPtps(PointToPointService.newBuilder()
                                    .setDirectionality(Directionality.BI_DIRECTIONAL)
                                    .setCapacity(1000)
                                    .setSourceStp("source")
                                    .setDestStp("dest")
                                    .build())
                            .build())
                    .build();

            ReserveConfirmedType soapReserveConfirmed = Converter.toSoap(pbReserveConfirmed);

            assertEquals("conn-reserve-1", soapReserveConfirmed.getConnectionId());
            assertEquals("global-1", soapReserveConfirmed.getGlobalReservationId());
            assertEquals("Test reservation", soapReserveConfirmed.getDescription());
            assertNotNull(soapReserveConfirmed.getCriteria());
            assertEquals(1, soapReserveConfirmed.getCriteria().getVersion());
        }

        @Test
        void toSoapReserveConfirmedWithoutOptionals() {
            ReserveConfirmedRequest pbReserveConfirmed = ReserveConfirmedRequest.newBuilder()
                    .setConnectionId("conn-reserve-2")
                    .setCriteria(ReservationConfirmCriteria.newBuilder()
                            .setVersion(2)
                            .setServiceType("serviceType")
                            .setPtps(PointToPointService.newBuilder()
                                    .setDirectionality(Directionality.BI_DIRECTIONAL)
                                    .setCapacity(100)
                                    .setSourceStp("src")
                                    .setDestStp("dst")
                                    .build())
                            .build())
                    .build();

            ReserveConfirmedType soapReserveConfirmed = Converter.toSoap(pbReserveConfirmed);

            assertEquals("conn-reserve-2", soapReserveConfirmed.getConnectionId());
            assertNull(soapReserveConfirmed.getGlobalReservationId());
            assertNull(soapReserveConfirmed.getDescription());
        }
    }

    @Nested
    class ReservationConfirmCriteriaConversions {
        @Test
        void toSoapReservationConfirmCriteria() {
            ReservationConfirmCriteria pbCriteria = ReservationConfirmCriteria.newBuilder()
                    .setVersion(5)
                    .setSchedule(Schedule.newBuilder()
                            .setStartTime(
                                    Timestamp.newBuilder().setSeconds(1000).build())
                            .setEndTime(Timestamp.newBuilder().setSeconds(2000).build())
                            .build())
                    .setServiceType("testServiceType")
                    .setPtps(PointToPointService.newBuilder()
                            .setDirectionality(Directionality.UNI_DIRECTIONAL)
                            .setCapacity(200)
                            .setSourceStp("src-stp")
                            .setDestStp("dst-stp")
                            .build())
                    .build();

            ReservationConfirmCriteriaType soapCriteria = Converter.toSoap(pbCriteria);

            assertEquals(5, soapCriteria.getVersion());
            assertEquals("testServiceType", soapCriteria.getServiceType());
            assertNotNull(soapCriteria.getSchedule());
            assertFalse(soapCriteria.getAny().isEmpty());
        }
    }

    @Nested
    class NotificationConversions {
        private Notification createNotification(String connectionId, int notificationId, long timestampSeconds) {
            return Notification.newBuilder()
                    .setConnectionId(connectionId)
                    .setNotificationId(notificationId)
                    .setTimeStamp(
                            Timestamp.newBuilder().setSeconds(timestampSeconds).build())
                    .build();
        }

        @Test
        void toSoapReserveTimeout() {
            ReserveTimeoutRequest pbReserveTimeout = ReserveTimeoutRequest.newBuilder()
                    .setNotification(createNotification("conn-1", 42, 1704067200))
                    .setTimeoutValue(300)
                    .setOriginatingConnectionId("orig-conn-1")
                    .setOriginatingNsa("orig-nsa-1")
                    .build();

            ReserveTimeoutRequestType soapReserveTimeout = Converter.toSoap(pbReserveTimeout);

            assertEquals("conn-1", soapReserveTimeout.getConnectionId());
            assertEquals(42, soapReserveTimeout.getNotificationId());
            assertEquals(300, soapReserveTimeout.getTimeoutValue());
            assertEquals("orig-conn-1", soapReserveTimeout.getOriginatingConnectionId());
            assertEquals("orig-nsa-1", soapReserveTimeout.getOriginatingNSA());
            assertNotNull(soapReserveTimeout.getTimeStamp());
        }

        @Test
        void toSoapDataPlaneStateChange() {
            DataPlaneStateChangeRequest pbStateChange = DataPlaneStateChangeRequest.newBuilder()
                    .setNotification(createNotification("conn-2", 10, 1704067200))
                    .setDataPlaneStatus(DataPlaneStatus.newBuilder()
                            .setActive(true)
                            .setVersion(2)
                            .setVersionConsistent(true)
                            .build())
                    .build();

            DataPlaneStateChangeRequestType soapStateChange = Converter.toSoap(pbStateChange);

            assertEquals("conn-2", soapStateChange.getConnectionId());
            assertEquals(10, soapStateChange.getNotificationId());
            assertTrue(soapStateChange.getDataPlaneStatus().isActive());
            assertEquals(2, soapStateChange.getDataPlaneStatus().getVersion());
        }

        @Test
        void toSoapMessageDeliveryTimeout() {
            MessageDeliveryTimeoutRequest pbTimeout = MessageDeliveryTimeoutRequest.newBuilder()
                    .setNotification(createNotification("conn-3", 7, 1704067200))
                    .setCorrelationId("corr-123")
                    .build();

            MessageDeliveryTimeoutRequestType soapTimeout = Converter.toSoap(pbTimeout);

            assertEquals("conn-3", soapTimeout.getConnectionId());
            assertEquals(7, soapTimeout.getNotificationId());
            assertEquals("corr-123", soapTimeout.getCorrelationId());
        }

        @Test
        void toSoapErrorEvent() throws ConverterException {
            ErrorEventRequest pbErrorEvent = ErrorEventRequest.newBuilder()
                    .setNotification(createNotification("conn-4", 99, 1704067200))
                    .setEvent(EventType.FORCED_END)
                    .setOriginatingConnectionId("orig-conn-4")
                    .setOriginatingNsa("orig-nsa-4")
                    .setServiceException(ServiceException.newBuilder()
                            .setNsaId("nsa-err")
                            .setConnectionId("conn-4")
                            .setServiceType("service")
                            .setErrorId("00100")
                            .setText("Forced end error")
                            .build())
                    .build();

            ErrorEventType soapErrorEvent = Converter.toSoap(pbErrorEvent);

            assertEquals("conn-4", soapErrorEvent.getConnectionId());
            assertEquals(99, soapErrorEvent.getNotificationId());
            assertEquals(EventEnumType.FORCED_END, soapErrorEvent.getEvent());
            assertEquals("orig-conn-4", soapErrorEvent.getOriginatingConnectionId());
            assertEquals("orig-nsa-4", soapErrorEvent.getOriginatingNSA());
            assertEquals(
                    "Forced end error", soapErrorEvent.getServiceException().getText());
        }
    }

    @Nested
    class QuerySummaryResultConversions {
        @Test
        void toSoapQuerySummaryResults() throws ConverterException {
            QueryConfirmedRequest pbQueryConfirmed = QueryConfirmedRequest.newBuilder()
                    .addReservation(QueryResult.newBuilder()
                            .setConnectionId("conn-query-1")
                            .setRequesterNsa("requester-nsa")
                            .setGlobalReservationId("global-1")
                            .setDescription("Query result description")
                            .setConnectionStates(ConnectionStates.newBuilder()
                                    .setReservationState(ReservationState.RESERVE_HELD)
                                    .setProvisionState(ProvisionState.RELEASED)
                                    .setLifecycleState(LifecycleState.CREATED)
                                    .setDataPlaneStatus(DataPlaneStatus.newBuilder()
                                            .setActive(false)
                                            .setVersion(0)
                                            .setVersionConsistent(true)
                                            .build())
                                    .build())
                            .addCriteria(QueryResultCriteria.newBuilder()
                                    .setVersion(1)
                                    .setServiceType("testService")
                                    .setPtps(PointToPointService.newBuilder()
                                            .setDirectionality(Directionality.BI_DIRECTIONAL)
                                            .setCapacity(1000)
                                            .setSourceStp("src")
                                            .setDestStp("dst")
                                            .build())
                                    .build())
                            .build())
                    .build();

            List<QuerySummaryResultType> results = Converter.toSoap(pbQueryConfirmed);

            assertEquals(1, results.size());
            QuerySummaryResultType result = results.get(0);
            assertEquals("conn-query-1", result.getConnectionId());
            assertEquals("requester-nsa", result.getRequesterNSA());
            assertEquals("global-1", result.getGlobalReservationId());
            assertEquals("Query result description", result.getDescription());
            assertEquals(
                    ReservationStateEnumType.RESERVE_HELD,
                    result.getConnectionStates().getReservationState());
            assertEquals(1, result.getCriteria().size());
        }

        @Test
        void toSoapQuerySummaryResultsWithoutOptionals() throws ConverterException {
            QueryConfirmedRequest pbQueryConfirmed = QueryConfirmedRequest.newBuilder()
                    .addReservation(QueryResult.newBuilder()
                            .setConnectionId("conn-query-2")
                            .setRequesterNsa("requester-nsa-2")
                            .setConnectionStates(ConnectionStates.newBuilder()
                                    .setReservationState(ReservationState.RESERVE_START)
                                    .setProvisionState(ProvisionState.RELEASED)
                                    .setLifecycleState(LifecycleState.CREATED)
                                    .setDataPlaneStatus(
                                            DataPlaneStatus.newBuilder().build())
                                    .build())
                            .build())
                    .build();

            List<QuerySummaryResultType> results = Converter.toSoap(pbQueryConfirmed);

            assertEquals(1, results.size());
            QuerySummaryResultType result = results.get(0);
            assertEquals("conn-query-2", result.getConnectionId());
            assertNull(result.getGlobalReservationId());
            assertNull(result.getDescription());
        }

        @Test
        void toSoapMultipleQuerySummaryResults() throws ConverterException {
            QueryConfirmedRequest pbQueryConfirmed = QueryConfirmedRequest.newBuilder()
                    .addReservation(QueryResult.newBuilder()
                            .setConnectionId("conn-1")
                            .setRequesterNsa("nsa-1")
                            .setConnectionStates(ConnectionStates.newBuilder()
                                    .setReservationState(ReservationState.RESERVE_HELD)
                                    .setProvisionState(ProvisionState.RELEASED)
                                    .setLifecycleState(LifecycleState.CREATED)
                                    .setDataPlaneStatus(
                                            DataPlaneStatus.newBuilder().build())
                                    .build())
                            .build())
                    .addReservation(QueryResult.newBuilder()
                            .setConnectionId("conn-2")
                            .setRequesterNsa("nsa-2")
                            .setConnectionStates(ConnectionStates.newBuilder()
                                    .setReservationState(ReservationState.RESERVE_COMMITTING)
                                    .setProvisionState(ProvisionState.PROVISIONING)
                                    .setLifecycleState(LifecycleState.CREATED)
                                    .setDataPlaneStatus(
                                            DataPlaneStatus.newBuilder().build())
                                    .build())
                            .build())
                    .build();

            List<QuerySummaryResultType> results = Converter.toSoap(pbQueryConfirmed);

            assertEquals(2, results.size());
            assertEquals("conn-1", results.get(0).getConnectionId());
            assertEquals("conn-2", results.get(1).getConnectionId());
        }
    }

    @Nested
    class QueryRecursiveResultConversions {
        @Test
        void toSoapQueryRecursiveResults() throws ConverterException {
            QueryConfirmedRequest pbQueryConfirmed = QueryConfirmedRequest.newBuilder()
                    .addReservation(QueryResult.newBuilder()
                            .setConnectionId("conn-recursive-1")
                            .setRequesterNsa("requester-nsa")
                            .setConnectionStates(ConnectionStates.newBuilder()
                                    .setReservationState(ReservationState.RESERVE_HELD)
                                    .setProvisionState(ProvisionState.PROVISIONED)
                                    .setLifecycleState(LifecycleState.CREATED)
                                    .setDataPlaneStatus(DataPlaneStatus.newBuilder()
                                            .setActive(true)
                                            .setVersion(1)
                                            .setVersionConsistent(true)
                                            .build())
                                    .build())
                            .addCriteria(QueryResultCriteria.newBuilder()
                                    .setVersion(1)
                                    .setServiceType("testService")
                                    .setPtps(PointToPointService.newBuilder()
                                            .setDirectionality(Directionality.BI_DIRECTIONAL)
                                            .setCapacity(500)
                                            .setSourceStp("src")
                                            .setDestStp("dst")
                                            .build())
                                    .build())
                            .build())
                    .build();

            List<QueryRecursiveResultType> results = Converter.toSoapQueryRecursiveResult(pbQueryConfirmed);

            assertEquals(1, results.size());
            QueryRecursiveResultType result = results.get(0);
            assertEquals("conn-recursive-1", result.getConnectionId());
            assertEquals("requester-nsa", result.getRequesterNSA());
            assertEquals(1, result.getCriteria().size());
        }
    }

    @Nested
    class QueryNotificationConfirmedConversions {
        @Test
        void toSoapQueryNotificationConfirmedWithMixedNotifications() throws ConverterException {
            QueryNotificationConfirmedRequest pbRequest = QueryNotificationConfirmedRequest.newBuilder()
                    .addReserveTimeout(ReserveTimeoutRequest.newBuilder()
                            .setNotification(Notification.newBuilder()
                                    .setConnectionId("conn-timeout")
                                    .setNotificationId(1)
                                    .setTimeStamp(Timestamp.newBuilder()
                                            .setSeconds(1704067200)
                                            .build())
                                    .build())
                            .setTimeoutValue(300)
                            .setOriginatingConnectionId("orig-conn")
                            .setOriginatingNsa("orig-nsa")
                            .build())
                    .addDataPlaneStateChange(DataPlaneStateChangeRequest.newBuilder()
                            .setNotification(Notification.newBuilder()
                                    .setConnectionId("conn-dp")
                                    .setNotificationId(2)
                                    .setTimeStamp(Timestamp.newBuilder()
                                            .setSeconds(1704067300)
                                            .build())
                                    .build())
                            .setDataPlaneStatus(DataPlaneStatus.newBuilder()
                                    .setActive(true)
                                    .setVersion(1)
                                    .setVersionConsistent(true)
                                    .build())
                            .build())
                    .addMessageDeliveryTimeout(MessageDeliveryTimeoutRequest.newBuilder()
                            .setNotification(Notification.newBuilder()
                                    .setConnectionId("conn-msg")
                                    .setNotificationId(3)
                                    .setTimeStamp(Timestamp.newBuilder()
                                            .setSeconds(1704067400)
                                            .build())
                                    .build())
                            .setCorrelationId("corr-1")
                            .build())
                    .build();

            QueryNotificationConfirmedType soapResult = Converter.toSoap(pbRequest);

            assertNotNull(soapResult);
            assertEquals(
                    3,
                    soapResult
                            .getErrorEventOrReserveTimeoutOrDataPlaneStateChange()
                            .size());
        }

        @Test
        void toSoapEmptyQueryNotificationConfirmed() throws ConverterException {
            QueryNotificationConfirmedRequest pbRequest =
                    QueryNotificationConfirmedRequest.newBuilder().build();

            QueryNotificationConfirmedType soapResult = Converter.toSoap(pbRequest);

            assertNotNull(soapResult);
            assertTrue(soapResult
                    .getErrorEventOrReserveTimeoutOrDataPlaneStateChange()
                    .isEmpty());
        }
    }

    @Nested
    class QueryResultConfirmedConversions {
        @Test
        void toSoapQueryResultConfirmedWithReserveConfirmed() throws ConverterException {
            QueryResultConfirmedRequest pbRequest = QueryResultConfirmedRequest.newBuilder()
                    .addResult(ResultResponse.newBuilder()
                            .setResultId(1)
                            .setCorrelationId("corr-1")
                            .setTimeStamp(Timestamp.newBuilder()
                                    .setSeconds(1704067200)
                                    .build())
                            .setReserveConfirmed(ReserveConfirmedRequest.newBuilder()
                                    .setConnectionId("conn-1")
                                    .setCriteria(ReservationConfirmCriteria.newBuilder()
                                            .setVersion(1)
                                            .setServiceType("service")
                                            .setPtps(PointToPointService.newBuilder()
                                                    .setDirectionality(Directionality.BI_DIRECTIONAL)
                                                    .setCapacity(100)
                                                    .setSourceStp("src")
                                                    .setDestStp("dst")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            List<QueryResultResponseType> results = Converter.toSoap(pbRequest);

            assertEquals(1, results.size());
            QueryResultResponseType result = results.get(0);
            assertEquals(1, result.getResultId());
            assertEquals("corr-1", result.getCorrelationId());
            assertNotNull(result.getReserveConfirmed());
            assertEquals("conn-1", result.getReserveConfirmed().getConnectionId());
        }

        @Test
        void toSoapQueryResultConfirmedWithGenericConfirmed() throws ConverterException {
            QueryResultConfirmedRequest pbRequest = QueryResultConfirmedRequest.newBuilder()
                    .addResult(ResultResponse.newBuilder()
                            .setResultId(2)
                            .setCorrelationId("corr-2")
                            .setTimeStamp(Timestamp.newBuilder()
                                    .setSeconds(1704067200)
                                    .build())
                            .setProvisionConfirmed(GenericConfirmedRequest.newBuilder()
                                    .setConnectionId("conn-prov")
                                    .build())
                            .build())
                    .addResult(ResultResponse.newBuilder()
                            .setResultId(3)
                            .setCorrelationId("corr-3")
                            .setTimeStamp(Timestamp.newBuilder()
                                    .setSeconds(1704067300)
                                    .build())
                            .setReleaseConfirmed(GenericConfirmedRequest.newBuilder()
                                    .setConnectionId("conn-rel")
                                    .build())
                            .build())
                    .addResult(ResultResponse.newBuilder()
                            .setResultId(4)
                            .setCorrelationId("corr-4")
                            .setTimeStamp(Timestamp.newBuilder()
                                    .setSeconds(1704067400)
                                    .build())
                            .setTerminateConfirmed(GenericConfirmedRequest.newBuilder()
                                    .setConnectionId("conn-term")
                                    .build())
                            .build())
                    .build();

            List<QueryResultResponseType> results = Converter.toSoap(pbRequest);

            assertEquals(3, results.size());
            assertNotNull(results.get(0).getProvisionConfirmed());
            assertNotNull(results.get(1).getReleaseConfirmed());
            assertNotNull(results.get(2).getTerminateConfirmed());
        }

        @Test
        void toSoapQueryResultConfirmedWithError() throws ConverterException {
            QueryResultConfirmedRequest pbRequest = QueryResultConfirmedRequest.newBuilder()
                    .addResult(ResultResponse.newBuilder()
                            .setResultId(5)
                            .setCorrelationId("corr-5")
                            .setTimeStamp(Timestamp.newBuilder()
                                    .setSeconds(1704067200)
                                    .build())
                            .setError(ErrorRequest.newBuilder()
                                    .setServiceException(ServiceException.newBuilder()
                                            .setNsaId("nsa")
                                            .setConnectionId("conn")
                                            .setServiceType("service")
                                            .setErrorId("00500")
                                            .setText("error text")
                                            .build())
                                    .build())
                            .build())
                    .build();

            List<QueryResultResponseType> results = Converter.toSoap(pbRequest);

            assertEquals(1, results.size());
            assertNotNull(results.get(0).getError());
            assertEquals(
                    "00500", results.get(0).getError().getServiceException().getErrorId());
        }
    }
}
