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
package com.codenvy.auth.sso.client;

import com.codenvy.api.dao.authentication.AccessTicket;
import com.codenvy.api.dao.authentication.TicketManager;
import com.codenvy.machine.authentication.server.MachineTokenRegistry;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Retrieves master {@link Subject} based on the machine token
 * Machine token detection is simple and based on the machine token prefix,
 * so if token is prefixed with 'machine' then the mechanism is triggered
 * otherwise method call delegated to the super {@link HttpSsoServerClient#getSubject(String, String)}.
 *
 * <p>Note that this component must be deployed to api war only.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class MachineSsoServerClient extends HttpSsoServerClient {
    private static final Logger LOG = LoggerFactory.getLogger(MachineSsoServerClient.class);

    private final MachineTokenRegistry tokenRegistry;
    private final TicketManager        ticketManager;
    private final UserManager          userManager;

    @Inject
    public MachineSsoServerClient(@Named("api.endpoint") String apiEndpoint,
                                  HttpJsonRequestFactory requestFactory,
                                  MachineTokenRegistry tokenRegistry,
                                  TicketManager ticketManager,
                                  UserManager userManager) {
        super(apiEndpoint, requestFactory);
        this.tokenRegistry = tokenRegistry;
        this.ticketManager = ticketManager;
        this.userManager = userManager;
    }

    @Override
    public Subject getSubject(String token, String clientUrl) {
        if (!token.startsWith("machine")) {
            return super.getSubject(token, clientUrl);
        }
        try {
            final org.eclipse.che.api.user.server.dao.User user = userManager.getById(tokenRegistry.getUserId(token));
            final Optional<AccessTicket> ticket = ticketManager.getAccessTickets()
                                                               .stream()
                                                               .filter(t -> t.getPrincipal().getUserId().equals(user.getId()))
                                                               .findAny();
            if (ticket.isPresent()) {
                return new SubjectImpl(user.getName(),
                                       user.getId(),
                                       ticket.get().getAccessToken(),
                                       ImmutableSet.of("user"),
                                       false);
            }
        } catch (ApiException x) {
            LOG.warn(x.getLocalizedMessage(), x);
        }
        return null;
    }
}
