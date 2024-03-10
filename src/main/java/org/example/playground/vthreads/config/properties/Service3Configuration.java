package org.example.playground.vthreads.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "org.example.playground.vthreads.services.service3")
public class Service3Configuration extends ServiceConfiguration {

}
