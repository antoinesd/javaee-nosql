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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

/**
 *
 */

@MongoClientDefinition(name = MongoExtensionTest.TEST, url = MongoExtensionTest.MONGODB_TEST_URI)

@RunWith(Arquillian.class)
public class MongoExtensionTest {

    public static final String MONGODB_TEST_URI = "mongodb://localhost";
    public static final String TEST = "test";
    static final String TEST_DB = "myTestDb";
    static final String TEST_COLLECTION = "testCollection";
    @Inject
    MongoClient mongoClient;

    @Inject
    @Mongo(db = TEST_DB)
    DB myDB;

    @Inject
    @Mongo(db = TEST_DB, collection = TEST_COLLECTION)
    DBCollection myCollection;

    @Deployment
    public static Archive<?> createTestArchive() throws FileNotFoundException {
        WebArchive ret = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addPackage("org.jboss.javaee.mongodb")
                .addAsLibraries(Maven.resolver()
                        .loadPomFromFile("pom.xml")
                        .resolve("org.mongodb:mongo-java-driver")
                        .withTransitivity().as(JavaArchive.class))
                .addAsWebInfResource("beans.xml", "beans.xml")
                .addAsServiceProvider(Extension.class, MongoExtension.class);
        return ret;
    }

    @Test
    public void shouldInjectedBeNotNull() {
        Assert.assertNotNull(mongoClient);
        Assert.assertNotNull(myDB);
        Assert.assertNotNull(myCollection);
    }


    @Test
    public void shouldInjectedDbBeTheSameThanResolved() {
        DB db = mongoClient.getDB(TEST_DB);
        Assert.assertEquals(db, myDB);
    }


    @Test
    public void shouldInjectedColBeTheSameThanResolved() {
        DBCollection coll = myDB.getCollection(TEST_COLLECTION);
        Assert.assertEquals(coll, myCollection);
    }

    @Test
    public void shouldBeAbleToInsert() {
        myCollection.insert(getBasicDBObject());

        DBObject myDoc = myCollection.findOne();

        Assert.assertNotNull(myDoc);
    }

    private BasicDBObject getBasicDBObject() {
        return new BasicDBObject("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("info", new BasicDBObject("x", 203).append("y", 102));
    }

    @Test
    public void shouldCdiAndClassicCallContainTheSameValue() throws UnknownHostException {

        MongoClient mgc = new MongoClient(new MongoClientURI(MONGODB_TEST_URI));
        DB db = mgc.getDB(TEST_DB);
        DBCollection dbc = db.getCollection(TEST_COLLECTION);
        dbc.insert(getBasicDBObject());
        DBObject inserted = dbc.findOne();

        DBObject myDoc = myCollection.findOne();

        Assert.assertEquals(inserted, myDoc);
    }
}
