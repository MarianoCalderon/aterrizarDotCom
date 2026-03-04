package com.aterrizar.service.external.homeoffice;

public interface HomeOfficeHttpClient {
    EtaResponse validateEta(EtaRequest request);
}
