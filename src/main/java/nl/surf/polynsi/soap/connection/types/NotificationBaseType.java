
package nl.surf.polynsi.soap.connection.types;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import nl.surf.polynsi.soap.connection.requester.Adapter1;


/**
 * 
 *                 A base type definition for an autonomous message issued from a
 *                 Provider NSA to a Requester NSA.
 *                 
 *                 Elements:
 *                 
 *                 connectionId - The Provider NSA assigned connectionId that this
 *                 notification is against.
 * 
 *                 notificationId - A notification identifier that is unique in the
 *                 context of a connectionId.  This is a linearly increasing
 *                 identifier that can be used for ordering notifications in the
 *                 context of the connectionId.
 *                 
 *                 timeStamp - Time the event was generated on the originating NSA.
 *             
 * 
 * <p>Java class for NotificationBaseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="NotificationBaseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="connectionId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ConnectionIdType"/&gt;
 *         &lt;element name="notificationId" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}NotificationIdType"/&gt;
 *         &lt;element name="timeStamp" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}DateTimeType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NotificationBaseType", propOrder = {
    "connectionId",
    "notificationId",
    "timeStamp"
})
@XmlSeeAlso({
    ErrorEventType.class,
    ReserveTimeoutRequestType.class,
    DataPlaneStateChangeRequestType.class,
    MessageDeliveryTimeoutRequestType.class
})
public class NotificationBaseType {

    @XmlElement(required = true)
    protected String connectionId;
    protected long notificationId;
    @XmlElement(required = true, type = String.class)
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "dateTime")
    protected Date timeStamp;

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

    /**
     * Gets the value of the notificationId property.
     * 
     */
    public long getNotificationId() {
        return notificationId;
    }

    /**
     * Sets the value of the notificationId property.
     * 
     */
    public void setNotificationId(long value) {
        this.notificationId = value;
    }

    /**
     * Gets the value of the timeStamp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the value of the timeStamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTimeStamp(Date value) {
        this.timeStamp = value;
    }

}
