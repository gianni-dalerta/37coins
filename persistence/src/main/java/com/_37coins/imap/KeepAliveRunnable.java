package com._37coins.imap;

import java.util.Date;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;

/**
 * Runnable used to keep alive the connection to the IMAP server
 * 
 * @author Juan Mart�n Sotuyo Dodero <jmsotuyo@monits.com>
 */
public class KeepAliveRunnable implements Runnable {

    private static final long KEEP_ALIVE_FREQ = 60L * 1000L;
    Logger log = LoggerFactory.getLogger(KeepAliveRunnable.class);

    private IMAPFolder folder;
    
    public KeepAliveRunnable(IMAPFolder folder) {
        this.folder = folder;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(KEEP_ALIVE_FREQ);

                // Perform a NOOP just to keep alive the connection
                folder.doCommand(new IMAPFolder.ProtocolCommand() {
                	@Override
                    public Object doCommand(IMAPProtocol p)
                            throws ProtocolException {
                		log.debug("doing NOOP: "+ new Date());
                        p.simpleCommand("NOOP", null);
                        return null;
                    }
                });
            } catch (InterruptedException e) {
                // Ignore, just aborting the thread...
            	log.info("keep alive interrupted");
            } catch (MessagingException e) {
                // Shouldn't really happen...
            	log.warn("Unexpected exception while keeping alive the IDLE connection" + e.getMessage());
            	Thread.currentThread().interrupt();
            }
        }
    }
}