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

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;


/**
 * This CDI Extension registers a <code>Mongoclient</code>
 * defined by adding a {@link MongoClientDefinition} annotation to any class of the application
 * Registration will be aborted if user defines her own <code>MongoClient</code> bean or producer
 *
 * @author Anttoine Sabot-Durand
 */
public class MongoExtension implements Extension {

    private static final Logger log = Logger.getLogger(MongoExtension.class.getName());
    private MongoClientDefinition mongoDef = null;
    private boolean moreThanOne = false;

    /**
     * Looks for {@link MongoClientDefinition} annotation to capture it.
     * Also Checks if the application contains more than one of these definition
     */
    void detectMongoClientDefinition(
            @Observes @WithAnnotations(MongoClientDefinition.class) ProcessAnnotatedType<?> pat) {
        AnnotatedType at = pat.getAnnotatedType();

        MongoClientDefinition md = at.getAnnotation(MongoClientDefinition.class);
        String name = md.name();

        if (mongoDef != null) {
            moreThanOne = true;
        } else {
            mongoDef = md;
        }
    }

    /**
     * Warns user if there's none onr more than one {@link MongoClientDefinition} in the application
     */
    void checkMongoClientUniqueness(@Observes AfterTypeDiscovery atd) {
        if (mongoDef == null) {
            log.warning("No MongoDB data sources found, mongo CDI extension will do nothing");
        } else if (moreThanOne) {
            log.log(Level.WARNING, "You defined more than one MongoDB data source. Only the one with name {0} will be "
                    + "created", mongoDef
                    .name());
        }

    }

    /**
     * If the application has a {@link MongoClientDefinition} register the bean for it unless user has defined a bean or a
     * producer for a <code>MongoClient</code>
     */
    void registerDataSourceBeans(@Observes AfterBeanDiscovery abd, BeanManager bm) {

        if (mongoDef != null) {
            if (bm.getBeans(MongoClient.class, DefaultLiteral.INSTANCE).isEmpty()) {
                log.log(Level.INFO, "Registering bean for MongoDB datasource {0}", mongoDef.name());
                MongoClientURI uri = new MongoClientURI(mongoDef.url());
                abd.addBean(bm.createBean(new MongoClientBeanAttributes(bm.createBeanAttributes(bm.createAnnotatedType
                        (MongoClient.class))), MongoClient.class, new MongoClientProducerFactory(uri)));
            } else {
                log.log(Level.INFO, "Application contains a default MongoClient Bean, automatic registration will be disabled");
            }
        }
    }


    private static class MongoClientBeanAttributes implements BeanAttributes<MongoClient> {

        private BeanAttributes<MongoClient> delegate;

        MongoClientBeanAttributes(BeanAttributes<MongoClient> beanAttributes) {
            delegate = beanAttributes;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return delegate.getQualifiers();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return ApplicationScoped.class;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return delegate.getStereotypes();
        }

        @Override
        public Set<Type> getTypes() {
            return delegate.getTypes();
        }

        @Override
        public boolean isAlternative() {
            return delegate.isAlternative();
        }
    }


    private static class MongoClientProducerFactory
            implements InjectionTargetFactory<MongoClient> {

        MongoClientURI uri;

        MongoClientProducerFactory(MongoClientURI uri) {
            this.uri = uri;
        }

        @Override
        public InjectionTarget<MongoClient> createInjectionTarget(Bean<MongoClient> bean) {
            return new InjectionTarget<MongoClient>() {
                @Override
                public void inject(MongoClient instance, CreationalContext<MongoClient> ctx) {
                }

                @Override
                public void postConstruct(MongoClient instance) {
                }

                @Override
                public void preDestroy(MongoClient instance) {
                }

                @Override
                public MongoClient produce(CreationalContext<MongoClient> ctx) {
                    try {
                        return new MongoClient(uri);
                    } catch (UnknownHostException e) {
                        throw new IllegalArgumentException(e);
                    }
                }

                @Override
                public void dispose(MongoClient instance) {
                    instance.close();
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return Collections.EMPTY_SET;
                }
            };
        }
    }
}
