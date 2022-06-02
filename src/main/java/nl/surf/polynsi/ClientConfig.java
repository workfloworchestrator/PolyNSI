package nl.surf.polynsi;

import nl.surf.polynsi.soap.connection.requester.ConnectionRequesterPort;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 This client config pertains to the configuration of the SOAP clients. The gRPC clients are 'configured'
 where their implementations are actually used and annotated with `@GrpcClient`. I'd rather have had this all done in
 the same location, but this all-over-the-place configuration/hooking up of things seem to be side-effect of Spring
 Boot's dependency injection. It makes searching for things a little difficult.
 */
@Configuration
public class ClientConfig {

    /*
     We should be dynamically configuring (as in, per request) the requester address based on the
     `replyTo` element/field in the header. Though with a single upstream aggregator it will, in practise,
     always be the same one. Hence we should be okay for now. If we get this working properly we
     can always enhance it do it on a per request basis.

     NOTE: As provisional solution the connectionRequesterProxy is now created on a per request basis inside
           ConnectionRequesterService class. If no better solution is found then the ClientConfig class can be
           removed.
     */
//    @Value("${soap.client.connection_requester.address}")
//    private String connectionRequesterAddress;
//
//    @Bean
//    public ConnectionRequesterPort connectionRequesterProxy() {
//        System.out.println("soap.client.connection_requester.address=");
//        System.out.println(connectionRequesterAddress);
//        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
//        jaxWsProxyFactoryBean.setServiceClass(ConnectionRequesterPort.class);
//        jaxWsProxyFactoryBean.setAddress(this.connectionRequesterAddress);
//        return (ConnectionRequesterPort) jaxWsProxyFactoryBean.create();
//    }
}
