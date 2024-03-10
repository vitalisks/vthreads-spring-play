package org.example.playground.vthreads.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.util.LinkedMultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
@Jacksonized
public class ResponseModel {
    Service1Model service1;
    Service2Model service2;
    Service3Model service3;

    public static class Service1Model extends LinkedMultiValueMap<String, String> {
        public Service1Model() {
            super();
        }

        public Service1Model(Map<String, List<String>> otherMap) {
            super(otherMap);
        }
    }

    public static class Service2Model extends HashMap<String, Double> {

        public Service2Model() {
            super();
        }

        public Service2Model(Map<? extends String, ? extends Double> m) {
            super(m);
        }
    }

    public static class Service3Model extends HashMap<String, String> {
        public Service3Model() {
            super();
        }

        public Service3Model(Map<? extends String, ? extends String> m) {
            super(m);
        }
    }

}
