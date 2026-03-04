package com.aterrizar.service.external.homeoffice;

public record EtaRequest(
        String passportNumber,
        String destination
) {}