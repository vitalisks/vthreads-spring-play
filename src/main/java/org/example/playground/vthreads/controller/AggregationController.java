package org.example.playground.vthreads.controller;

import org.example.playground.vthreads.dto.ResponseModel;
import org.example.playground.vthreads.dto.params.AggregationParams;
import org.example.playground.vthreads.service.AggregationService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AggregationController {
    private final Validator validator;
    private final AggregationService aggregationService;

    @GetMapping("/aggregation")
    ResponseModel testRoute(
            @RequestParam(required = false) List<String> service1Query,
            @RequestParam(required = false) List<String> service2Query,
            @RequestParam(required = false) List<String> service3Query
    ) {

        var aggregationParams = new AggregationParams(service1Query, service2Query, service3Query);
        log.info("Received request {}", aggregationParams);
        validate(aggregationParams);
        var response = aggregationService.aggregate(aggregationParams);
        log.debug("Response completed {}", response);
        log.info("Response completed");
        return response;
    }

    private void validate(AggregationParams aggregationParams) {
        var validate = validator.validate(aggregationParams);
        if (!validate.isEmpty()) {
            throw new ConstraintViolationException(validate);
        }
    }
}
