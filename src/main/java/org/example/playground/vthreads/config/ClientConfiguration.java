package org.example.playground.vthreads.config;

import org.example.playground.vthreads.config.properties.ServiceConfiguration;
import org.example.playground.vthreads.config.properties.Service2Configuration;
import org.example.playground.vthreads.config.properties.Service1Configuration;
import org.example.playground.vthreads.config.properties.Service3Configuration;
import org.example.playground.vthreads.dto.ResponseModel.Service2Model;
import org.example.playground.vthreads.dto.ResponseModel.Service1Model;
import org.example.playground.vthreads.dto.ResponseModel.Service3Model;
import org.example.playground.vthreads.service.client.ServiceConsumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

@Configuration
@EnableConfigurationProperties({Service2Configuration.class, Service1Configuration.class, Service3Configuration.class})
public class ClientConfiguration {
    @Bean
    public ServiceConsumer<Service1Model> service1Client(RestClient.Builder clientBuilder, @Validated Service1Configuration configuration) {
        return configureClient(clientBuilder, configuration, Service1Model.class);
    }

    @Bean
    public ServiceConsumer<Service2Model> service2Client(Builder clientBuilder, Service2Configuration configuration) {
        return configureClient(clientBuilder, configuration, Service2Model.class);
    }

    @Bean
    public ServiceConsumer<Service3Model> service3Client(Builder clientBuilder, Service3Configuration configuration) {
        return configureClient(clientBuilder, configuration, Service3Model.class);
    }

    private <U> ServiceConsumer<U> configureClient(RestClient.Builder clientBuilder, ServiceConfiguration configuration, Class<U> clazz) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(configuration.getConnectTimeout());
        requestFactory.setReadTimeout(configuration.getRequestTimeout());

        return ServiceConsumer.createClient(
                clientBuilder.baseUrl(configuration.getBaseServiceEndpoint())
                             .requestFactory(requestFactory)
                             .build(),
                clazz
        );
    }
}
