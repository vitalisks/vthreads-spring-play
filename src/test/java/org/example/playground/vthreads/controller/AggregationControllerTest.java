package org.example.playground.vthreads.controller;


import org.example.playground.vthreads.dto.ResponseModel;
import org.example.playground.vthreads.service.AggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
class AggregationControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private AggregationService aggregationService;

    private MockMvc mockMvc;

    private static final String CONTENT_TYPE = "application/json";

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @Test
    void isBadRequest() throws Exception {
        mockMvc.perform(get("/aggregation"))
               .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"service1Query=123", "service2Query=123", "service3Query=123"})
    void isOk(String queryParam) throws Exception {
        doReturn(ResponseModel.builder()
                              .service1(new ResponseModel.Service1Model(Map.of("s", List.of("a", "b"))))
                              .service2(new ResponseModel.Service2Model(Map.of("p", 1.0)))
                              .service3(new ResponseModel.Service3Model(Map.of("t", "zz")))
                              .build()).when(aggregationService).aggregate(any());
        mockMvc.perform(get("/aggregation?%s".formatted(queryParam)))
               .andExpect(status().isOk())
               .andExpect(content().json(
                       """
                               {
                                 "service2": {
                                   "p": 1
                                 },
                                 "service1": {
                                   "s": [
                                     "a",
                                     "b"
                                   ]
                                 },
                                 "service3": {
                                   "t": "zz"
                                 }
                               }
                               """, true
               ))
        ;
    }


}