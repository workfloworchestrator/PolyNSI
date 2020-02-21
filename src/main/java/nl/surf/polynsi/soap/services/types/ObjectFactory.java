
package nl.surf.polynsi.soap.services.types;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the nl.surf.polynsi.soap.services.types package. 
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


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: nl.surf.polynsi.soap.services.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link StpListType }
     * 
     */
    public StpListType createStpListType() {
        return new StpListType();
    }

    /**
     * Create an instance of {@link OrderedStpType }
     * 
     */
    public OrderedStpType createOrderedStpType() {
        return new OrderedStpType();
    }

    /**
     * Create an instance of {@link TypeValueType }
     * 
     */
    public TypeValueType createTypeValueType() {
        return new TypeValueType();
    }

    /**
     * Create an instance of {@link ClusionType }
     * 
     */
    public ClusionType createClusionType() {
        return new ClusionType();
    }

}
