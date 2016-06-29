/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.managers.helper;

import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/** @author Dmytro Nochevnov */
public class TestNodeConfigHelperCodenvy4 {

    @Mock
    private Config mockConfig;

    private static final String              TEST_HOST_URL  = "codenvy";
    private static final String              TEST_NODE_DNS  = "node1.codenvy";
    private static final NodeConfig.NodeType TEST_NODE_TYPE = NodeConfig.NodeType.MACHINE_NODE;
    private static final NodeConfig          TEST_NODE      = new NodeConfig(TEST_NODE_TYPE, TEST_NODE_DNS);

    private static final String ADDITIONAL_NODES_PROPERTY_NAME = Config.SWARM_NODES;

    private NodeConfigHelperCodenvy4Impl spyConfigHelper;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyConfigHelper = spy(new NodeConfigHelperCodenvy4Impl(mockConfig));
    }

    @Test(dataProvider = "dataRecognizeNodeTypeConfig")
    public void testRecognizeNodeTypeFromConfigBy(String swarmNodes, NodeConfig.NodeType expectedNodeType, NodeConfig.NodeType expectedDefaultNodeType) throws Exception {
        Config testConfig = new Config(ImmutableMap.of("host_url", TEST_HOST_URL,
                                                       Config.SWARM_NODES, swarmNodes));
        NodeConfigHelperCodenvy4Impl testConfigUtil = new NodeConfigHelperCodenvy4Impl(testConfig);

        assertEquals(testConfigUtil.recognizeNodeTypeFromConfigBy(TEST_NODE_DNS), expectedNodeType);
        assertEquals(testConfigUtil.recognizeNodeTypeFromConfigBy(TEST_HOST_URL), expectedDefaultNodeType);
    }

    @DataProvider
    public Object[][] dataRecognizeNodeTypeConfig() {
        return new Object[][] {
            { "", null, null },
            { format("$host_url:2375", TEST_NODE_DNS),  null, NodeConfig.NodeType.MACHINE_NODE },
            { format("%s:2375", TEST_NODE_DNS),  NodeConfig.NodeType.MACHINE_NODE, null },
            { format("$host_url:2375\n%s:2375", TEST_NODE_DNS),  NodeConfig.NodeType.MACHINE_NODE, NodeConfig.NodeType.MACHINE_NODE }
        };
    }

    @Test
    public void testRecognizeNodeTypeFail() {
        NodeConfig.NodeType result = spyConfigHelper.recognizeNodeTypeFromConfigBy(TEST_NODE_DNS);
        assertNull(result);
    }

    @Test
    public void testGetPropertyNameBy() {
        assertEquals(spyConfigHelper.getPropertyNameBy(NodeConfig.NodeType.MACHINE_NODE),
                     ADDITIONAL_NODES_PROPERTY_NAME);
    }

    @Test(dataProvider = "GetValueWithNode")
    public void testGetValueWithNode(List<String> additionalNodes, String addingNodeDns, String expectedResult) {
        doReturn(TEST_HOST_URL).when(mockConfig).getHostUrl();
        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                                String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));
        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.MACHINE_NODE, addingNodeDns);

        String result = spyConfigHelper.getValueWithNode(testNode);
        assertEquals(result, expectedResult);
    }

    @DataProvider(name = "GetValueWithNode")
    public static Object[][] GetValueWithNode() {
        return new Object[][]{
            {new ArrayList(), TEST_HOST_URL, "$host_url:2375"},
            {new ArrayList<>(ImmutableList.of("$host_url:2375")), TEST_HOST_URL + "2", "$host_url:2375\n" + TEST_HOST_URL + "2:2375"}
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Node '" + TEST_NODE_DNS + "' has been already used")
    public void testGetValueWithNodeWhenNodeExists() {
        List<String> additionalNodes = new ArrayList<>();
        additionalNodes.add(String.format("%s:2375", TEST_NODE_DNS));
        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                                String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));

        spyConfigHelper.getValueWithNode(TEST_NODE);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Node list property '" + ADDITIONAL_NODES_PROPERTY_NAME + "' isn't found in Codenvy config")
    public void testGetValueWithNodeWithoutAdditionalNodesProperty() {
        doReturn(null).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                     String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));
        spyConfigHelper.getValueWithNode(TEST_NODE);
    }

    @Test(dataProvider = "GetValueWithoutNode")
    public void testGetValueWithoutNode(List<String> additionalNodes, String removingNodeDns, String expectedResult) {
        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                                String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));
        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.MACHINE_NODE, removingNodeDns);

        String result = spyConfigHelper.getValueWithoutNode(testNode);
        assertEquals(result, expectedResult);
    }

    @DataProvider(name = "GetValueWithoutNode")
    public static Object[][] GetValueWithoutNode() {
        return new Object[][]{
            {new ArrayList<>(ImmutableList.of("test:2375")), "test", ""},
            {new ArrayList<>(ImmutableList.of(
                    "$host_url:2375",
                    "test1:2375",
                    "test2:2375",
                    "test3:2375"
                )),
                 "test1",
                 "$host_url:2375\ntest2:2375\ntest3:2375"},
            {new ArrayList<>(ImmutableList.of(
                    "test1:2375",
                    "test2:2375",
                    "test3:2375"
                )),
                 "test2",
                 "test1:2375\ntest3:2375"},
            {new ArrayList<>(ImmutableList.of(
                    "test1:2375",
                    "test2:2375",
                    "test3:2375"
                )),
                 "test3",
                 "test1:2375\ntest2:2375"},
            };
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "There is no node '" + TEST_NODE_DNS + "' in the list of nodes")
    public void testGetValueWithoutNodeWhenNodeIsNotExists() {
        doReturn(new ArrayList<>()).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                                  String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));

        spyConfigHelper.getValueWithoutNode(TEST_NODE);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Node list property '" + ADDITIONAL_NODES_PROPERTY_NAME + "' isn't found in Codenvy config")
    public void testGetValueWithoutNodeWithoutAdditionalNodesProperty() {
        doReturn(null).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                     String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));
        spyConfigHelper.getValueWithoutNode(TEST_NODE);
    }

    @Test
    public void testGetNodeUrl() {
        String additionalNodeUrl = spyConfigHelper.getNodeUrl(new NodeConfig(NodeConfig.NodeType.MACHINE_NODE, TEST_NODE_DNS));
        assertEquals(additionalNodeUrl, "node1.codenvy:2375");
    }

    @Test(dataProvider = "dataRecognizeNodeConfigFromDns")
    public void testRecognizeNodeConfigFromDns(String dns, NodeConfig expectedNodeConfig) {
        doReturn(TEST_HOST_URL).when(mockConfig).getHostUrl();

        NodeConfig actual = spyConfigHelper.recognizeNodeConfigFromDns(dns);
        assertEquals(actual, expectedNodeConfig);
    }

    @DataProvider
    public Object[][] dataRecognizeNodeConfigFromDns() {
        return new Object[][] {
            { TEST_HOST_URL, new NodeConfig(NodeConfig.NodeType.MACHINE_NODE, TEST_HOST_URL) },
            { TEST_NODE_DNS, new NodeConfig(NodeConfig.NodeType.MACHINE_NODE, TEST_NODE_DNS) }
        };
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Host name of base node of type 'MACHINE_NODE' wasn't found.")
    public void testRecognizeNodeConfigFromDnsWhenBaseNodeDomainUnknown() {
        spyConfigHelper.recognizeNodeConfigFromDns("some");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Illegal DNS name 'node2.another.com' of node. Correct DNS name templates: \\['some.com', 'node<number>.some.com'\\]")
    public void testRecognizeNodeConfigFromDnsWhenDnsDoesNotComplyBaseNodeDomain() {
        doReturn("some.com").when(mockConfig).getHostUrl();
        spyConfigHelper.recognizeNodeConfigFromDns("node2.another.com");
    }

    @Test
    public void testExtractNodesDns() {
        ArrayList additionalNodes = new ArrayList<>(ImmutableList.of(
            "dev.com:2375",       // main docker machine
            "test1.dev.com:2375",
            "test-2.dev.com:2375",
            "test3.dev.com:2375"
        ));

        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                                String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));

        doReturn("dev.com").when(mockConfig).getHostUrl();

        Map<String, List<String>> result = spyConfigHelper.extractNodesDns(NodeConfig.NodeType.MACHINE_NODE);
        assertEquals(result.toString(), "{swarm_nodes=[dev.com, test1.dev.com, test-2.dev.com, test3.dev.com]}");
    }

    @Test
    public void testExtractNodesDnsWhenPropertiesIsAbsent() {
        doReturn(null).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                     String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));

        Map<String, List<String>> result = spyConfigHelper.extractNodesDns(NodeConfig.NodeType.MACHINE_NODE);
        assertNull(result);
    }

    @Test
    public void testGetNodeNumber() {
        ArrayList nodes = new ArrayList<>(ImmutableList.of(
            "dev.com:2375",       // main docker machine
            "test1.dev.com:2375",
            "test2.dev.com:2375",
            "test3.dev.com:2375"
        ));

        doReturn(nodes).when(mockConfig).getAllValues(ADDITIONAL_NODES_PROPERTY_NAME,
                                                                String.valueOf(NodeConfigHelperCodenvy4Impl.NODE_DELIMITER));

        int result = spyConfigHelper.getNodeNumber();
        assertEquals(result, 4);
    }

}
