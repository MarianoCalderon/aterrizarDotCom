package com.aterrizar.http.external.gateway.homeoffice;

import com.aterrizar.service.external.homeoffice.EtaRequest;
import com.aterrizar.service.external.homeoffice.EtaResponse;
import com.aterrizar.service.external.homeoffice.HomeOfficeHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class HomeOfficeHttpClientImpl implements HomeOfficeHttpClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HomeOfficeHttpClientImpl(
            RestTemplate restTemplate,
            @Value("${http.client.homeoffice.base.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public EtaResponse validateEta(EtaRequest request) {
        String url = baseUrl + "eta-validation";

        try {
            ResponseEntity<EtaResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    EtaResponse.class
            );
            return response.getBody();

        } catch (HttpClientErrorException.NotAcceptable e) {
            return new EtaResponse("Rejected");
        }
    }
}