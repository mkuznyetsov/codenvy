package com.codenvy.ext.java.server;

import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author Mihail Kuznyetsov
 */
public class WsAgentAnalyticsAddresser {
    private static final Logger LOG = LoggerFactory.getLogger(WsAgentAnalyticsAddresser.class);

    @ScheduleRate(period = 1, unit = TimeUnit.HOURS)
    void send() {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL("https://install.codenvycorp.com/codenvy/init/workspace");
            connection = (HttpsURLConnection)url.openConnection();
            connection.getResponseCode();
        } catch (Exception e) {
            LOG.debug("Failed to send agent analytics", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
