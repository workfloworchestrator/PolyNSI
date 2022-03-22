package nl.surf.polynsi;

import com.google.protobuf.Timestamp;
import nl.surf.polynsi.soap.connection.provider.PathType;
import nl.surf.polynsi.soap.connection.requester.PathTraceType;
import nl.surf.polynsi.soap.connection.requester.SegmentType;
import nl.surf.polynsi.soap.connection.requester.StpType;
import nl.surf.polynsi.soap.connection.types.*;
import nl.surf.polynsi.soap.framework.headers.CommonHeaderType;
import nl.surf.polynsi.soap.framework.headers.SessionSecurityAttrType;
import nl.surf.polynsi.soap.framework.types.ServiceExceptionType;
import org.ogf.nsi.grpc.connection.common.*;
import org.ogf.nsi.grpc.policy.Path;
import org.ogf.nsi.grpc.policy.PathTrace;
import org.ogf.nsi.grpc.policy.Segment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;


/**
 * Class to convert between SOAP and Protobuf values.
 * <p>
 * {@code Converter} has mostly two types of static methods {@code toProtobuf} and {@code toSoap}. These method
 * are overloaded on the different types of values that need to be converted. Having these methods takes the mundane
 * conversion code out of the methods dealing with the SOAP and gRPC calls. Not that these call handling methods
 * need to do anything other but converting data, this is a translating proxy after all, but it should keep thing a bit
 * more readable and testable.
 */
public class Converter {
    /**
     * Convert SOAP timestamp to Protobuf timestamp.
     * <p>
     * There is some loss of information here. The SOAP timestamp includes a timezone offset that is lost in the
     * conversion to Protobuf. The latter being normalized to UTC. It stills point to the same moment in time, but
     * after conversion we cannot devise the original timezone offset anymore.
     *
     * @param soapTimestamp Timestamp used by SOAP (includes a timezone offset)
     * @return Timestamp used by Protobuf (does not include timezone offset, normalized to UTC)
     */
    public static Timestamp toProtobuf(OffsetDateTime soapTimestamp) {
        Instant instant = soapTimestamp.toInstant();
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    /**
     * Convert Protobuf timestamp to SOAP timestamp.
     * <p>
     * As the Protobuf timestamp does not have the concept of timezones or timezone offsets the timestamp we will be
     * converting to will have an offset of +00:00 (eg UTC).
     *
     * @param pbTimestamp Timestamp used by Protobuf
     * @return Timestamp used by SOAP (with offset set to +00:00, eg UTC)
     */
    public static OffsetDateTime toSoap(Timestamp pbTimestamp) {
        return OffsetDateTime
                .ofInstant(Instant.ofEpochSecond(pbTimestamp.getSeconds(), pbTimestamp.getNanos()), ZoneOffset.UTC);
    }

    /**
     * Get the SessionSecurityAttr of the SOAP Header as a string.
     * <p>
     * Apache CXF, by means of JAXB, will have unmarshalled the XML SOAP header into corresponding Java objects.
     * However that is more work done than needed. The SessionSecurityAttr should have remained XML (as a
     * String), as whatever system that's on PolyNSI's gRPC side will have to deal with SAML (=the contents of
     * SecuritySessionAttr) in its native format, namely XML. So here we reverse that: we marshall the SOAP
     * header back to XML and extract the SessionSecurityAttr from it. We have the work with the complete SOAP
     * header, instead of just SessionSecurityAttr for weird JAXB reasons.
     * <p>
     * NOTE: Maybe there is a better way to go about this. The current solution implies that for each SOAP
     * call the header is processed twice: XML->Java (Apache CXF) and Java->XML (this method). Maybe with Apache CXF
     * interceptors we might be able to do things more efficiently. Or we might be able to tinker with the JAXB
     * binding configuration to adjust the (un)marshalling. However both alternatives require more expertise in using
     * Apache CXF and JAXB than available. Fortunately PolyNSI is a very low traffic service that can easily deal with
     * this small inefficiency.
     *
     * @param soapHeader The header object to marshal to XML
     * @return a string representing the XML representation of the SessionSecurityAttr
     * @throws ConverterException if marshalling, xml->string transformation or xpath lookup fails.
     */
    protected static String fromSessionSecurityAttr(CommonHeaderType soapHeader) throws ConverterException {
        try {
            // Explicitly add classes to marshall. Include PathTraceType.class.
            JAXBContext ssaContext = JAXBContext.newInstance(CommonHeaderType.class, nl.surf.polynsi.soap.connection.provider.PathTraceType.class);
            Marshaller marshaller = ssaContext.createMarshaller();
            // Enable formatted output when debugging.
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            // Wrap it, as CommonHeaderType is not annotated with @XMLRootElement
            JAXBElement<CommonHeaderType> jaxbElemHeader = new JAXBElement<>(new QName("", "header"), CommonHeaderType.class, soapHeader);
            marshaller.marshal(jaxbElemHeader, sw);
            InputSource xmlHeader = new InputSource(new StringReader(sw.toString()));
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node nodeSSA = (Node) xPath.evaluate("//sessionSecurityAttr", xmlHeader, XPathConstants.NODE);
            StreamResult xmlSSA = new StreamResult(new StringWriter());
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(nodeSSA), xmlSSA);
            return xmlSSA.getWriter().toString();
        } catch (JAXBException | XPathExpressionException | TransformerException e) {
            throw new ConverterException("Could not convert 'sessionSecurityAttr' to a string.", e);
        }
    }

    /**
     * Get the sessionSecurityAttrs as an object from the raw XML string.
     * <p>
     * <b>IMPORTANT:</b> currently this method will try unmarshalling the {@code xmlSSA} string into a single
     * {@code SessionSecurityAttributeType}. The return type of this method is the same as that of the {@code
     * sessionSecurityAttr} in {@code CommonHeaderType}. This type, being an {@code ArrayList}, suggests that there
     * could be more than one SessionSecurityAttributeType encoded in the {@code xmlSSA}. But this method doesn't
     * support it yet.
     *
     * @param xmlSSA The string representing the sessionSecurityAttr as XML
     * @return The unmarshalled list of SessionSecurityAttributeType's
     * @throws ConverterException if the unmarshalling or XML parsing fails.
     */
    protected static ArrayList<SessionSecurityAttrType> toSessionSecurityAttr(String xmlSSA) throws ConverterException {
        try {
            JAXBContext jc = JAXBContext
                    .newInstance("nl.surf.polynsi.soap.framework.headers",
                            nl.surf.polynsi.soap.framework.headers.ObjectFactory.class
                            .getClassLoader());
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            /*
             TODO: CommonHeaderType.sessionSecurityAttribute is an ArrayList, meaning we might need to iterate
                   over certain XML elements and unmarshal them into separate SessionAttributeSecurityType's.
                   However we don't know what elements make up separate SessionAttributeSecurityType and what
                   elements belong to the same SessionAttributeSecurityType. We need SAML expertise for that. For
                   now we consider all the elements to belong to the same single SessionAttributeSecurityType.
             */
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(xmlSSA.getBytes()));
            JAXBElement<SessionSecurityAttrType> ssaElem = unmarshaller.unmarshal(doc, SessionSecurityAttrType.class);
            return new ArrayList<>() {
                {
                    add(ssaElem.getValue());
                }
            };
        } catch (JAXBException | ParserConfigurationException | SAXException | IOException e) {
            throw new ConverterException("Could not convert 'sessionSecurityAttr' to an object.", e);
        }
    }

    public static Header toProtobuf(CommonHeaderType soapHeader) throws ConverterException {
        try {
            Header.Builder pbHeaderBuilder = Header.newBuilder();
            pbHeaderBuilder.setProtocolVersion(soapHeader.getProtocolVersion())
                    .setCorrelationId(soapHeader.getCorrelationId()).setRequesterNsa(soapHeader.getRequesterNSA())
                    .setProviderNsa(soapHeader.getProviderNSA());
            /*
                Optional values in SOAP are set to `null` when not present. Protobuf however does not
                serialize `null` values. Instead if values are not present they simple should not be set. Hence
                the pattern of testing for `null` and conditionally setting Protobuf values.
             */
            if (soapHeader.getReplyTo() != null) {
                pbHeaderBuilder.setReplyTo(soapHeader.getReplyTo());
            }
            if (soapHeader.getSessionSecurityAttr() != null) {
                String ssa = fromSessionSecurityAttr(soapHeader);
                pbHeaderBuilder.setSessionSecurityAttributes(ssa);
            }

            // Process NSI extensions that are included in the SOAP header
            for (Object elem : soapHeader.getAny()) {
                if (elem instanceof Element) {
                    Element hdElem = (Element) elem;
                    if (hdElem.getLocalName().equals("pathTrace")) {
                        // dynamically create Java PathTraceType instance from raw XML
                        JAXBContext jc = JAXBContext
                                .newInstance("nl.surf.polynsi.soap.policies",
                                        nl.surf.polynsi.soap.connection.provider.ObjectFactory.class
                                        .getClassLoader());
                        JAXBElement<PathTraceType> root = jc.createUnmarshaller()
                                .unmarshal(hdElem.getOwnerDocument().getDocumentElement(), PathTraceType.class);
                        PathTraceType soapPathTrace = root.getValue();

                        // Build protobuf PathTrace message
                        PathTrace.Builder pbPathTraceBuilder = PathTrace.newBuilder().setId(soapPathTrace.getId())
                                .setConnectionId(soapPathTrace.getConnectionId());
                        for (nl.surf.polynsi.soap.connection.requester.PathType soapPathType : soapPathTrace.getPath()) {
                            Path.Builder pbPathBuilder = Path.newBuilder();
                            List<SegmentType> soapSegmentTypes = soapPathType.getSegment();
                            /*
                              SOAP `segment` elements effectively constitute a list. As such have have an
                              implied order. However for some reason they have an explicit `order` attribute
                              that defines their order. The protobuf version of `Segment` intentionally does not
                              have this attribute. Instead it relies on the implied order of list elements. The
                              presence of the `order` attribute however, *suggests* the SOAP `segment` elements
                              might not be ordered based on the order they appear in. Hence we order them
                              explicitly so that their order is the order of appearance, fullfilling the
                              implicit ordering in the protobuf version.
                            */
                            soapSegmentTypes.sort(Comparator.comparing(SegmentType::getOrder));
                            for (SegmentType soapSegmentType : soapSegmentTypes) {
                                Segment.Builder pbSegmentBuilder = Segment.newBuilder().setId(soapSegmentType.getId())
                                        .setConnectionId(soapSegmentType.getConnectionId());
                                List<StpType> soapStpTypes = soapSegmentType.getStp();
                                /*
                                 Similarly to SOAP `segment` elements, SOAP `stp` elements, although being part of a
                                 list, have an explicit `order` attribute. Suggesting they might not be ordered
                                 based on their order of appearance. The protobuf version does rely on the order of
                                 appearance, hence we sort them first.
                                 */
                                soapStpTypes.sort(Comparator.comparing(StpType::getOrder));
                                pbSegmentBuilder.addAllStps(soapStpTypes.stream().map(StpType::getValue)
                                        .collect(Collectors.toList()));
                                pbPathBuilder.addSegments(pbSegmentBuilder);
                            }
                            pbPathTraceBuilder.addPaths(pbPathBuilder);
                        }
                        pbHeaderBuilder.setPathTrace(pbPathTraceBuilder);
                    }
                }
            }
            return pbHeaderBuilder.build();
        } catch (Exception e) {
            throw new ConverterException("Could not convert SOAP header to protobuf header.", e);
        }
    }

    public static CommonHeaderType toSoap(Header pbHeader) throws ConverterException {
        nl.surf.polynsi.soap.framework.headers.ObjectFactory headerObjFactory =
                new nl.surf.polynsi.soap.framework.headers.ObjectFactory();
        CommonHeaderType soapHeader = headerObjFactory.createCommonHeaderType();
        soapHeader.setProtocolVersion(pbHeader.getProtocolVersion());
        soapHeader.setCorrelationId(pbHeader.getCorrelationId());
        soapHeader.setRequesterNSA(pbHeader.getRequesterNsa());
        soapHeader.setProviderNSA(pbHeader.getProviderNsa());
        if (!pbHeader.getReplyTo().isEmpty()) {
            soapHeader.setReplyTo(pbHeader.getReplyTo());
        }
        if (!pbHeader.getSessionSecurityAttributes().isEmpty()) {
            List<SessionSecurityAttrType> soapSSAs = soapHeader.getSessionSecurityAttr();
            soapSSAs.addAll(toSessionSecurityAttr(pbHeader.getSessionSecurityAttributes()));
        }
        if (pbHeader.getPathTrace().isInitialized()) {
            PathTrace pbPathTrace = pbHeader.getPathTrace();
            nl.surf.polynsi.soap.connection.requester.ObjectFactory policiesObjFactory = new nl.surf.polynsi.soap.connection.requester.ObjectFactory();
            PathTraceType pathTraceType = policiesObjFactory.createPathTraceType();
            pathTraceType.setId(pbPathTrace.getId());
            pathTraceType.setConnectionId(pbPathTrace.getConnectionId());
            List<nl.surf.polynsi.soap.connection.requester.PathType> soapPaths = pathTraceType.getPath();
            for (Path pbPath : pbPathTrace.getPathsList()) {
                nl.surf.polynsi.soap.connection.requester.PathType soapPath = policiesObjFactory.createPathType();
                List<SegmentType> soapSegments = soapPath.getSegment();
                ListIterator<Segment> pbSegmentsIterator = pbPath.getSegmentsList().listIterator();
                while (pbSegmentsIterator.hasNext()) {
                    int segOrder = pbSegmentsIterator.nextIndex();
                    Segment pbSegment = pbSegmentsIterator.next();
                    SegmentType soapSegment = policiesObjFactory.createSegmentType();
                    soapSegment.setOrder(segOrder);
                    soapSegment.setId(pbSegment.getId());
                    soapSegment.setConnectionId(pbSegment.getConnectionId());
                    List<StpType> soapStps = soapSegment.getStp();
                    ListIterator<String> pbStpsIterator = pbSegment.getStpsList().listIterator();
                    while (pbStpsIterator.hasNext()) {
                        StpType soapStp = policiesObjFactory.createStpType();
                        soapStp.setOrder(pbStpsIterator.nextIndex());
                        soapStp.setValue(pbStpsIterator.next());
                        soapStps.add(soapStp);
                    }
                    soapSegments.add(soapSegment);
                }
                soapPaths.add(soapPath);
            }
            List<Object> soapAny = soapHeader.getAny();
            JAXBElement<PathTraceType> ptt = policiesObjFactory.createPathTrace(pathTraceType);
            soapAny.add(ptt);
        }
        return soapHeader;
    }


    public static Schedule toProtobuf(ScheduleType soapSchedule) {
        if (soapSchedule == null) {
            return Schedule.getDefaultInstance();
        }
        Schedule.Builder pbScheduleBuilder = Schedule.newBuilder();
        if (soapSchedule.getStartTime() != null) {
            pbScheduleBuilder.setStartTime(toProtobuf(soapSchedule.getStartTime()));
        }
        if (soapSchedule.getEndTime() != null) {
            pbScheduleBuilder.setEndTime(toProtobuf(soapSchedule.getEndTime()));
        }
        return pbScheduleBuilder.build();
    }

    public static ScheduleType toSoap(Schedule pbSchedule) {
        if (!pbSchedule.isInitialized()) return null;

        var objectFactory = new nl.surf.polynsi.soap.connection.types.ObjectFactory();
        ScheduleType soapSchedule = objectFactory.createScheduleType();

        if (pbSchedule.getStartTime().isInitialized()) {
            soapSchedule.setStartTime(toSoap(pbSchedule.getStartTime()));
        }
        if (pbSchedule.getEndTime().isInitialized()) {
            soapSchedule.setEndTime(toSoap(pbSchedule.getEndTime()));
        }
        return soapSchedule;
    }

    public static ReservationStateEnumType toSoap(ReservationState pbReservationState) throws ConverterException {
        switch (pbReservationState) {
            case RESERVE_START:
                return ReservationStateEnumType.RESERVE_START;
            case RESERVE_CHECKING:
                return ReservationStateEnumType.RESERVE_CHECKING;
            case RESERVE_FAILED:
                return ReservationStateEnumType.RESERVE_FAILED;
            case RESERVE_ABORTING:
                return ReservationStateEnumType.RESERVE_ABORTING;
            case RESERVE_HELD:
                return ReservationStateEnumType.RESERVE_HELD;
            case RESERVE_COMMITTING:
                return ReservationStateEnumType.RESERVE_COMMITTING;
            case RESERVE_TIMEOUT:
                return ReservationStateEnumType.RESERVE_TIMEOUT;
            default:
                throw new ConverterException("Unexpected `ReservationState` value: " + pbReservationState.toString());
        }
    }

    public static ProvisionStateEnumType toSoap(ProvisionState pbProvisionState) throws ConverterException {
        switch (pbProvisionState) {
            case RELEASED:
                return ProvisionStateEnumType.RELEASED;
            case PROVISIONING:
                return ProvisionStateEnumType.PROVISIONING;
            case PROVISIONED:
                return ProvisionStateEnumType.PROVISIONED;
            case RELEASING:
                return ProvisionStateEnumType.RELEASING;
            default:
                throw new ConverterException("Unexpected `ProvisionState` value: " + pbProvisionState.toString());
        }
    }

    public static LifecycleStateEnumType toSoap(LifecycleState pbLifecycleState) throws ConverterException {
        switch (pbLifecycleState) {
            case CREATED:
                return LifecycleStateEnumType.CREATED;
            case FAILED:
                return LifecycleStateEnumType.FAILED;
            case PASSED_END_TIME:
                return LifecycleStateEnumType.PASSED_END_TIME;
            case TERMINATING:
                return LifecycleStateEnumType.TERMINATING;
            case TERMINATED:
                return LifecycleStateEnumType.TERMINATED;
            default:
                throw new ConverterException("Unexpected `LifecycleState` value: " + pbLifecycleState.toString());
        }
    }

    public static DataPlaneStatusType toSoap(DataPlaneStatus pbDataPlaneStatus) {
        var objectFactory = new nl.surf.polynsi.soap.connection.types.ObjectFactory();
        DataPlaneStatusType soapDataPlaneStatus = objectFactory.createDataPlaneStatusType();
        soapDataPlaneStatus.setActive(pbDataPlaneStatus.getActive());
        soapDataPlaneStatus.setVersion(pbDataPlaneStatus.getVersion());
        soapDataPlaneStatus.setVersionConsistent(pbDataPlaneStatus.getVersionConsistent());
        return soapDataPlaneStatus;
    }

    public static ConnectionStatesType toSoap(ConnectionStates pbConnectionStates) throws ConverterException {
        if (!pbConnectionStates.isInitialized()) return null;

        var objectFactory = new nl.surf.polynsi.soap.connection.types.ObjectFactory();
        ConnectionStatesType soapConnectionStates = objectFactory.createConnectionStatesType();
        soapConnectionStates.setReservationState(toSoap(pbConnectionStates.getReservationState()));
        soapConnectionStates.setProvisionState(toSoap(pbConnectionStates.getProvisionState()));
        soapConnectionStates.setLifecycleState(toSoap(pbConnectionStates.getLifecycleState()));
        soapConnectionStates.setDataPlaneStatus(toSoap(pbConnectionStates.getDataPlaneStatus()));
        return soapConnectionStates;
    }

    public static ServiceExceptionType toSoap(ServiceException pbServiceException) {
        var objectFactory = new nl.surf.polynsi.soap.framework.types.ObjectFactory();
        ServiceExceptionType soapServiceException = objectFactory.createServiceExceptionType();
        soapServiceException.setNsaId(pbServiceException.getNsaId());
        soapServiceException.setConnectionId(pbServiceException.getConnectionId());
        soapServiceException.setServiceType(pbServiceException.getServiceType());
        soapServiceException.setErrorId(pbServiceException.getErrorId());
        soapServiceException.setText(pbServiceException.getText());
        if (pbServiceException.getChildExceptionCount() > 0) {
            List<ServiceExceptionType> soapChildExceptions = soapServiceException.getChildException();
            List<ServiceException> pbChildExceptions = pbServiceException.getChildExceptionList();
            for (ServiceException pbChildException: pbChildExceptions) {
                soapChildExceptions.add(toSoap(pbChildException));
            }
        }
        return soapServiceException;
    }
}
