/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.javaee.mongodb;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

/**
 *
 * Contains producers for <code>MongoDB</code> elements qualified with {@link Mongo}
 *
 * @author Antoine Sabot-Durand
 * 
 */
@ApplicationScoped
public class MongoProducers
{
    @Inject
    private MongoClient mongoClient;

    @Produces
    @Mongo
    protected DB produceDb(InjectionPoint ip)
    {
        String id = getMongoAnnotation(ip).db();
        return mongoClient.getDB(id);
    }
    
    @Produces
    @Mongo
    protected DBCollection produceCollection(InjectionPoint ip)
    {
        DB db = produceDb(ip);
        return db.getCollection(getMongoAnnotation(ip).collection());
    }

    protected Mongo getMongoAnnotation(InjectionPoint ip)
    {
        return ip.getAnnotated().getAnnotation(Mongo.class);
    }


}
