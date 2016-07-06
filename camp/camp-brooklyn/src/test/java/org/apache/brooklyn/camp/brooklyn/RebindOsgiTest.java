/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.camp.brooklyn;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.util.List;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.api.policy.Policy;
import org.apache.brooklyn.core.catalog.internal.CatalogUtils;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.StartableApplication;
import org.apache.brooklyn.core.mgmt.ha.OsgiManager;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.mgmt.internal.ManagementContextInternal;
import org.apache.brooklyn.core.mgmt.osgi.OsgiStandaloneTest;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.core.test.entity.TestEntity;
import org.apache.brooklyn.util.core.osgi.Osgis;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.brooklyn.util.javalang.Reflections;
import org.apache.brooklyn.util.osgi.OsgiTestResources;
import org.jclouds.compute.domain.OsFamily;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class RebindOsgiTest extends AbstractYamlRebindTest {

    private static final Logger LOG = LoggerFactory.getLogger(RebindOsgiTest.class);

    private static final String OSGI_BUNDLE_URL = OsgiStandaloneTest.BROOKLYN_TEST_OSGI_ENTITIES_URL;
    private static final String OSGI_BUNDLE_SYMBOLIC_NAME = "org.apache.brooklyn.test.resources.osgi.brooklyn-test-osgi-entities";
    private static final String OSGI_ENTITY_TYPE = OsgiTestResources.BROOKLYN_TEST_OSGI_ENTITIES_SIMPLE_ENTITY;
    private static final String OSGI_POLICY_TYPE = OsgiTestResources.BROOKLYN_TEST_OSGI_ENTITIES_SIMPLE_POLICY;
    private static final String OSGI_OBJECT_TYPE = OsgiTestResources.BROOKLYN_TEST_OSGI_ENTITIES_SIMPLE_OBJECT;
    private static final String OSGI_ENTITY_CONFIG_NAME = OsgiTestResources.BROOKLYN_TEST_OSGI_ENTITIES_SIMPLE_ENTITY_CONFIG_NAME;
    private static final String OSGI_ENTITY_SENSOR_NAME = OsgiTestResources.BROOKLYN_TEST_OSGI_ENTITIES_SIMPLE_ENTITY_SENSOR_NAME;

    private List<String> bundleUrlsToInstallOnRebind;
    
    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp() throws Exception {
        bundleUrlsToInstallOnRebind = Lists.newArrayList();
        super.setUp();
    }

    @Override
    protected boolean useOsgi() {
        return true;
    }
    
    @Override
    protected LocalManagementContext createNewManagementContext(File mementoDir) {
        LocalManagementContext result = super.createNewManagementContext(mementoDir);
        for (String bundleUrl : bundleUrlsToInstallOnRebind) {
            try {
                installBundle(result, bundleUrl);
            } catch (Exception e) {
                throw Exceptions.propagate(e);
            }
        }
        return result;
    }
    
    @DataProvider(name = "valInEntityDataProvider")
    public Object[][] valInEntityDataProvider() {
        return new Object[][] {
            {Predicates.alwaysTrue(), false},
            {Predicates.alwaysTrue(), true},
            {OsFamily.CENTOS, false},
            {OsFamily.CENTOS, true},
        };
    }
 
    @Test(dataProvider = "valInEntityDataProvider")
    public void testValInEntity(Object val, boolean useOsgi) throws Exception {
        String appSymbolicName = "my.catalog.app.id.load";
        String appVersion = "0.1.0";
        String appCatalogFormat;
        if (useOsgi) {
            appCatalogFormat = Joiner.on("\n").join(
                    "brooklyn.catalog:",
                    "  id: " + appSymbolicName,
                    "  version: " + appVersion,
                    "  itemType: entity",
                    "  libraries:",
                    "  - " + OSGI_BUNDLE_URL,
                    "  item:",
                    "    type: " + OSGI_ENTITY_TYPE);
        } else {
            appCatalogFormat = Joiner.on("\n").join(
                    "brooklyn.catalog:",
                    "  id: " + appSymbolicName,
                    "  version: " + appVersion,
                    "  itemType: entity",
                    "  item:",
                    "    type: " + TestEntity.class.getName());
        }
        
        // Create the catalog items
        Iterables.getOnlyElement(addCatalogItems(String.format(appCatalogFormat, appVersion)));
        
        // Create an app, using that catalog item
        String appBlueprintYaml = Joiner.on("\n").join(
                "location: localhost\n",
                "services:",
                "- type: " + CatalogUtils.getVersionedId(appSymbolicName, appVersion));
        origApp = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity origEntity = Iterables.getOnlyElement(origApp.getChildren());
        origEntity.config().set(TestEntity.CONF_OBJECT, val);
        
        // Rebind
        rebind();

        Entity newEntity = Iterables.getOnlyElement(newApp.getChildren());
        assertEquals(newEntity.config().get(TestEntity.CONF_OBJECT), val);
    }

    @Test
    public void testValInEntityFromOtherBundle() throws Exception {
        installBundle(mgmt(), OSGI_BUNDLE_URL);
        bundleUrlsToInstallOnRebind.add(OSGI_BUNDLE_URL);
        
        // Create an app, using that catalog item
        String appBlueprintYaml = Joiner.on("\n").join(
                "services:",
                "- type: " + TestEntity.class.getName());
        origApp = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity origEntity = Iterables.getOnlyElement(origApp.getChildren());

        Object configVal = newOsgiSimpleObject("myEntityConfigVal");
        origEntity.config().set(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME), configVal);
        
        // Rebind
        rebind();

        // Ensure app is still there, and that it is usable - e.g. "stop" effector functions as expected
        Entity newEntity = Iterables.getOnlyElement(newApp.getChildren());

        Object newConfigVal = newEntity.config().get(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME));
        assertOsgiSimpleObjectsEqual(newConfigVal, configVal);
    }
    
    @Test
    public void testEntityAndPolicyFromCatalogOsgi() throws Exception {
        String appSymbolicName = "my.catalog.app.id.load";
        String appVersion = "0.1.0";
        String appCatalogFormat = Joiner.on("\n").join(
                "brooklyn.catalog:",
                "  id: " + appSymbolicName,
                "  version: " + appVersion,
                "  itemType: entity",
                "  libraries:",
                "  - " + OSGI_BUNDLE_URL,
                "  item:",
                "    type: " + OSGI_ENTITY_TYPE,
                "    brooklyn.policies:",
                "    - type: " + OSGI_POLICY_TYPE);
        
        // Create the catalog items
        Iterables.getOnlyElement(addCatalogItems(String.format(appCatalogFormat, appVersion)));
        
        // Create an app, using that catalog item
        String appBlueprintYaml = Joiner.on("\n").join(
                "location: localhost\n",
                "services:",
                "- type: " + CatalogUtils.getVersionedId(appSymbolicName, appVersion));
        origApp = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity origEntity = Iterables.getOnlyElement(origApp.getChildren());
        Policy origPolicy = Iterables.getOnlyElement(origEntity.policies());

        // Rebind
        rebind();

        // Ensure app is still there, and that it is usable - e.g. "stop" effector functions as expected
        Entity newEntity = Iterables.getOnlyElement(newApp.getChildren());
        Policy newPolicy = Iterables.getOnlyElement(newEntity.policies());
        assertEquals(newEntity.getCatalogItemId(), appSymbolicName+":"+appVersion);
        assertEquals(newPolicy.getId(), origPolicy.getId());

        // Ensure stop works as expected
        newApp.stop();
        assertFalse(Entities.isManaged(newApp));
        assertFalse(Entities.isManaged(newEntity));
        
        // Ensure can still use catalog item to deploy a new entity
        StartableApplication app2 = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity entity2 = Iterables.getOnlyElement(app2.getChildren());
        assertEquals(entity2.getCatalogItemId(), appSymbolicName+":"+appVersion);
    }

    @Test
    public void testJavaPojoFromCatalogOsgi() throws Exception {
        String appSymbolicName = "my.catalog.app.id.load";
        String appVersion = "0.1.0";
        String appCatalogFormat = Joiner.on("\n").join(
                "brooklyn.catalog:",
                "  id: " + appSymbolicName,
                "  version: " + appVersion,
                "  itemType: entity",
                "  libraries:",
                "  - " + OSGI_BUNDLE_URL,
                "  item:",
                "    type: " + OSGI_ENTITY_TYPE);
        
        // Create the catalog items
        Iterables.getOnlyElement(addCatalogItems(String.format(appCatalogFormat, appVersion)));
        
        // Create an app, using that catalog item
        String appBlueprintYaml = Joiner.on("\n").join(
                "location: localhost\n",
                "services:",
                "- type: " + CatalogUtils.getVersionedId(appSymbolicName, appVersion));
        origApp = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity origEntity = Iterables.getOnlyElement(origApp.getChildren());

        Object configVal = newOsgiSimpleObject("myEntityConfigVal");
        Object sensorVal = newOsgiSimpleObject("myEntitySensorVal");
        origEntity.config().set(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME), configVal);
        origEntity.sensors().set(Sensors.newSensor(Object.class, OSGI_ENTITY_SENSOR_NAME), sensorVal);
        
        // Rebind
        rebind();

        // Ensure app is still there, and that it is usable - e.g. "stop" effector functions as expected
        Entity newEntity = Iterables.getOnlyElement(newApp.getChildren());

        Object newConfigVal = newEntity.config().get(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME));
        Object newSensorVal = newEntity.sensors().get(Sensors.newSensor(Object.class, OSGI_ENTITY_SENSOR_NAME));
        assertOsgiSimpleObjectsEqual(newConfigVal, configVal);
        assertOsgiSimpleObjectsEqual(newSensorVal, sensorVal);
    }
    
    @Test
    public void testBrooklynObjectDslFromCatalogOsgi() throws Exception {
        String appSymbolicName = "my.catalog.app.id.load";
        String appVersion = "0.1.0";
        String appCatalogFormat = Joiner.on("\n").join(
                "brooklyn.catalog:",
                "  id: " + appSymbolicName,
                "  version: " + appVersion,
                "  itemType: entity",
                "  libraries:",
                "  - " + OSGI_BUNDLE_URL,
                "  item:",
                "    type: " + OSGI_ENTITY_TYPE,
                "    brooklyn.config:",
                "      " + OSGI_ENTITY_CONFIG_NAME + ":",
                "        $brooklyn:object:",
                "          type: " + OSGI_OBJECT_TYPE,
                "          object.fields:",
                "            val: myEntityVal");
        
        // Create the catalog items
        Iterables.getOnlyElement(addCatalogItems(String.format(appCatalogFormat, appVersion)));
        
        // Create an app, using that catalog item
        String appBlueprintYaml = Joiner.on("\n").join(
                "location: localhost\n",
                "services:",
                "- type: " + CatalogUtils.getVersionedId(appSymbolicName, appVersion));
        origApp = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity origEntity = Iterables.getOnlyElement(origApp.getChildren());

        Object configVal = origEntity.config().get(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME));
        assertEquals(getOsgiSimpleObjectsVal(configVal), "myEntityVal");
        
        // Rebind
        rebind();

        // Ensure app is still there, and that it is usable - e.g. "stop" effector functions as expected
        Entity newEntity = Iterables.getOnlyElement(newApp.getChildren());

        Object newConfigVal = newEntity.config().get(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME));
        assertOsgiSimpleObjectsEqual(newConfigVal, configVal);
        
        // Ensure stop works as expected
        newApp.stop();
        assertFalse(Entities.isManaged(newApp));
        assertFalse(Entities.isManaged(newEntity));
        
        // Ensure can still use catalog item to deploy a new entity
        StartableApplication app2 = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity entity2 = Iterables.getOnlyElement(app2.getChildren());
        assertEquals(entity2.getCatalogItemId(), appSymbolicName+":"+appVersion);
    }
    
    // TODO Does not do rebind; the config isn't there after rebind.
    // Need to reproduce that in a simpler use-case.
    @Test
    public void testBrooklynObjectDslFromCatalogOsgiInPolicy() throws Exception {
        String appSymbolicName = "my.catalog.app.id.load";
        String appVersion = "0.1.0";
        String appCatalogFormat = Joiner.on("\n").join(
                "brooklyn.catalog:",
                "  id: " + appSymbolicName,
                "  version: " + appVersion,
                "  itemType: entity",
                "  libraries:",
                "  - " + OSGI_BUNDLE_URL,
                "  item:",
                "    type: " + OSGI_ENTITY_TYPE,
                "    brooklyn.policies:",
                "    - type: " + OSGI_POLICY_TYPE,
                "      brooklyn.config:",
                "        " + OSGI_ENTITY_CONFIG_NAME + ":",
                "          $brooklyn:object:",
                "            type: " + OSGI_OBJECT_TYPE,
                "            object.fields:",
                "              val: myPolicyVal");
        
        // Create the catalog items
        Iterables.getOnlyElement(addCatalogItems(String.format(appCatalogFormat, appVersion)));
        
        // Create an app, using that catalog item
        String appBlueprintYaml = Joiner.on("\n").join(
                "location: localhost\n",
                "services:",
                "- type: " + CatalogUtils.getVersionedId(appSymbolicName, appVersion));
        origApp = (StartableApplication) createAndStartApplication(appBlueprintYaml);
        Entity origEntity = Iterables.getOnlyElement(origApp.getChildren());
        Policy origPolicy = Iterables.getOnlyElement(origEntity.policies());

        Object policyConfigVal = origPolicy.config().get(ConfigKeys.newConfigKey(Object.class, OSGI_ENTITY_CONFIG_NAME));
        assertEquals(getOsgiSimpleObjectsVal(policyConfigVal), "myPolicyVal");
    }
    
    private Bundle getBundle(ManagementContext mgmt, final String symbolicName) throws Exception {
        OsgiManager osgiManager = ((ManagementContextInternal)mgmt).getOsgiManager().get();
        Framework framework = osgiManager.getFramework();
        Maybe<Bundle> result = Osgis.bundleFinder(framework)
                .symbolicName(symbolicName)
                .find();
        return result.get();
    }
    
    private Object newOsgiSimpleObject(String val) throws Exception {
        Class<?> osgiObjectClazz = getBundle(mgmt(), OSGI_BUNDLE_SYMBOLIC_NAME).loadClass(OSGI_OBJECT_TYPE);
        return Reflections.invokeConstructorWithArgs(osgiObjectClazz, val).get();
    }
    
    private void assertOsgiSimpleObjectsEqual(Object val1, Object val2) throws Exception {
        if (val2 == null) {
            assertNull(val1);
        } else {
            assertNotNull(val1);
        }
        assertEquals(val1.getClass().getName(), val2.getClass().getName());
        assertEquals(getOsgiSimpleObjectsVal(val1), getOsgiSimpleObjectsVal(val2));
    }

    private String getOsgiSimpleObjectsVal(Object val) throws Exception {
        assertNotNull(val);
        return (String) Reflections.invokeMethodWithArgs(val, "getVal", ImmutableList.of()).get();
    }
    
    private Bundle installBundle(ManagementContext mgmt, String bundleUrl) throws Exception {
        OsgiManager osgiManager = ((ManagementContextInternal)mgmt).getOsgiManager().get();
        Framework framework = osgiManager.getFramework();
        return Osgis.install(framework, bundleUrl);
    }
}
