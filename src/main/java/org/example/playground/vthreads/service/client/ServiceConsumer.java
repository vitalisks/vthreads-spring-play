package org.example.playground.vthreads.service.client;

import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

public interface ServiceConsumer<U> {
    U retrieveByQuery(List<String> query);

    static <U> ServiceConsumer<U> createClient(RestClient restClient, Class<U> clazz) {
        return args -> restClient.get()
                                 .uri(query(args))
                                 .retrieve()
                                 .body(clazz);
    }

    static Function<UriBuilder, URI> query(List<String> q) {
        return uriBuilder -> uriBuilder.queryParam("q", String.join(",", q)).build();
    }
}
