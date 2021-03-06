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
package org.apache.brooklyn.entity.software.base;

import java.util.Collection;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.MachineProvisioningLocation;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.annotation.Effector;
import org.apache.brooklyn.core.config.BasicConfigInheritance;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.config.MapConfigKey;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.BrooklynConfigKeys;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle.Transition;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.core.sensor.AttributeSensorAndConfigKey;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.brooklyn.util.time.Duration;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

public interface SoftwareProcess extends Entity, Startable {

    AttributeSensor<String> HOSTNAME = Attributes.HOSTNAME;
    AttributeSensor<String> ADDRESS = Attributes.ADDRESS;
    AttributeSensor<String> SUBNET_HOSTNAME = Attributes.SUBNET_HOSTNAME;
    AttributeSensor<String> SUBNET_ADDRESS = Attributes.SUBNET_ADDRESS;

    // TODO Want this to have typeInheritance.merge as well, but currently only supported for maps
    @SuppressWarnings("serial")
    ConfigKey<Collection<Integer>> REQUIRED_OPEN_LOGIN_PORTS = ConfigKeys.newConfigKey(
            new TypeToken<Collection<Integer>>() {},
            "requiredOpenLoginPorts",
            "The port(s) to be opened (also see 'inboundPorts.autoInfer')",
            ImmutableSet.of(22));

    ConfigKey<String> INBOUND_PORTS_CONFIG_REGEX = ConfigKeys.newStringConfigKey("inboundPorts.configRegex",
            "Regex governing the opening of ports based on config names (see 'inboundPorts.autoInfer')",
            ".*\\.port");

    ConfigKey<Boolean> INBOUND_PORTS_AUTO_INFER = ConfigKeys.newBooleanConfigKey("inboundPorts.autoInfer",
            "By default, the ports to open in iptables and security group is inferred from the config keys. "
                    + "This follows a naming convention (defaulting to all config keys matching the name '*.\\.port', "
                    + "but customizable with the config key 'inboundPorts.configRegex'), and also including all "
                    + "config keys of type 'PortRange'. This behaviour is turned off by setting it to 'false'.",
            true);

    @SetFromFlag("startTimeout")
    ConfigKey<Duration> START_TIMEOUT = BrooklynConfigKeys.START_TIMEOUT;

    @SetFromFlag("startLatch")
    ConfigKey<Boolean> START_LATCH = BrooklynConfigKeys.START_LATCH;

    @SetFromFlag("stopLatch")
    ConfigKey<Boolean> STOP_LATCH = BrooklynConfigKeys.STOP_LATCH;

    @SetFromFlag("setupLatch")
    ConfigKey<Boolean> SETUP_LATCH = BrooklynConfigKeys.SETUP_LATCH;

    @SetFromFlag("installResourcesLatch")
    ConfigKey<Boolean> INSTALL_RESOURCES_LATCH = BrooklynConfigKeys.INSTALL_RESOURCES_LATCH;

    @SetFromFlag("installLatch")
    ConfigKey<Boolean> INSTALL_LATCH = BrooklynConfigKeys.INSTALL_LATCH;

    @SetFromFlag("runtimeResourcesLatch")
    ConfigKey<Boolean> RUNTIME_RESOURCES_LATCH = BrooklynConfigKeys.RUNTIME_RESOURCES_LATCH;

    @SetFromFlag("customizeLatch")
    ConfigKey<Boolean> CUSTOMIZE_LATCH = BrooklynConfigKeys.CUSTOMIZE_LATCH;

    @SetFromFlag("launchLatch")
    ConfigKey<Boolean> LAUNCH_LATCH = BrooklynConfigKeys.LAUNCH_LATCH;

    @SetFromFlag("skipStart")
    ConfigKey<Boolean> ENTITY_STARTED = BrooklynConfigKeys.SKIP_ENTITY_START;

    @SetFromFlag("skipStartIfRunning")
    ConfigKey<Boolean> SKIP_ENTITY_START_IF_RUNNING = BrooklynConfigKeys.SKIP_ENTITY_START_IF_RUNNING;

    @SetFromFlag("skipInstall")
    ConfigKey<Boolean> SKIP_INSTALLATION = BrooklynConfigKeys.SKIP_ENTITY_INSTALLATION;

    @SetFromFlag("preInstallCommand")
    ConfigKey<String> PRE_INSTALL_COMMAND = BrooklynConfigKeys.PRE_INSTALL_COMMAND;

    @SetFromFlag("postInstallCommand")
    ConfigKey<String> POST_INSTALL_COMMAND = BrooklynConfigKeys.POST_INSTALL_COMMAND;

    @SetFromFlag("preCustomizeCommand")
    ConfigKey<String> PRE_CUSTOMIZE_COMMAND = BrooklynConfigKeys.PRE_CUSTOMIZE_COMMAND;

    @SetFromFlag("postCustomizeCommand")
    ConfigKey<String> POST_CUSTOMIZE_COMMAND = BrooklynConfigKeys.POST_CUSTOMIZE_COMMAND;

    @SetFromFlag("preLaunchCommand")
    ConfigKey<String> PRE_LAUNCH_COMMAND = BrooklynConfigKeys.PRE_LAUNCH_COMMAND;

    @SetFromFlag("postLaunchCommand")
    ConfigKey<String> POST_LAUNCH_COMMAND = BrooklynConfigKeys.POST_LAUNCH_COMMAND;

    @SetFromFlag("env")
    MapConfigKey<Object> SHELL_ENVIRONMENT = BrooklynConfigKeys.SHELL_ENVIRONMENT;

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = BrooklynConfigKeys.SUGGESTED_VERSION;

    @SetFromFlag("downloadUrl")
    AttributeSensorAndConfigKey<String,String> DOWNLOAD_URL = Attributes.DOWNLOAD_URL;

    @SetFromFlag("downloadAddonUrls")
    AttributeSensorAndConfigKey<Map<String,String>,Map<String,String>> DOWNLOAD_ADDON_URLS = Attributes.DOWNLOAD_ADDON_URLS;

    @SetFromFlag("archiveNameFormat")
    ConfigKey<String> ARCHIVE_DIRECTORY_NAME_FORMAT = ConfigKeys.newStringConfigKey("archive.nameFormat",
            "The format for the directory created when the installation archive is extracted, if required. " +
            "The version string will be passed in as the first argument, replacing the %s format specifier");

    @SetFromFlag("installLabel")
    ConfigKey<String> INSTALL_UNIQUE_LABEL = BrooklynConfigKeys.INSTALL_UNIQUE_LABEL;

    @SetFromFlag("expandedInstallDir")
    AttributeSensorAndConfigKey<String,String> EXPANDED_INSTALL_DIR = BrooklynConfigKeys.EXPANDED_INSTALL_DIR;

    @SetFromFlag("installDir")
    AttributeSensorAndConfigKey<String,String> INSTALL_DIR = BrooklynConfigKeys.INSTALL_DIR;

    @SetFromFlag("runDir")
    AttributeSensorAndConfigKey<String,String> RUN_DIR = BrooklynConfigKeys.RUN_DIR;

    ConfigKey<Boolean> ADD_OPEN_INBOUND_PORTS_EFFECTOR = ConfigKeys.newBooleanConfigKey(
            "effector.add.openInboundPorts",
            "Whether to add an effector to the entity for opening ports through Cloud security groups", 
            false);

    ConfigKey<Boolean> OPEN_IPTABLES = ConfigKeys.newBooleanConfigKey("openIptables",
            "Whether to open the INBOUND_PORTS via iptables rules; " +
            "if true then ssh in to run iptables commands, as part of machine provisioning", 
            false);

    ConfigKey<Boolean> STOP_IPTABLES = ConfigKeys.newBooleanConfigKey("stopIptables",
            "Whether to stop iptables entirely; " +
            "if true then ssh in to stop the iptables service, as part of machine provisioning", 
            false);

    ConfigKey<Boolean> DONT_REQUIRE_TTY_FOR_SUDO = ConfigKeys.newBooleanConfigKey("dontRequireTtyForSudo",
            "Whether to explicitly set /etc/sudoers, so don't need tty (will leave unchanged if 'false'); " +
            "some machines require a tty for sudo; brooklyn by default does not use a tty " +
            "(so that it can get separate error+stdout streams); you can enable a tty as an " +
            "option to every ssh command, or you can do it once and " +
            "modify the machine so that a tty is not subsequently required.",
            false);

    @SetFromFlag("substitutions")
    MapConfigKey<Object> TEMPLATE_SUBSTITUTIONS = new MapConfigKey.Builder<Object>(Object.class, "template.substitutions")
            .description("Map of values to be substituted for the keys in any templated files used by the entity")
            .defaultValue(ImmutableMap.<String,Object>of())
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .build();

    /**
     * Files to be copied to the server before pre-install.
     * <p>
     * Map of {@code classpath://foo/file.txt} (or other url) source to destination path,
     * as {@code subdir/file} relative to installation directory or {@code /absolute/path/to/file}.
     *
     * @see #PRE_INSTALL_TEMPLATES
     */
    @Beta
    @SetFromFlag("preInstallFiles")
    MapConfigKey<String> PRE_INSTALL_FILES = new MapConfigKey.Builder<String>(String.class, "files.preinstall")
            .description("Files to be copied before pre-install; mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Templates to be filled in and then copied to the server before install.
     *
     * @see #PRE_INSTALL_FILES
     */
    @Beta
    @SetFromFlag("preInstallTemplates")
    MapConfigKey<String> PRE_INSTALL_TEMPLATES = new MapConfigKey.Builder<String>(String.class, "templates.preinstall")
            .description("Templated files to be filled in and copied before pre-install; mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Files to be copied to the server before install.
     * <p>
     * Map of {@code classpath://foo/file.txt} (or other url) source to destination path,
     * as {@code subdir/file} relative to installation directory or {@code /absolute/path/to/file}.
     *
     * @see #INSTALL_TEMPLATES
     */
    @Beta
    @SetFromFlag("installFiles")
    MapConfigKey<String> INSTALL_FILES = new MapConfigKey.Builder<String>(String.class, "files.install")
            .description("Files to be copied before install; mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Templates to be filled in and then copied to the server before install.
     *
     * @see #INSTALL_FILES
     */
    @Beta
    @SetFromFlag("installTemplates")
    MapConfigKey<String> INSTALL_TEMPLATES = new MapConfigKey.Builder<String>(String.class, "templates.install")
            .description("Templated files to be filled in and copied before install; mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Files to be copied to the server before customize.
     * <p>
     * Map of {@code classpath://foo/file.txt} (or other url) source to destination path,
     * as {@code subdir/file} relative to installation directory or {@code /absolute/path/to/file}.
     *
     * @see #INSTALL_TEMPLATES
     */
    @Beta
    @SetFromFlag("customizeFiles")
    MapConfigKey<String> CUSTOMIZE_FILES = new MapConfigKey.Builder<String>(String.class, "files.customize")
            .description("Files to be copied before customize; mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Templates to be filled in and then copied to the server before customize.
     *
     * @see #INSTALL_FILES
     */
    @Beta
    @SetFromFlag("customizeTemplates")
    MapConfigKey<String> CUSTOMIZE_TEMPLATES = new MapConfigKey.Builder<String>(String.class, "templates.customize")
            .description("Templated files to be filled in and copied before customize; mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Files to be copied to the server after customisation.
     * <p>
     * Map of {@code classpath://foo/file.txt} (or other url) source to destination path,
     * as {@code subdir/file} relative to runtime directory or {@code /absolute/path/to/file}.
     *
     * @see #RUNTIME_TEMPLATES
     */
    @Beta
    @SetFromFlag("runtimeFiles")
    MapConfigKey<String> RUNTIME_FILES = new MapConfigKey.Builder<String>(String.class, "files.runtime")
            .description("Files to be copied before launch (and before pre-launch); mapping from resource to the destination name relative to runDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    /**
     * Templates to be filled in and then copied to the server after customisation.
     *
     * @see #RUNTIME_FILES
     */
    @Beta
    @SetFromFlag("runtimeTemplates")
    MapConfigKey<String> RUNTIME_TEMPLATES = new MapConfigKey.Builder<String>(String.class, "templates.runtime")
            .description("Templated files to be filled in and copied before launch (and before pre-launch); mapping from resource to the destination name relative to installDir")
            .typeInheritance(BasicConfigInheritance.DEEP_MERGE)
            .runtimeInheritance(BasicConfigInheritance.NOT_REINHERITED_ELSE_DEEP_MERGE)
            .build();

    @SetFromFlag("provisioningProperties")
    MapConfigKey<Object> PROVISIONING_PROPERTIES = BrooklynConfigKeys.PROVISIONING_PROPERTIES;

    @SetFromFlag("maxRebindSensorsDelay")
    ConfigKey<Duration> MAXIMUM_REBIND_SENSOR_CONNECT_DELAY = ConfigKeys.newConfigKey(Duration.class,
            "softwareProcess.maxSensorRebindDelay",
            "The maximum delay to apply when reconnecting sensors when rebinding to this entity. " +
                    "Brooklyn will wait a random amount of time, up to the value of this config key, to " +
                    "avoid a thundering herd problem when the entity shares its machine with " +
                    "several others. Set to null or to 0 to disable any delay.",
            Duration.TEN_SECONDS);

    ConfigKey<Duration> SERVICE_PROCESS_IS_RUNNING_POLL_PERIOD = ConfigKeys.newConfigKey(Duration.class,
            "softwareProcess.serviceProcessIsRunningPollPeriod",
            "The period for polling for whether the process is running; applies only if the entity "
                    + "wires up the connectServiceUpIsRunning.",
            Duration.FIVE_SECONDS);
    
    /**
     * Sets the object that manages the sequence of calls of the entity's driver.
     */
    @Beta
    @SetFromFlag("lifecycleEffectorTasks")
    ConfigKey<SoftwareProcessDriverLifecycleEffectorTasks> LIFECYCLE_EFFECTOR_TASKS = ConfigKeys.newConfigKey(SoftwareProcessDriverLifecycleEffectorTasks.class,
            "softwareProcess.lifecycleTasks", "An object that handles lifecycle of an entity's associated machine.",
            new SoftwareProcessDriverLifecycleEffectorTasks());

    ConfigKey<Boolean> RETRIEVE_USAGE_METRICS = ConfigKeys.newBooleanConfigKey(
            "metrics.usage.retrieve",
            "Whether to retrieve the usage (e.g. performance) metrics",
            true);

    /** Controls the behavior when starting (stop, restart) {@link Startable} children as part of the start (stop, restart) effector on this entity
     * <p>
     * (NB: restarts are currently not propagated to children in the default {@link SoftwareProcess}
     * due to the various semantics which may be desired; this may change, but if entities have specific requirements for restart,
     * developers should either subclass the {@link SoftwareProcessDriverLifecycleEffectorTasks} and/or lean on sensors from the parent */
    enum ChildStartableMode {
        /** do nothing with {@link Startable} children */
        NONE(true, false, false),
        /** start (stop) {@link Startable} children concurrent with *driver* start (stop),
         * in foreground, so invoking entity will wait for children to complete.
         * <p>
         * if the child requires the parent to reach a particular state before acting,
         * when running in foreground the parent should communicate its state using sensors
         * which the child listens for.
         * note that often sensors at the parent are not activated until it is started,
         * so the usual sensors connected at an entity may not be available when running in this mode */
        FOREGROUND(false, false, false),
        /** as {@link #FOREGROUND} but {@link ChildStartableMode#isLate} */
        FOREGROUND_LATE(false, false, true),
        /** start {@link Startable} children concurrent with *driver* start (stop, restart),
         * but in background, ie disassociated from the effector task at this entity
         * (so that this entity can complete start/stop independent of children) */
        BACKGROUND(false, true, false),
        /** as {@link #BACKGROUND} but {@link ChildStartableMode#isLate} */
        BACKGROUND_LATE(false, true, true);

        /** whether starting (stopping, restarting) children is disabled */
        public final boolean isDisabled;
        /** whether starting (stopping, restarting) children is backgrounded, so parent should not wait on them */
        public final boolean isBackground;
        /** whether starting (stopping, restarting) children should be nested, so start occurs after the driver is started,
         * and stop before the driver is stopped (if false the children operations are concurrent with the parent),
         * (with restart always being done in parallel though this behaviour may change) */
        public final boolean isLate;

        private ChildStartableMode(boolean isDisabled, boolean isBackground, boolean isLate) {
            this.isDisabled = isDisabled;
            this.isBackground = isBackground;
            this.isLate = isLate;
        }

    }

    @SetFromFlag("childStartMode")
    ConfigKey<ChildStartableMode> CHILDREN_STARTABLE_MODE = ConfigKeys.newConfigKey(ChildStartableMode.class,
            "children.startable.mode", "Controls behaviour when starting Startable children as part of this entity's lifecycle.",
            ChildStartableMode.NONE);

    @SuppressWarnings("rawtypes")
    AttributeSensor<MachineProvisioningLocation> PROVISIONING_LOCATION = Sensors.newSensor(
            MachineProvisioningLocation.class, "softwareservice.provisioningLocation", "Location used to provision a machine where this is running");

    AttributeSensor<Boolean> SERVICE_PROCESS_IS_RUNNING = Sensors.newBooleanSensor("service.process.isRunning",
            "Whether the process for the service is confirmed as running");

    AttributeSensor<Lifecycle> SERVICE_STATE_ACTUAL = Attributes.SERVICE_STATE_ACTUAL;
    AttributeSensor<Transition> SERVICE_STATE_EXPECTED = Attributes.SERVICE_STATE_EXPECTED;

    AttributeSensor<String> PID_FILE = Sensors.newStringSensor("softwareprocess.pid.file", "PID file");

    @Beta
    public static class RestartSoftwareParameters {
        @Beta /** @since 0.7.0 semantics of parameters to restart being explored */
        public static final ConfigKey<Boolean> RESTART_CHILDREN = ConfigKeys.newConfigKey(Boolean.class, "restartChildren",
            "Whether to restart children; default false", false);

        @Beta /** @since 0.7.0 semantics of parameters to restart being explored */
        public static final ConfigKey<Object> RESTART_MACHINE = ConfigKeys.newConfigKey(Object.class, "restartMachine",
            "Whether to restart/replace the machine provisioned for this entity:  'true', 'false', or 'auto' are supported, "
            + "with the default being 'auto' which means to restart or reprovision the machine if there is no simpler way known to restart the entity "
            + "(for example, if the machine is unhealthy, it would not be possible to restart the process, not even via a stop-then-start sequence); "
            + "if the machine was not provisioned for this entity, this parameter has no effect",
            RestartMachineMode.AUTO.toString().toLowerCase());

        // we supply a typed variant for retrieval; we want the untyped (above) to use lower case as the default in the GUI
        // (very hard if using enum, since enum takes the name, and RendererHints do not apply to parameters)
        @Beta /** @since 0.7.0 semantics of parameters to restart being explored */
        public static final ConfigKey<RestartMachineMode> RESTART_MACHINE_TYPED = ConfigKeys.newConfigKey(RestartMachineMode.class, "restartMachine");

        public enum RestartMachineMode { TRUE, FALSE, AUTO }
    }

    @Beta
    public static class StopSoftwareParameters {
        //IF_NOT_STOPPED includes STARTING, STOPPING, RUNNING
        public enum StopMode { ALWAYS, IF_NOT_STOPPED, NEVER };

        @Beta /** @since 0.7.0 semantics of parameters to restart being explored */
        public static final ConfigKey<StopMode> STOP_PROCESS_MODE = ConfigKeys.newConfigKey(StopMode.class, "stopProcessMode",
                "When to stop the process with regard to the entity state. " +
                "ALWAYS will try to stop the process even if the entity is marked as stopped, " +
                "IF_NOT_STOPPED stops the process only if the entity is not marked as stopped, " +
                "NEVER doesn't stop the process.", StopMode.IF_NOT_STOPPED);

        @Beta /** @since 0.7.0 semantics of parameters to restart being explored */
        public static final ConfigKey<StopMode> STOP_MACHINE_MODE = ConfigKeys.newConfigKey(StopMode.class, "stopMachineMode",
                "When to stop the machine with regard to the entity state. " +
                "ALWAYS will try to stop the machine even if the entity is marked as stopped, " +
                "IF_NOT_STOPPED stops the machine only if the entity is not marked as stopped, " +
                "NEVER doesn't stop the machine.", StopMode.IF_NOT_STOPPED);
    }

    // NB: the START, STOP, and RESTART effectors themselves are (re)defined by MachineLifecycleEffectorTasks

    /**
     * @since 0.8.0
     */
    @Effector(description="Populates the attribute service.notUp.diagnostics, with any available health indicators")
    @Beta
    void populateServiceNotUpDiagnostics();
}
