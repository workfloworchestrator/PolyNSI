
package nl.surf.polynsi.soap.connection.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 This is a generic type definition for a "Confirmed" messages in
 *                 response to a successful processing of a previous "Request"
 *                 message such as provision, release, and terminate.
 * 
 *                 Elements:
 * 
 *                 connectionId - The Provider NSA assigned connectionId for this
 *                 reservation request. This value will be unique within the context
 *                 of the Provider NSA.
 *             
 * 
 * <p>Java class for GenericConfirmedType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GenericConfirmedType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="connectionId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ConnectionIdType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GenericConfirmedType", propOrder = {
    "connectionId"
})
public class GenericConfirmedType {

    @XmlElement(required = true)
    protected String connectionId;

    /**
     * Gets the value of the connectionId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Sets the value of the connectionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConnectionId(String value) {
        this.connectionId = value;
    }

}
