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

package org.springframework.cloud.gateway.actuate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Spencer Gibb
 */
@RestControllerEndpoint(id = "gateway")
public class GatewayControllerEndpoint implements ApplicationEventPublisherAware {

	private static final Log log = LogFactory.getLog(GatewayControllerEndpoint.class);

	private List<GlobalFilter> globalFilters;

	private List<GatewayFilterFactory> gatewayFilters;

	private RouteDefinitionWriter routeDefinitionWriter;

	private RouteLocator routeLocator;

	private ApplicationEventPublisher publisher;

	public GatewayControllerEndpoint(List<GlobalFilter> globalFilters,
			List<GatewayFilterFactory> gatewayFilters,
			RouteDefinitionWriter routeDefinitionWriter, RouteLocator routeLocator) {
		this.globalFilters = globalFilters;
		this.gatewayFilters = gatewayFilters;
		this.routeDefinitionWriter = routeDefinitionWriter;
		this.routeLocator = routeLocator;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	// TODO: Add uncommited or new but not active routes endpoint

	@PostMapping("/refresh")
	public Mono<Void> refresh() {
		this.publisher.publishEvent(new RefreshRoutesEvent(this));
		return Mono.empty();
	}

	@GetMapping("/globalfilters")
	public Mono<HashMap<String, Object>> globalfilters() {
		return getNamesToOrders(this.globalFilters);
	}

	@GetMapping("/routefilters")
	public Mono<HashMap<String, Object>> routefilers() {
		return getNamesToOrders(this.gatewayFilters);
	}

	private <T> Mono<HashMap<String, Object>> getNamesToOrders(List<T> list) {
		return Flux.fromIterable(list).reduce(new HashMap<>(), this::putItem);
	}

	private HashMap<String, Object> putItem(HashMap<String, Object> map, Object o) {
		Integer order = null;
		if (o instanceof Ordered) {
			order = ((Ordered) o).getOrder();
		}
		// filters.put(o.getClass().getName(), order);
		map.put(o.toString(), order);
		return map;
	}

	// TODO: Flush out routes without a definition
	@GetMapping("/routes")
	public Flux<Map<String, Object>> routes() {
		return this.routeLocator.getRoutes().map(this::serialize);
	}

	Map<String, Object> serialize(Route route) {
		HashMap<String, Object> r = new HashMap<>();
		r.put("route_id", route.getId());
		r.put("uri", route.getUri().toString());
		r.put("order", route.getOrder());
		r.put("predicate", route.getPredicate().toString());

		ArrayList<String> filters = new ArrayList<>();

		for (int i = 0; i < route.getFilters().size(); i++) {
			GatewayFilter gatewayFilter = route.getFilters().get(i);
			filters.add(gatewayFilter.toString());
		}

		r.put("filters", filters);
		return r;
	}

	/*
	 * http POST :8080/admin/gateway/routes/apiaddreqhead uri=http://httpbin.org:80
	 * predicates:='["Host=**.apiaddrequestheader.org", "Path=/headers"]'
	 * filters:='["AddRequestHeader=X-Request-ApiFoo, ApiBar"]'
	 */
	@PostMapping("/routes/{id}")
	@SuppressWarnings("unchecked")
	public Mono<ResponseEntity<Void>> save(@PathVariable String id,
			@RequestBody Mono<RouteDefinition> route) {
		return this.routeDefinitionWriter.save(route.map(r -> {
			r.setId(id);
			log.debug("Saving route: " + route);
			return r;
		})).then(Mono.defer(() -> Mono
				.just(ResponseEntity.created(URI.create("/routes/" + id)).build())));
	}

	@DeleteMapping("/routes/{id}")
	public Mono<ResponseEntity<Object>> delete(@PathVariable String id) {
		return this.routeDefinitionWriter.delete(Mono.just(id))
				.then(Mono.defer(() -> Mono.just(ResponseEntity.ok().build())))
				.onErrorResume(t -> t instanceof NotFoundException,
						t -> Mono.just(ResponseEntity.notFound().build()));
	}

	@GetMapping("/routes/{id}")
	public Mono<ResponseEntity<Map<String, Object>>> route(@PathVariable String id) {
		return this.routeLocator.getRoutes()
				.filter(route -> route.getId().equals(id)).singleOrEmpty()
				.map(this::serialize)
				.map(ResponseEntity::ok)
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
	}

	@GetMapping("/routes/{id}/combinedfilters")
	public Mono<HashMap<String, Object>> combinedfilters(@PathVariable String id) {
		// TODO: missing global filters
		return this.routeLocator.getRoutes().filter(route -> route.getId().equals(id))
				.reduce(new HashMap<>(), this::putItem);
	}

}
