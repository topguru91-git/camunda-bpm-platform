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
package org.camunda.bpm.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.cfg.CompositeProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.util.ReflectUtil;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;
import org.camunda.bpm.run.property.CamundaBpmRunLdapProperties;
import org.camunda.bpm.run.property.CamundaBpmRunProperties;
import org.camunda.bpm.run.utils.CamundaBpmRunLogger;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaDeploymentConfiguration;
import org.camunda.bpm.spring.boot.starter.util.SpringBootStarterException;
import org.camunda.bpm.spring.boot.starter.util.SpringBootStarterPropertyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(CamundaBpmRunProperties.class)
@Configuration
@AutoConfigureAfter({ CamundaBpmAutoConfiguration.class })
public class CamundaBpmRunConfiguration {

  protected static final CamundaBpmRunLogger LOG = CamundaBpmRunLogger.LOG;

  @Autowired
  CamundaBpmRunProperties camundaBpmRunProperties;

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = CamundaBpmRunLdapProperties.PREFIX)
  public LdapIdentityProviderPlugin ldapIdentityProviderPlugin() {
    return camundaBpmRunProperties.getLdap();
  }

  @Bean
  public ProcessEngineConfigurationImpl processEngineConfigurationImpl(List<ProcessEnginePlugin> processEnginePlugins) {
    final SpringProcessEngineConfiguration configuration = new CamundaBpmRunProcessEngineConfiguration();

    // register process engine plugins defined in yaml
    Map<String, Map<String, Object>> yamlPluginsInfo = camundaBpmRunProperties.getPlugins();
    initializePlugins(processEnginePlugins, yamlPluginsInfo);

    configuration.getProcessEnginePlugins().add(new CompositeProcessEnginePlugin(processEnginePlugins));
    return configuration;
  }

  @Bean
  public static CamundaDeploymentConfiguration camundaDeploymentConfiguration() {
    return new CamundaBpmRunDeploymentConfiguration();
  }

  protected List<ProcessEnginePlugin> initializePlugins(List<ProcessEnginePlugin> processEnginePlugins, Map<String, Map<String, Object>> pluginsInfo) {

    List<ProcessEnginePlugin> plugins = new ArrayList<>();

    for (Map.Entry<String, Map<String, Object>> entry : pluginsInfo.entrySet()) {
      String className = entry.getKey();
      ProcessEnginePlugin plugin = getOrCreatePluginInstance(processEnginePlugins, className);

      Map<String, Object> pluginParameters = entry.getValue();
      populatePluginInstance(plugin, pluginParameters);

      LOG.processEnginePluginRegistered(className);
    }

    return plugins;
  }

  protected ProcessEnginePlugin getOrCreatePluginInstance(List<ProcessEnginePlugin> processEnginePlugins, String className) {
    try {
      // find class on classpath
      Class<? extends ProcessEnginePlugin> pluginClass = ReflectUtil
          .loadClass(className, null, ProcessEnginePlugin.class);

      // check if an instance of the process engine plugin is already present
      Optional<ProcessEnginePlugin> plugin = processEnginePlugins.stream()
          .filter(p -> pluginClass.isInstance(p)).findFirst();

      // get existing plugin instance or create a new one and add it to the list
      return plugin.orElseGet(() -> {

        ProcessEnginePlugin newPlugin = ReflectUtil.createInstance(pluginClass);
        processEnginePlugins.add(newPlugin);

        return newPlugin;
      });

    } catch (ClassNotFoundException | ClassCastException | ProcessEngineException e) {
      throw LOG.failedProcessEnginePluginInstantiation(className, e);
    }
  }

  protected void populatePluginInstance(ProcessEnginePlugin plugin, Map<String, Object> properties) {
    try {
      SpringBootStarterPropertyHelper.applyProperties(plugin, properties, false);
    } catch (SpringBootStarterException e) {
      throw LOG.pluginPropertyNotFound(plugin.getClass().getCanonicalName(), "", e);
    }
  }
}
