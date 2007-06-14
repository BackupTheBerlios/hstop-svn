package net.sf.microlog.example;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import net.sf.microlog.Logger;
import net.sf.microlog.util.GlobalProperties;

/**
 * This MIDlet shows how to use a property file for configuration.
 * @author Johan Karlsson
 *
 */
public class PropertiesConfigMidlet extends MIDlet {
	
	private boolean firstTime = true;
    
    private Logger log = Logger.getLogger();
	
	public PropertiesConfigMidlet() {
		super();
            
        try {
            GlobalProperties.init(this);
        } catch (IllegalStateException e) {
            //Ignore this exception. It is already initiated.
        }

		log.configure(GlobalProperties.getInstance());
	}

	protected void startApp() throws MIDletStateChangeException {
        if(firstTime){   // first time called...
            log.info("startApp() first time");
            firstTime = false;
        } else {
            log.info("startApp() again");
        }
	}

	protected void pauseApp() {
		log.info("pauseApp()");
	}

	protected void destroyApp(boolean conditional) throws MIDletStateChangeException {
		log.info("destroyApp()");
	}

}
