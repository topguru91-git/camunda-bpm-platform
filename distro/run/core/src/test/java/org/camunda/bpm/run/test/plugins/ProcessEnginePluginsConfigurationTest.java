/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.run.test.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.camunda.bpm.run.CamundaBpmRun;
import org.camunda.bpm.run.property.CamundaBpmRunProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { CamundaBpmRun.class })
@ActiveProfiles(profiles = { "test-plugins" }, inheritProfiles = true)
public class ProcessEnginePluginsConfigurationTest {

  @Autowired
  protected CamundaBpmRunProperties properties;

  @Test
  public void shouldPickUpAllPluginConfigurations() {
    // given a CamundaBpmRunProperties instance
    String pluginOne = "org.camunda.bpm.run.test.plugins.TestFirstPlugin";
    String pluginTwo = "org.camunda.bpm.run.test.plugins.TestSecondPlugin";

    // then
    assertThat(properties.getPlugins()).hasSize(2);
    assertThat(properties.getPlugins().keySet())
        .contains(pluginOne, pluginTwo);

    Map<String, Object> firstPluginMap = properties.getPlugins().get(pluginOne);
    assertThat(firstPluginMap).hasSize(2);
    assertThat(firstPluginMap.keySet()).contains("parameterOne", "parameterTwo");
    assertThat(firstPluginMap.values()).contains("valueOne", true);

    Map<String, Object> secondPluginMap = properties.getPlugins().get(pluginTwo);
    assertThat(secondPluginMap).hasSize(3);
    assertThat(secondPluginMap.keySet())
        .contains("parameterOne", "parameterTwo", "parameterThree");
    assertThat(secondPluginMap.values()).contains(1.222, false, 123);
  }

}