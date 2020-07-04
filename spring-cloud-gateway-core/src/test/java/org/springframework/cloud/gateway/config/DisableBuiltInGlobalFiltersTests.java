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

package org.springframework.cloud.gateway.config;

import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RemoveCachedBodyFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class DisableBuiltInGlobalFiltersTests {

	@EnableAutoConfiguration
	@SpringBootConfiguration
	protected static class Config {

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = Config.class)
	public static class GlobalFilterDefault {

		@Autowired
		private List<GlobalFilter> globalFilters;

		@Test
		public void shouldInjectBuiltInFilters() {
			assertThat(globalFilters).hasSizeGreaterThanOrEqualTo(10);
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = Config.class,
			properties = { "spring.cloud.gateway.RemoveCachedBody.enabled=false",
					"spring.cloud.gateway.RouteToRequestUrl.enabled=false" })
	@ActiveProfiles("disable-components")
	public static class DisableSpecificsFiltersByProperty {

		@Autowired
		private List<GlobalFilter> globalFilters;

		@Test
		public void shouldInjectOnlyEnabledBuiltInFilters() {
			assertThat(globalFilters).hasSizeGreaterThan(0);
			assertThat(globalFilters)
					.allSatisfy(filter -> assertThat(filter).isNotInstanceOfAny(
							RemoveCachedBodyFilter.class, RouteToRequestUrlFilter.class));
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = Config.class,
			properties = { "spring.cloud.gateway.AdaptCachedBody.enabled=false",
					"spring.cloud.gateway.RemoveCachedBody.enabled=false",
					"spring.cloud.gateway.RouteToRequestUrl.enabled=false",
					"spring.cloud.gateway.ForwardRouting.enabled=false",
					"spring.cloud.gateway.ForwardPath.enabled=false",
					"spring.cloud.gateway.WebsocketRouting.enabled=false",
					"spring.cloud.gateway.WeightRoute.enabled=false",
					"spring.cloud.gateway.Netty.enabled=false",
					"spring.cloud.gateway.ReactiveLoadBalancerClient.enabled=false",
					"spring.cloud.gateway.metrics.enabled=false" })
	@ActiveProfiles("disable-components")
	public static class DisableAllGlobalFiltersByProperty {

		@Autowired(required = false)
		private List<GlobalFilter> globalFilters;

		@Test
		public void shouldDisableAllBuiltInFilters() {
			assertThat(globalFilters).isNull();
		}

	}

}
