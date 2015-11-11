package org.apache.brooklyn.test.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.sensor.AttributeSensorAndConfigKey;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.location.localhost.LocalhostMachineProvisioningLocation;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author m4rkmckenna on 27/10/2015.
 */
public class TestSensorTest {

    private static final AttributeSensorAndConfigKey<Boolean, Boolean> BOOLEAN_SENSOR = ConfigKeys.newSensorAndConfigKey(Boolean.class, "boolean-sensor", "Boolean Sensor");
    private static final AttributeSensorAndConfigKey<String, String> STRING_SENSOR = ConfigKeys.newSensorAndConfigKey(String.class, "string-sensor", "String Sensor");
    private static final AttributeSensorAndConfigKey<Object, Object> OBJECT_SENSOR = ConfigKeys.newSensorAndConfigKey(Object.class, "object-sensor", "Object Sensor");

    private TestApplication app;
    private ManagementContext managementContext;
    private LocalhostMachineProvisioningLocation loc;
    private String testId;

    @BeforeMethod
    public void setup() {
        testId = Identifiers.makeRandomId(8);
        app = TestApplication.Factory.newManagedInstanceForTests();
        managementContext = app.getManagementContext();
        loc = managementContext.getLocationManager().createLocation(LocationSpec.create(LocalhostMachineProvisioningLocation.class)
                .configure("name", testId));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }

    @Test
    public void testAssertEqual() {
        //Add Sensor Test for BOOLEAN sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, BOOLEAN_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("equals", true)));
        //Add Sensor Test for STRING sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, STRING_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("equals", testId)));

        //Set BOOLEAN Sensor to true
        app.sensors().set(BOOLEAN_SENSOR, Boolean.TRUE);
        //Set STRING sensor to random string
        app.sensors().set(STRING_SENSOR, testId);


        app.start(ImmutableList.of(loc));

    }

    @Test
    public void testAssertEqualFailure() {
        boolean booleanAssertFailed = false;

        //Add Sensor Test for BOOLEAN sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, BOOLEAN_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("equals", true)));

        //Set BOOLEAN Sensor to false
        app.sensors().set(BOOLEAN_SENSOR, Boolean.FALSE);

        try {
            app.start(ImmutableList.of(loc));
        } catch (final PropagatedRuntimeException pre) {
            final AssertionError assertionError = Exceptions.getFirstThrowableOfType(pre, AssertionError.class);
            assertThat(assertionError).isNotNull();
            booleanAssertFailed = true;
        } finally {
            assertThat(booleanAssertFailed).isTrue();
        }
    }

    @Test
    public void testAssertEqualOnNullSenor() {
        boolean booleanAssertFailed = false;

        //Add Sensor Test for BOOLEAN sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, BOOLEAN_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("equals", false)));

        try {
            app.start(ImmutableList.of(loc));
        } catch (final PropagatedRuntimeException pre) {
            final AssertionError assertionError = Exceptions.getFirstThrowableOfType(pre, AssertionError.class);
            assertThat(assertionError).isNotNull().as("An assertion error should have been thrown");
            booleanAssertFailed = true;
        } finally {
            assertThat(booleanAssertFailed).isTrue().as("Equals assert should have failed as the sensor is NULL");
        }
    }

    @Test
    public void testAssertNull() {
        //Add Sensor Test for BOOLEAN sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, BOOLEAN_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("isNull", true)));
        //Add Sensor Test for STRING sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, STRING_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("isNull", false)));

        //Set STRING sensor to random string
        app.sensors().set(STRING_SENSOR, testId);

        app.start(ImmutableList.of(loc));

    }


    @Test
    public void testAssertNullFail() {
        boolean sensorTestFail = false;
        //Add Sensor Test for STRING sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, STRING_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("isNull", true)));

        //Set STRING sensor to random string
        app.sensors().set(STRING_SENSOR, testId);


        try {
            app.start(ImmutableList.of(loc));
        } catch (final PropagatedRuntimeException pre) {
            final AssertionError assertionError = Exceptions.getFirstThrowableOfType(pre, AssertionError.class);
            assertThat(assertionError).isNotNull().as("An assertion error should have been thrown");
            sensorTestFail = true;
        } finally {
            assertThat(sensorTestFail).isTrue().as("isNull assert should have failed as the sensor has been set");
        }

    }

    @Test
    public void testAssertRegex() {
        final long time = System.currentTimeMillis();
        final String sensorValue = String.format("%s%s%s", Identifiers.makeRandomId(8), time, Identifiers.makeRandomId(8));

        //Add Sensor Test for STRING sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, STRING_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("regex", String.format(".*%s.*", time))));
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, BOOLEAN_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("regex", "true")));

        //Set STRING sensor
        app.sensors().set(STRING_SENSOR, sensorValue);
        app.sensors().set(BOOLEAN_SENSOR, true);


        app.start(ImmutableList.of(loc));
    }

    @Test
    public void testAssertRegexFail() {
        boolean sensorTestFail = false;
        final String sensorValue = String.format("%s%s%s", Identifiers.makeRandomId(8), System.currentTimeMillis(), Identifiers.makeRandomId(8));

        //Add Sensor Test for STRING sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, STRING_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("regex", String.format(".*%s.*", Identifiers.makeRandomId(8)))));

        //Set STRING sensor
        app.sensors().set(STRING_SENSOR, sensorValue);


        try {
            app.start(ImmutableList.of(loc));
        } catch (final PropagatedRuntimeException pre) {
            final AssertionError assertionError = Exceptions.getFirstThrowableOfType(pre, AssertionError.class);
            assertThat(assertionError).isNotNull().as("An assertion error should have been thrown");
            sensorTestFail = true;
        } finally {
            assertThat(sensorTestFail).isTrue().as("regex assert should have failed");
        }
    }

    @Test
    public void testAssertRegexOnNullSensor() {
        boolean sensorTestFail = false;
        final String sensorValue = String.format("%s%s%s", Identifiers.makeRandomId(8), System.currentTimeMillis(), Identifiers.makeRandomId(8));

        //Add Sensor Test for STRING sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, STRING_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("regex", String.format(".*%s.*", Identifiers.makeRandomId(8)))));

        try {
            app.start(ImmutableList.of(loc));
        } catch (final PropagatedRuntimeException pre) {
            final AssertionError assertionError = Exceptions.getFirstThrowableOfType(pre, AssertionError.class);
            assertThat(assertionError).isNotNull().as("An assertion error should have been thrown");
            sensorTestFail = true;
        } finally {
            assertThat(sensorTestFail).isTrue().as("regex assert should have failed");
        }
    }


    @Test
    public void testAssertRegexOnNonStringSensor() {
        //Add Sensor Test for OBJECT sensor
        app.createAndManageChild(EntitySpec.create(TestSensor.class)
                .configure(TestSensor.TARGET_ENTITY, app)
                .configure(TestSensor.SENSOR_NAME, OBJECT_SENSOR.getName())
                .configure(TestSensor.ASSERTIONS, ImmutableMap.of("regex", ".*TestObject.*id=.*")));

        app.sensors().set(OBJECT_SENSOR, new TestObject());

        app.start(ImmutableList.of(loc));

    }


    class TestObject {
        private final String id;

        public TestObject() {
            id = Identifiers.makeRandomId(8);
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

}
