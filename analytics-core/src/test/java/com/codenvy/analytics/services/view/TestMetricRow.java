/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.services.view;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.Utils;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.datamodel.StringValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.Metric;
import com.codenvy.analytics.metrics.Parameters;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestMetricRow extends BaseTest {

    private Map<String, String> context;
    private Map<String, String> parameters;

    @BeforeClass
    public void prepare() throws Exception {
        context = Utils.initializeContext(Parameters.TimeUnit.DAY);

        parameters = new HashMap<>();
        parameters.put("time-field", "true");
    }

    @Test
    public void testFormatTimeValue15h50m() throws Exception {
        TestedMetric metric = new TestedMetric(15 * 60 * 60 * 1000 + 50 * 60 * 1000);
        Row row = new MetricRow(metric, parameters);

        assertData(row, "15h 50m");
    }

    @Test
    public void testFormatTimeValue30h50m() throws Exception {
        TestedMetric metric = new TestedMetric(30 * 60 * 60 * 1000 + 50 * 60 * 1000);
        Row row = new MetricRow(metric, parameters);

        assertData(row, "30h 50m");
    }

    @Test
    public void testFormatTimeValue30h00m() throws Exception {
        TestedMetric metric = new TestedMetric(30 * 60 * 60 * 1000);
        Row row = new MetricRow(metric, parameters);

        assertData(row, "30h 0m");
    }

    @Test
    public void testFormatTimeValue50m() throws Exception {
        TestedMetric metric = new TestedMetric(50 * 60 * 1000);
        Row row = new MetricRow(metric, parameters);

        assertData(row, "50m");
    }

    @Test
    public void testFormatTimeValue1m() throws Exception {
        TestedMetric metric = new TestedMetric(60 * 1000);
        Row row = new MetricRow(metric, parameters);

        assertData(row, "1m");
    }

    @Test
    public void testFormatTimeValue0m() throws Exception {
        TestedMetric metric = new TestedMetric(30 * 1000);
        Row row = new MetricRow(metric, parameters);

        assertData(row, "");
    }

    private void assertData(Row row, String value) throws Exception {
        List<List<ValueData>> data = row.getData(context, 1);
        assertEquals(data.size(), 1);

        List<ValueData> item = data.get(0);
        assertEquals(item.size(), 1);
        assertEquals(item.get(0), StringValueData.valueOf(value));
    }

    // ----------------------> Tested metric

    private class TestedMetric implements Metric {
        private final long value;

        private TestedMetric(long value) {
            this.value = value;
        }

        @Override
        public ValueData getValue(Map<String, String> context) throws IOException {
            return LongValueData.valueOf(value);
        }

        @Override
        public Class<? extends ValueData> getValueDataClass() {
            return LongValueData.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}
