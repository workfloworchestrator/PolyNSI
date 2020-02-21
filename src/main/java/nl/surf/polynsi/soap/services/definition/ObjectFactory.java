
package nl.surf.polynsi.soap.services.definition;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the nl.surf.polynsi.soap.services.definition package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ServiceDefinition_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/definition", "serviceDefinition");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: nl.surf.polynsi.soap.services.definition
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ServiceDefinitionType }
     * 
     */
    public ServiceDefinitionType createServiceDefinitionType() {
        return new ServiceDefinitionType();
    }

    /**
     * Create an instance of {@link AdaptationType }
     * 
     */
    public AdaptationType createAdaptationType() {
        return new AdaptationType();
    }

    /**
     * Create an instance of {@link SchemaType }
     * 
     */
    public SchemaType createSchemaType() {
        return new SchemaType();
    }

    /**
     * Create an instance of {@link ParameterType }
     * 
     */
    public ParameterType createParameterType() {
        return new ParameterType();
    }

    /**
     * Create an instance of {@link AttributeType }
     * 
     */
    public AttributeType createAttributeType() {
        return new AttributeType();
    }

    /**
     * Create an instance of {@link ErrorType }
     * 
     */
    public ErrorType createErrorType() {
        return new ErrorType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServiceDefinitionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ServiceDefinitionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/services/definition", name = "serviceDefinition")
    public JAXBElement<ServiceDefinitionType> createServiceDefinition(ServiceDefinitionType value) {
        return new JAXBElement<ServiceDefinitionType>(_ServiceDefinition_QNAME, ServiceDefinitionType.class, null, value);
    }

}
