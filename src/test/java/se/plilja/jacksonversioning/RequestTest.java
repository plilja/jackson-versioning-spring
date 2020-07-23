/*
 * The MIT License
 * Copyright © 2020 Patrik Lilja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.plilja.jacksonversioning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestTest {

    private TypeReference<HashMap<String, Object>> mapTypeReference = new TypeReference<HashMap<String, Object>>() {
    };

    @LocalServerPort
    int port;

    @Autowired
    private CarController carController;
    @Autowired
    private ObjectMapper objectMapper;

    RestTemplate restTemplate = new RestTemplate();

    @AfterEach
    void tearDown() {
        carController.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "V1|{\"id\":1,\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}",
            "V2|{\"id\":1,\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}",
            "V3|{\"id\":1,\"make\":\"Toyota\",\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"}}",
    })
    void getHistoricApiVersion(String parameters) throws Exception {
        String[] args = parameters.split("\\|");
        String version = args[0];
        String expectedMapString = args[1];
        Map<String, Object> actual = restTemplate.getForObject("http://localhost:{port}/cars/1?API_VERSION={version}", Map.class, port, version);
        Map<String, Object> expected = objectMapper.readValue(expectedMapString, mapTypeReference);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "V1|{\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}",
            "V2|{\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}",
            "V3|{\"make\":\"Toyota\",\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"}}",
    })
    void postHistoricApiVersion(String parameters) throws Exception {
        String expectedMapString = "{\"make\":\"Toyota\",\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"}}";
        String[] args = parameters.split("\\|");
        String version = args[0];
        String requestMapString = args[1];
        Map<String, Object> request = objectMapper.readValue(requestMapString, mapTypeReference);
        ResponseEntity<Map> postResponse = restTemplate.postForEntity("http://localhost:{port}/cars?API_VERSION={version}", request, Map.class, port, version);

        Object id = postResponse.getBody().remove("id");
        assertEquals(request, postResponse.getBody());

        Map<String, Object> getResponse = restTemplate.getForObject("http://localhost:{port}/cars/{id}?API_VERSION=V3", Map.class, port, id);
        Map<String, Object> expected = objectMapper.readValue(expectedMapString, mapTypeReference);
        expected.put("id", id);

        assertEquals(expected, getResponse);
    }

    @Test
    void getIllegalApiVersion() {
        HttpStatusCodeException ex = assertThrows(HttpStatusCodeException.class, () -> {
            restTemplate.getForObject("http://localhost:{port}/cars?API_VERSION=UNKNOWN", Car[].class, port);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}