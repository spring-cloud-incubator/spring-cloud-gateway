/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.handler;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.test.BaseWebClientTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.cloud.gateway.test.BaseWebClientTests.DefaultTestConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {})
@DirtiesContext
public class CustomizeRouteResolveHandlerMappingTest extends AbstractCustomizeRouteResolveHandlerMapping {

	@Test
	public void requestsToCustomizeRoute() {
	}

	protected String resolveRouteId(ServerWebExchange serverWebExchange) {
		return "one_customize_route";
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@Import(DefaultTestConfig.class)
	public static class TestConfig {

		@Bean
		RouteDefinitionLocator myRouteDefinitionLocator() {
			RouteDefinition definition = new RouteDefinition();
			definition.setId("one_customize_route");
			definition.setUri(URI.create("https://oneapi"));
			return () -> Flux.just(definition);
		}

	}
}
