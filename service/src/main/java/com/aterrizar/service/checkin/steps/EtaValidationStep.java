package com.aterrizar.service.checkin.steps;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.aterrizar.service.checkin.feature.EtaFeature;
import com.aterrizar.service.core.framework.flow.Step;
import com.aterrizar.service.core.framework.flow.StepResult;
import com.aterrizar.service.core.model.Context;
import com.aterrizar.service.external.homeoffice.EtaRequest;
import com.aterrizar.service.external.homeoffice.EtaResponse;
import com.aterrizar.service.external.homeoffice.HomeOfficeHttpClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EtaValidationStep implements Step {

    private final EtaFeature etaFeature;
    private final HomeOfficeHttpClient homeOfficeHttpClient;

    @Override
    public boolean when(Context context) {
        var session = context.session();
        var userInfo = session.userInformation();

        if (context.countryCode() == null) {
            return false;
        }

        String countryCode = context.countryCode().name();

        boolean isCountryEnabled = etaFeature.isCountryAvailable(countryCode);

        boolean hasPassport =
                Optional.ofNullable(userInfo).map(info -> info.passportNumber()).isPresent();

        return isCountryEnabled && hasPassport;
    }

    @Override
    public StepResult onExecute(Context context) {
        var session = context.session();
        var userInfo = session.userInformation();

        String passportNumber = userInfo.passportNumber();
        String destinationCode = context.countryCode().name();

        destinationCode = etaFeature.normalizeDestinationCode(destinationCode);

        try {
            EtaResponse response =
                    homeOfficeHttpClient.validateEta(
                            new EtaRequest(passportNumber, destinationCode));

            if ("Pending".equalsIgnoreCase(response.status())) {
                return StepResult.success(markManualReview(context, true));
            }

            if ("Rejected".equalsIgnoreCase(response.status())) {
                throw new IllegalStateException("ETA validation rejected by Home Office");
            }

            return StepResult.success(markManualReview(context, false));

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Fallo técnico en sesión [" + session.sessionId() + "]: " + e.getMessage(), e);
        }
    }

    private Context markManualReview(Context context, boolean isRequired) {
        return context.withSessionData(builder -> builder.etaManualReviewRequired(isRequired));
    }
}
