package com.codenvy.auth.sso.server;

import com.codenvy.mail.MailSenderClient;
import com.codenvy.mail.shared.dto.AttachmentDto;
import com.codenvy.mail.shared.dto.EmailBeanDto;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.commons.lang.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.eclipse.che.commons.lang.IoUtil.getResource;
import static org.eclipse.che.commons.lang.IoUtil.readAndCloseQuietly;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Created by sj on 14.06.16.
 */
public class SelfRegistrationManager {
    private static final Logger LOG = LoggerFactory.getLogger(SelfRegistrationManager.class);

    // TODO made this configurable
    private static final String MAIL_TEMPLATE = "email-templates/verify_email_address.html";
    private static final String LOGO          = "/email-templates/header.png";
    private static final String LOGO_CID      = "codenvyLogo";

    @Inject
    BearerTokenManager tokenManager;
    @Inject
    MailSenderClient   mailSenderClient;
    @Inject
    @Named("mailsender.application.from.email.address")
    String             mailFrom;


    public void createUser(String token){

    }


    public void sendVerificationEmail(SelfRegistrationService.ValidationData validationData, String queryParams, String masterHostUrl)
            throws IOException {
        try {
            Map<String, String> props = new HashMap<>();
            props.put("logo.cid", "codenvyLogo");
            props.put("bearertoken", tokenManager.generateBearerToken(
                    ImmutableMap.of(
                            "initiator", "email",
                            "email", validationData.getEmail(),
                            "userName", validationData.getUserName(),
                            "password", validationData.getPassword()
                                   )));
            props.put("additional.query.params", queryParams);
            props.put("com.codenvy.masterhost.url", masterHostUrl);

            File logo = new File(this.getClass().getResource(LOGO).getPath());
            AttachmentDto attachmentDto = newDto(AttachmentDto.class)
                    .withContent(Base64.getEncoder().encodeToString(Files.toByteArray(logo)))
                    .withContentId(LOGO_CID)
                    .withFileName("logo.png");

            EmailBeanDto emailBeanDto = newDto(EmailBeanDto.class)
                    .withBody(Deserializer.resolveVariables(readAndCloseQuietly(getResource("/" + MAIL_TEMPLATE)), props))
                    .withFrom(mailFrom)
                    .withTo(validationData.getEmail())
                    .withReplyTo(null)
                    .withSubject("Verify Your Codenvy Account")
                    .withMimeType(TEXT_HTML)
                    .withAttachments(Collections.singletonList(attachmentDto));


            mailSenderClient.sendMail(emailBeanDto);
        } catch (ApiException e) {
            LOG.warn("Unable to send confirmation email", e);
            throw new IOException("Not able to send confirmation email", e);
        }
    }
}
