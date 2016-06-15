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

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import org.bson.Document;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.factory.server.spi.FactoryDao;
import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.commons.lang.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.api.dao.mongo.MongoUtil.handleWriteConflict;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link FactoryDao} based on MongoDB storage.
 *
 * <p>Factory collection document scheme:
 * <pre>
 * {
 *     "_id" : "ihb2r9ys1uk4lqxk",
 *     "factory" : {
 *         "v" : "4.0",
 *         "workspace" : {
 *             ...
 *         },
 *         "ide" : {
 *             "onProjectOpened" : {
 *                 "actions" : [
 *                     ...
 *                 ]
 *             }
 *         },
 *         "creator" : {
 *             "created" : 1448271625116,
 *             "userId" : "user9s7lxyvk6eqzgb7t"
 *         }
 *     },
 *     "images" : [
 *         {
 *             "name" : "o9urwt58gnfjs11j",
 *             "type" : "image/jpeg",
 *             "data" : "<binary_data>"
 *         }
 *     ]
 * }
 * </pre>
 *
 * @author Max Shaposhnik
 * @author Anton Korneta
 */
@Singleton
public class FactoryDaoImpl implements FactoryDao {

    private final MongoCollection<FactoryImpl> collection;

    @Inject
    public FactoryDaoImpl(@Named("mongo.db.factory") MongoDatabase database,
                          @Named("factory.storage.db.collection") String collectionName) {
        collection = database.getCollection(collectionName, FactoryImpl.class);
        collection.createIndex(new Document("_id", 1),
                               new IndexOptions().unique(true));
        collection.createIndex(new Document("factory.name", 1).append("factory.creator.userId", 1),
                               new IndexOptions().unique(true).sparse(true));
    }

    @Override
    public FactoryImpl create(FactoryImpl factory) throws ConflictException, ServerException {
        requireNonNull(factory, "Factory must not be null");
        try {
            collection.insertOne(factory);
        } catch (MongoWriteException writeEx) {
            handleWriteConflict(writeEx, format("Factory with id '%s' or name '%s' for creator '%s' already exists",
                                                factory.getId(),
                                                factory.getName(),
                                                factory.getCreator()));
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
        return factory;
    }

    @Override
    public FactoryImpl update(FactoryImpl factory) throws NotFoundException, ConflictException, ServerException {
        requireNonNull(factory, "Factory update must not be null");
        try {
            if (collection.findOneAndReplace(eq("_id", factory.getId()), factory) == null) {
                throw new NotFoundException("Factory with id '" + factory.getId() + "' was not found");
            }
        } catch (MongoWriteException writeEx) {
            handleWriteConflict(writeEx, format("Factory with name '%s' for user '%s' already exists",
                                                factory.getName(),
                                                factory.getCreator()));
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
        return factory;
    }

    @Override
    public void remove(String id) throws NotFoundException, ServerException {
        requireNonNull(id, "Factory identifier must not be null");
        collection.findOneAndDelete(eq("_id", id));
    }

    @Override
    public FactoryImpl getById(String id) throws NotFoundException, ServerException {
        requireNonNull(id, "Factory identifier must not be null");
        final FactoryImpl factory = collection.find(eq("_id", id)).first();
        if (factory == null) {
            throw new NotFoundException("Factory with id '" + id + "' was not found");
        }
        return factory;
    }

    @Override
    public List<FactoryImpl> getByAttribute(int maxItems,
                                            int skipCount,
                                            List<Pair<String, String>> attributes) throws IllegalArgumentException {
        if (skipCount < 0) {
            throw new IllegalArgumentException("Required non-negative value for skipCount parameter");
        }
        final Document query = new Document();
        for (Pair<String, String> pair : attributes) {
            query.append(format("factory.%s", pair.first), pair.second);
        }
        return collection.find(query)
                         .limit(maxItems)
                         .skip(skipCount)
                         .into(new ArrayList<>());
    }
}
