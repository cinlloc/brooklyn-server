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

import com.google.common.base.Joiner;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.entity.stock.BasicApplication;
import org.apache.brooklyn.location.winrm.WinRmMachineLocation;
import org.apache.brooklyn.util.core.internal.winrm.RecordingWinRmTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class WinRmMachineLocationExternalConfigYamlTest extends AbstractYamlTest {
    private static final Logger log = LoggerFactory.getLogger(WinRmMachineLocationExternalConfigYamlTest.class);

    @Override
    protected LocalManagementContext newTestManagementContext() {
        BrooklynProperties props = BrooklynProperties.Factory.newEmpty();
        props.put("brooklyn.external.inPlaceSupplier1", "org.apache.brooklyn.core.config.external.InPlaceExternalConfigSupplier");
        props.put("brooklyn.external.inPlaceSupplier1.byonPassword", "passw0rd");

        return LocalManagementContextForTests.builder(true)
                .useProperties(props)
                .enableOsgiReusable()
                .build();
    }

    @Test()
    public void testWindowsMachinesPasswordExternalProvider() throws Exception {
        RecordingWinRmTool.constructorProps.clear();
        final String yaml = Joiner.on("\n").join("location:",
                "  byon:",
                "    hosts:",
                "    - winrm: 127.0.0.1",
                "      user: admin",
                "      brooklyn.winrm.config.winrmToolClass: org.apache.brooklyn.util.core.internal.winrm.RecordingWinRmTool",
                "      password: $brooklyn:external(\"inPlaceSupplier1\", \"byonPassword\")",
                "      osFamily: windows",
                "services:",
                "- type: org.apache.brooklyn.entity.software.base.VanillaWindowsProcess",
                "  brooklyn.config:",
                "    launch.command: echo launch",
                "    checkRunning.command: echo running");

        BasicApplication app = (BasicApplication) createAndStartApplication(yaml);
        waitForApplicationTasks(app);
        assertEquals(RecordingWinRmTool.constructorProps.get(0).get(WinRmMachineLocation.PASSWORD.getName()), "passw0rd");
    }

    @Test()
    public void testWindowsMachinesNoPasswordExternalProvider() throws Exception {
        RecordingWinRmTool.constructorProps.clear();
        final String yaml = Joiner.on("\n").join("location:",
                "  byon:",
                "    hosts:",
                "    - winrm: 127.0.0.1",
                "      user: admin",
                "      brooklyn.winrm.config.winrmToolClass: org.apache.brooklyn.util.core.internal.winrm.RecordingWinRmTool",
                "      password: $brooklyn:external(\"inPlaceSupplier1\", \"byonPasswordddd\")",
                "      osFamily: windows",
                "services:",
                "- type: org.apache.brooklyn.entity.software.base.VanillaWindowsProcess",
                "  brooklyn.config:",
                "    launch.command: echo launch",
                "    checkRunning.command: echo running");

        BasicApplication app = (BasicApplication) createAndStartApplication(yaml);
        waitForApplicationTasks(app);
        assertNull(RecordingWinRmTool.constructorProps.get(0).get(WinRmMachineLocation.PASSWORD.getName()));
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
