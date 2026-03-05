package com.aterrizar.service.checkin.feature;

public interface EtaFeature {
    boolean isCountryAvailable(String countryCode);

    String normalizeDestinationCode(String countryCode);
}
