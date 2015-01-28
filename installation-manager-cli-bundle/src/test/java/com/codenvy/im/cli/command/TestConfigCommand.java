/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.im.cli.command;


import com.codenvy.im.service.InstallationManagerConfig;
import com.codenvy.im.service.InstallationManagerService;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Anatoliy Bazko */
public class TestConfigCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;
    private String okStatus = "{\"status\": \"OK\"}";

    @Mock
    private InstallationManagerService service;
    @Mock
    private CommandSession             commandSession;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new ConfigCommand());
        spyCommand.service = service;

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void testSetConfig() throws Exception {
        doReturn(okStatus).when(service).setConfig(any(InstallationManagerConfig.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--download-dir", "test");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, okStatus + "\n");
    }

    @Test
    public void testSetEmptyPort() throws Exception {
        doReturn(okStatus).when(service).setConfig(any(InstallationManagerConfig.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--proxy-port", " ");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, okStatus + "\n");
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(okStatus).when(service).getConfig();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, okStatus + "\n");
    }
}