
package org.oasis.saml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import nl.surf.polynsi.soap.connection.requester.Adapter1;


/**
 * <p>Java class for ConditionsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConditionsType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Condition"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AudienceRestriction"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}OneTimeUse"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}ProxyRestriction"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="NotBefore" type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *       &lt;attribute name="NotOnOrAfter" type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConditionsType", propOrder = {
    "conditionOrAudienceRestrictionOrOneTimeUse"
})
public class ConditionsType {

    @XmlElements({
        @XmlElement(name = "Condition"),
        @XmlElement(name = "AudienceRestriction", type = AudienceRestrictionType.class),
        @XmlElement(name = "OneTimeUse", type = OneTimeUseType.class),
        @XmlElement(name = "ProxyRestriction", type = ProxyRestrictionType.class)
    })
    protected List<ConditionAbstractType> conditionOrAudienceRestrictionOrOneTimeUse;
    @XmlAttribute(name = "NotBefore")
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "dateTime")
    protected Date notBefore;
    @XmlAttribute(name = "NotOnOrAfter")
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "dateTime")
    protected Date notOnOrAfter;

    /**
     * Gets the value of the conditionOrAudienceRestrictionOrOneTimeUse property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the conditionOrAudienceRestrictionOrOneTimeUse property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConditionOrAudienceRestrictionOrOneTimeUse().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ConditionAbstractType }
     * {@link AudienceRestrictionType }
     * {@link OneTimeUseType }
     * {@link ProxyRestrictionType }
     * 
     * 
     */
    public List<ConditionAbstractType> getConditionOrAudienceRestrictionOrOneTimeUse() {
        if (conditionOrAudienceRestrictionOrOneTimeUse == null) {
            conditionOrAudienceRestrictionOrOneTimeUse = new ArrayList<ConditionAbstractType>();
        }
        return this.conditionOrAudienceRestrictionOrOneTimeUse;
    }

    /**
     * Gets the value of the notBefore property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Date getNotBefore() {
        return notBefore;
    }

    /**
     * Sets the value of the notBefore property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotBefore(Date value) {
        this.notBefore = value;
    }

    /**
     * Gets the value of the notOnOrAfter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Date getNotOnOrAfter() {
        return notOnOrAfter;
    }

    /**
     * Sets the value of the notOnOrAfter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotOnOrAfter(Date value) {
        this.notOnOrAfter = value;
    }

}
