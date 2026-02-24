package com.ankur.webcurve.fix;

import com.ankur.webcurve.exchange.Exchange;
import lombok.extern.slf4j.Slf4j;
import quickfix.*;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

import static quickfix.Acceptor.*;
/**
 * Industry term: FIX Gateway / Order Gateway (Sell-Side / Exchange-Side)
 *
 * FIX (Financial Information eXchange) is the universal messaging protocol for
 * electronic trading, used by every major exchange and broker-dealer globally
 * since 1992. A FIX Gateway is the network entry point into an exchange —
 * brokers establish FIX sessions over TCP and submit orders as structured messages.
 *
 * This class is the acceptor (server side): it listens for incoming broker
 * connections using QuickFIX/J's SocketAcceptor. In production, exchange FIX
 * gateways handle thousands of concurrent sessions simultaneously. Firms like
 * NYSE, ASX, and HKEX operate co-location facilities where hedge funds and
 * market makers place their servers physically next to the matching engine to
 * achieve microsecond round-trip latency.
 *
 * Configuration is read from exchange.cfg (BeginString=FIX.4.2, SocketAcceptPort).
 * The dynamic session provider supports client-initiated sessions without
 * pre-configuration — standard practice for exchange gateways with many clients.
 */
@Slf4j
public class ExchangeFixGateway {
    private SocketAcceptor accepter = null;
    private final Map<InetSocketAddress, List<TemplateMapping>> dynamicSessionMappings = new HashMap<InetSocketAddress, List<TemplateMapping>>();
    private Exchange exchange = null;
    private SessionSettings settings;
   
    public ExchangeFixGateway(Exchange exchange)
    {
    	super();
    	this.exchange = exchange;
    }

    private void configureDynamicSessions(SessionSettings settings, ExchangeFixManager application,
            MessageStoreFactory messageStoreFactory, LogFactory logFactory,
            MessageFactory messageFactory) throws ConfigError, FieldConvertError {
        //
        // If a session template is detected in the settings, then
        // set up a dynamic session provider.
        //

        Iterator<SessionID> sectionIterator = settings.sectionIterator();
        while (sectionIterator.hasNext()) {
            SessionID sessionID = sectionIterator.next();
            if (isSessionTemplate(settings, sessionID)) {
                InetSocketAddress address = getAcceptorSocketAddress(settings, sessionID);
                getMappings(address).add(new TemplateMapping(sessionID, sessionID));
            }
        }

        for (Map.Entry<InetSocketAddress, List<TemplateMapping>> entry : dynamicSessionMappings
                .entrySet()) {
            accepter.setSessionProvider(entry.getKey(), new DynamicAcceptorSessionProvider(
                    settings, entry.getValue(), application, messageStoreFactory, logFactory,
                    messageFactory));
        }
    }

    private List<TemplateMapping> getMappings(InetSocketAddress address) {
        List<TemplateMapping> mappings = dynamicSessionMappings.get(address);
        if (mappings == null) {
            mappings = new ArrayList<TemplateMapping>();
            dynamicSessionMappings.put(address, mappings);
        }
        return mappings;
    }

    private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID)
            throws ConfigError, FieldConvertError {
        String acceptorHost = "0.0.0.0";
        if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
            acceptorHost = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
        }
        int acceptorPort = (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT);

        InetSocketAddress address = new InetSocketAddress(acceptorHost, acceptorPort);
        return address;
    }

    private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID)
            throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE)
                && settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
    }

    public boolean open(String cfgFile) 
    {
        try {
            InputStream inputStream = getSettingsInputStream(cfgFile);
            if (null != inputStream)
            	settings = new SessionSettings(inputStream);
            else
            {
                log.error("Cant load configuration");
            	return false;
            }
            inputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        
        ExchangeFixManager application;
		try {
			application = new ExchangeFixManager(exchange);
			
			MessageStoreFactory messageStoreFactory;
			messageStoreFactory = new MemoryStoreFactory();
			
	        LogFactory logFactory = new ScreenLogFactory(true, true, true);
	        MessageFactory messageFactory = new DefaultMessageFactory();
	
	        accepter = new SocketAcceptor(application, messageStoreFactory, settings, logFactory,
	                messageFactory);
			configureDynamicSessions(settings, application, messageStoreFactory, logFactory,
			        messageFactory);

			accepter.start();

		} catch (FieldConvertError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("error in construct FixApp: " + e.toString());
			return false;
		} catch (ConfigError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("error in construct FixApp: " + e.toString());
		}
 
        return true;
    }

    public void close() {
        accepter.stop();
     }

    private static InputStream getSettingsInputStream(String cfgFile) throws FileNotFoundException {
        InputStream inputStream = null;
        if (cfgFile == null || cfgFile.equals("")) {
        	log.info("loading config from resource");
            inputStream = ExchangeFixGateway.class.getResourceAsStream("exchange.cfg");
        } else {
        	log.info("loading config from: " + cfgFile);
            inputStream = new FileInputStream(cfgFile);
        }
        
        if (inputStream == null) {
            log.error("missing configuration file: exchange.cfg");
        }
        return inputStream;
    }

}
