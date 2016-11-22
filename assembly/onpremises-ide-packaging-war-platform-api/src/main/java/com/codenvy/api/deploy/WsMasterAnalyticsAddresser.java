package com.codenvy.api.deploy;

import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author Mihail Kuznyetsov
 */
@Singleton
public class WsMasterAnalyticsAddresser {

    private static final Logger LOG = LoggerFactory.getLogger(WsMasterAnalyticsAddresser.class);

    @ScheduleRate(period = 1, unit = TimeUnit.HOURS)
    void send() {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL("https://install.codenvycorp.com/codenvy/init/server");
            connection = (HttpsURLConnection)url.openConnection();
            connection.getResponseCode();
        } catch (Exception e) {
            LOG.debug("Failed to send master analytics", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
