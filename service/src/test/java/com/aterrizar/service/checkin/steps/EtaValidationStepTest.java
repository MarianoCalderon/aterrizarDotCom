package com.aterrizar.service.checkin.steps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aterrizar.service.checkin.feature.EtaFeature;
import com.aterrizar.service.core.framework.flow.StepResult;
import com.aterrizar.service.core.model.Context;
import com.aterrizar.service.core.model.session.Session;
import com.aterrizar.service.core.model.session.SessionData;
import com.aterrizar.service.core.model.session.UserInformation;
import com.aterrizar.service.external.homeoffice.EtaRequest;
import com.aterrizar.service.external.homeoffice.EtaResponse;
import com.aterrizar.service.external.homeoffice.HomeOfficeHttpClient;
import com.neovisionaries.i18n.CountryCode;

@ExtendWith(MockitoExtension.class)
class EtaValidationStepTest {

    @Mock private HomeOfficeHttpClient homeOfficeHttpClient;

    @Mock private Context context;

    @Mock private Session session;

    @Mock private SessionData sessionData;

    @Mock private UserInformation userInformation;

    @Mock private EtaFeature etaFeature;

    private EtaValidationStep etaValidationStep;

    @BeforeEach
    void setUp() {
        etaValidationStep = new EtaValidationStep(etaFeature, homeOfficeHttpClient);

        when(etaFeature.normalizeDestinationCode(any())).thenAnswer(i -> i.getArgument(0));

        when(context.session()).thenReturn(session);
        when(session.userInformation()).thenReturn(userInformation);
    }

    @ParameterizedTest(name = "ETA Rejected")
    @EnumSource(
            value = CountryCode.class,
            names = {"GB", "CH", "SE"})
    void shouldThrowExceptionWhenApiReturnsRejected(CountryCode countryCode) {
        when(context.countryCode()).thenReturn(countryCode);
        when(userInformation.passportNumber()).thenReturn("MEX9876541");

        when(homeOfficeHttpClient.validateEta(any(EtaRequest.class)))
                .thenReturn(new EtaResponse("Rejected"));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            etaValidationStep.onExecute(context);
                        });

        assertEquals("ETA validation rejected by Home Office", exception.getMessage());
    }

    @ParameterizedTest(name = "ETA Pending")
    @EnumSource(
            value = CountryCode.class,
            names = {"GB", "CH", "SE"})
    void shouldMarkSessionAsPendingWhenApiReturnsPending(CountryCode countryCode) {
        when(context.countryCode()).thenReturn(countryCode);
        when(userInformation.passportNumber()).thenReturn("MEX9876542");

        when(homeOfficeHttpClient.validateEta(any(EtaRequest.class)))
                .thenReturn(new EtaResponse("Pending"));

        when(context.withSessionData(any(Consumer.class))).thenReturn(context);

        StepResult result = etaValidationStep.onExecute(context);

        assertTrue(result.isSuccess());
        verify(context).withSessionData(any(Consumer.class));
    }

    @ParameterizedTest(name = "ETA Accepted")
    @EnumSource(
            value = CountryCode.class,
            names = {"GB", "CH", "SE"})
    void shouldNotMarkManualReviewWhenApiReturnsAccepted(CountryCode countryCode) {
        when(context.countryCode()).thenReturn(countryCode);
        when(userInformation.passportNumber()).thenReturn("MEX9876540");

        when(homeOfficeHttpClient.validateEta(any(EtaRequest.class)))
                .thenReturn(new EtaResponse("Accepted"));

        when(context.withSessionData(any(Consumer.class))).thenReturn(context);

        StepResult result = etaValidationStep.onExecute(context);

        assertTrue(result.isSuccess());
        verify(context).withSessionData(any(Consumer.class));
    }
}
