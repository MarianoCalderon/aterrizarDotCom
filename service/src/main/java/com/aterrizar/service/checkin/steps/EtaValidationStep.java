package com.aterrizar.service.checkin.steps;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aterrizar.service.core.framework.flow.Step;
import com.aterrizar.service.core.framework.flow.StepResult;
import com.aterrizar.service.core.model.Context;
import com.aterrizar.service.external.homeoffice.EtaRequest;
import com.aterrizar.service.external.homeoffice.EtaResponse;
import com.aterrizar.service.external.homeoffice.HomeOfficeHttpClient;

@Service
public class EtaValidationStep implements Step {

    private final List<String> enabledCountries;
    private final HomeOfficeHttpClient homeOfficeHttpClient;

    public EtaValidationStep(
            @Value("${feature.homeoffice.eta.enabled-countries}") List<String> enabledCountries,
            HomeOfficeHttpClient homeOfficeHttpClient) {
        this.enabledCountries = enabledCountries;
        this.homeOfficeHttpClient = homeOfficeHttpClient;
    }

    @Override
    public boolean when(Context context) {
        if (context.countryCode() == null) return false;

        String sessionCountry = context.countryCode().name();

        boolean isCountryEnabled =
                enabledCountries.contains(sessionCountry)
                        || ("GB".equals(sessionCountry) && enabledCountries.contains("UK"));

        boolean hasPassport =
                Optional.ofNullable(context.session().userInformation())
                        .map(userInfo -> userInfo.passportNumber())
                        .isPresent();

        return isCountryEnabled && hasPassport;
    }

    @Override
    public StepResult onExecute(Context context) {
        System.out.println("--- EJECUTANDO VALIDACION ETA ---");
        String passportNumber = context.session().userInformation().passportNumber();
        String destinationCode = context.countryCode().name();

        if ("GB".equals(destinationCode) && enabledCountries.contains("UK")) {
            destinationCode = "UK";
        }

        try {
            EtaResponse response =
                    homeOfficeHttpClient.validateEta(
                            new EtaRequest(passportNumber, destinationCode));
            System.out.println(
                    "DEBUG: El estatus que devolvió WireMock es: [" + response.status() + "]");

            if ("Pending".equalsIgnoreCase(response.status())) {
                System.out.println("Alerta PENDING pasaporte: " + passportNumber);
                return StepResult.success(markManualReview(context, true));
            }

            if ("Rejected".equalsIgnoreCase(response.status())) {
                throw new IllegalStateException("ETA validation rejected by Home Office");
            }

            return StepResult.success(markManualReview(context, false));

        } catch (IllegalStateException e) {
            System.err.println(
                    "FALLO Sesion: ["
                            + context.session().sessionId()
                            + "] - Pasaporte: ["
                            + passportNumber
                            + "]");
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Fallo técnico en sesión ["
                            + context.session().sessionId()
                            + "]: "
                            + e.getMessage(),
                    e);
        } /*catch (IllegalStateException e) {
              throw e;
          } catch (Exception e) {
              throw new IllegalStateException("ETA validation failed or was rejected: " + e.getMessage(), e);
          }*/
    }

    private Context markManualReview(Context context, boolean isRequired) {
        return context.withSessionData(builder -> builder.etaManualReviewRequired(isRequired));
    }
}
