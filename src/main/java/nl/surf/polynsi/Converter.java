package nl.surf.polynsi;

import com.google.protobuf.Timestamp;
import nl.surf.polynsi.soap.ConverterException;
import nl.surf.polynsi.soap.framework.headers.CommonHeaderType;
import nl.surf.polynsi.soap.framework.headers.SessionSecurityAttrType;
import nl.surf.polynsi.soap.policies.PathTraceType;
import nl.surf.polynsi.soap.policies.PathType;
import nl.surf.polynsi.soap.policies.SegmentType;
import nl.surf.polynsi.soap.policies.StpType;
import org.ogf.nsi.grpc.connection.common.Header;
import org.ogf.nsi.grpc.policy.Path;
import org.ogf.nsi.grpc.policy.PathTrace;
import org.ogf.nsi.grpc.policy.Segment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
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
    private static String getSessionSecurityAttrAsString(CommonHeaderType soapHeader) throws ConverterException {
        try {
            JAXBContext ssaContext = JAXBContext
                    .newInstance("nl.surf.polynsi.soap.framework.headers",
                            nl.surf.polynsi.soap.framework.headers.ObjectFactory.class
                            .getClassLoader());
            Marshaller marshaller = ssaContext.createMarshaller();
            // Enable formatted output when debugging.
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            // Wrap it, as CommonHeaderType is not annotated with @XMLRootElement
            JAXBElement<CommonHeaderType> jaxbElemHeader = new JAXBElement<>(new QName("", "header"),
                    CommonHeaderType.class, soapHeader);
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
                String ssa = getSessionSecurityAttrAsString(soapHeader);
                pbHeaderBuilder.setSessionSecurityAttributes(ssa);
            }

            // Process NSI extensions that are included in the SOAP header
            for (Object elem : soapHeader.getAny()) {
                if (elem instanceof Element) {
                    Element hdElem = (Element) elem;
                    if (hdElem.getLocalName().equals("pathTrace")) {
                        // dynamically create Java PathTraceType instance from raw XML
                        JAXBContext hdElemContext = JAXBContext
                                .newInstance("nl.surf.polynsi.soap.policies",
                                        nl.surf.polynsi.soap.policies.ObjectFactory.class
                                        .getClassLoader());
                        JAXBElement<PathTraceType> root = hdElemContext.createUnmarshaller()
                                .unmarshal(hdElem.getOwnerDocument().getDocumentElement(), PathTraceType.class);
                        PathTraceType soapPathTrace = root.getValue();

                        // Build protobuf PathTrace message
                        PathTrace.Builder pbPathTraceBuilder = PathTrace.newBuilder().setId(soapPathTrace.getId())
                                .setConnectionId(soapPathTrace.getConnectionId());
                        for (PathType soapPathType : soapPathTrace.getPath()) {
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
                                pbPathBuilder.addSegment(pbSegmentBuilder);
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
}
