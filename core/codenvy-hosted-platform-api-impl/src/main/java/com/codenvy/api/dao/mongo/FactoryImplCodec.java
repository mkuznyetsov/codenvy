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
package com.codenvy.api.dao.mongo;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.Binary;
import org.eclipse.che.api.factory.server.FactoryImage;
import org.eclipse.che.api.factory.server.model.impl.ActionImpl;
import org.eclipse.che.api.factory.server.model.impl.AuthorImpl;
import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.api.factory.server.model.impl.IdeImpl;
import org.eclipse.che.api.factory.server.model.impl.OnAppClosedImpl;
import org.eclipse.che.api.factory.server.model.impl.OnAppLoadedImpl;
import org.eclipse.che.api.factory.server.model.impl.OnProjectsLoadedImpl;
import org.eclipse.che.api.factory.shared.model.Policies;

import java.util.List;
import java.util.stream.Collectors;

import static com.codenvy.api.dao.mongo.MongoUtil.asDBList;

/**
 * Encodes & decodes {@link FactoryImpl}.
 *
 * @author Anton Korneta
 */
public class FactoryImplCodec extends AbstractDocumentCodec<FactoryImpl> {

    public FactoryImplCodec(CodecRegistry registry) {
        super(registry);
    }

    @Override
    public Document encode(FactoryImpl factory) {
        final Document factoryDoc = new Document().append("name", factory.getName())
                                                  .append("v", factory.getVersion());
        factoryDoc.append("workspace", WorkspaceImplCodec.asDocument(factory.getWorkspace()));
        final AuthorImpl creator = factory.getCreator();
        if (creator != null) {
            final Document creatorDoc = new Document().append("name", creator.getName())
                                                      .append("email", creator.getEmail())
                                                      .append("userId", creator.getUserId())
                                                      .append("created", creator.getCreated());
            factoryDoc.append("creator", creatorDoc);
        }
        final Policies policies = factory.getPolicies();
        if (policies != null) {
            final Document policyDoc = new Document().append("match", policies.getMatch())
                                                     .append("since", policies.getSince())
                                                     .append("until", policies.getUntil())
                                                     .append("create", policies.getCreate())
                                                     .append("referer", policies.getReferer());
            factoryDoc.append("policies", policyDoc);
        }
        if (factory.getIde() != null) {
            factoryDoc.append("ide", asDocument(factory.getIde()));
        }
        final List<Document> imageDocs = factory.getImages()
                                                .stream()
                                                .map(img -> new Document().append("name", img.getName())
                                                                          .append("type", img.getMediaType())
                                                                          .append("data", new Binary(img.getImageData())))
                                                .collect(Collectors.toList());
        return new Document().append("_id", factory.getId())
                             .append("factory", factoryDoc)
                             .append("images", imageDocs);
    }

    @Override
    public FactoryImpl decode(Document document) {
        final FactoryImpl.FactoryImplBuilder factoryBuilder = FactoryImpl.builder();
        final Document factoryDoc = (Document)document.get("factory");
        @SuppressWarnings("unchecked")
        final List<Document> imageDocs = (List<Document>)document.get("images");
        final Document creatorDoc = (Document)document.get("creator");
        if(creatorDoc != null) {
            creatorDoc.getString("name");
        }

//        .append("name", creator.getName())
//        .append("email", creator.getEmail())
//                .append("userId", creator.getUserId())
//                .append("created", creator.getCreated());

        return factoryBuilder.setId(document.getString("_id"))
                             .setName(factoryDoc.getString("name"))
                             .setName(factoryDoc.getString("v"))
                             .setWorkspace(WorkspaceImplCodec.asWorkspaceConfig((Document)factoryDoc.get("workspace")))
                             .setImages(imageDocs.stream()
                                                 .map(FactoryImplCodec::asFactoryImage)
                                                 .collect(Collectors.toSet()))
                             .build();
    }

    @Override
    public Class<FactoryImpl> getEncoderClass() {
        return FactoryImpl.class;
    }

    private static Document asDocument(IdeImpl ide) {
        final Document ideDoc = new Document();
        final OnAppClosedImpl onAppClosed = ide.getOnAppClosed();
        if (onAppClosed != null) {
            ideDoc.append("onAppClosed", new Document().append("actions", onAppClosed.getActions()
                                                                                     .stream()
                                                                                     .map(FactoryImplCodec::asDocument)
                                                                                     .collect(Collectors.toList())));
        }
        final OnAppLoadedImpl onAppLoaded = ide.getOnAppLoaded();
        if (onAppLoaded != null) {
            ideDoc.append("onAppLoaded", new Document().append("actions", onAppLoaded.getActions()
                                                                                     .stream()
                                                                                     .map(FactoryImplCodec::asDocument)
                                                                                     .collect(Collectors.toList())));
        }
        final OnProjectsLoadedImpl onProjectsLoaded = ide.getOnProjectsLoaded();
        if (onAppClosed != null) {
            ideDoc.append("onProjectsLoaded", new Document().append("actions", onProjectsLoaded.getActions()
                                                                                               .stream()
                                                                                               .map(FactoryImplCodec::asDocument)
                                                                                               .collect(Collectors.toList())));
        }
        return ideDoc;
    }

    private static Document asDocument(ActionImpl action) {
        return new Document().append("id", action.getId())
                             .append("properties", asDBList(action.getProperties()));
    }

    private static FactoryImage asFactoryImage(Document document) {
        return new FactoryImage(((Binary)document.get("data")).getData(),
                                document.getString("type"),
                                document.getString("name"));
    }
}
