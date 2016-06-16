package com.codenvy.auth.sso.server;

import com.codenvy.auth.sso.server.organization.UserCreationValidator;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * Created by sj on 10.06.16.
 */
@Path("user")
public class SelfRegistrationService {

    private static final Logger LOG = LoggerFactory.getLogger(SelfRegistrationService.class);


    @Inject
    InputDataValidator inputDataValidator;

    @Inject
    UserCreationValidator creationValidator;

    @Inject
    SelfRegistrationManager selfRegistrationManager;


    @POST
    @Path("verify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verify(ValidationData validationData, @Context UriInfo uriInfo)
            throws BadRequestException, ConflictException, ServerException {
        inputDataValidator.validateUserMail(validationData.getEmail());
        creationValidator.ensureUserCreationAllowed(validationData.getEmail(), validationData.getUserName());

        try {
            selfRegistrationManager.sendVerifiactionEmail(validationData,
                                                          uriInfo.getRequestUri().getQuery(),
                                                          uriInfo.getBaseUriBuilder().replacePath(null).build().toString());
        } catch (IOException e) {
            throw new ServerException("Not able to send confirmation email. Please try again later");
        }
        LOG.info("EVENT#signup-validation-email-send# EMAIL#{}#", validationData.getEmail());
        LOG.info("Email validation message send to {}", validationData.getEmail());

        return Response.ok().build();
    }

    @POST
    @Path("confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response confirm() {
        return null;
    }

    public static class ValidationData {
        private String email;
        private String userName;
        private String password;

        public ValidationData() {
        }

        public ValidationData(String email, String userName, String password) {
            this.email = email;
            this.userName = userName;
            this.password = password;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

    }
}
