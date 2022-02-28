/*
 * Copyright 2022 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.clouddriver.google.security

import com.netflix.spinnaker.clouddriver.config.AbstractBootstrapCredentialsConfigurationProvider
import com.netflix.spinnaker.clouddriver.google.GoogleOperation
import com.netflix.spinnaker.clouddriver.google.config.GoogleConfigurationProperties
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.config.GoogleConfiguration
import com.netflix.spinnaker.kork.configserver.CloudConfigResourceService
import com.netflix.spinnaker.kork.secrets.SecretManager
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext

/*
* If a configuration properties file has a large number of google accounts, as-is SpringBoot
* implementation of properties binding is inefficient. Hence, a logic for binding just the
* {@link GoogleCommonManagedAccount} is written but it still uses SpringBoot's Binder class.
* {@link GoogleAccountsConfigurationProvider} class fetches the flattened google account
* properties from Spring Cloud Config's BootstrapPropertySource and creates an {@link
* GoogleCommonManagedAccount} object.
*/
@Slf4j
@GoogleOperation(AtomicOperations.UPDATE_LAUNCH_CONFIG)
class GoogleAccountsConfigurationProvider extends AbstractBootstrapCredentialsConfigurationProvider {

  @Autowired
  private GoogleConfiguration googleConfiguration

  GoogleAccountsConfigurationProvider(
     ConfigurableApplicationContext applicationContext,
     CloudConfigResourceService configResourceService,
     SecretManager secretManager) {
   super(applicationContext, configResourceService, secretManager)
  }

  GoogleConfigurationProperties getConfigurationProperties() {
    return googleConfiguration.googleConfigurationProperties()
  }
}
