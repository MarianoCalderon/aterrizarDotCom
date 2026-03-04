package com.aterrizar.service.checkin.steps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private EtaValidationStep etaValidationStep;

    @BeforeEach
    void setUp() {
        etaValidationStep = new EtaValidationStep(List.of("UK", "CH", "SE"), homeOfficeHttpClient);

        when(context.session()).thenReturn(session);
        when(session.userInformation()).thenReturn(userInformation);
        when(context.countryCode()).thenReturn(CountryCode.GB);
    }

    @Test
    void shouldThrowExceptionWhenApiReturnsRejected() {
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

    @Test
    void shouldMarkSessionAsPendingWhenApiReturnsPending() {

        when(userInformation.passportNumber()).thenReturn("MEX9876542");
        when(homeOfficeHttpClient.validateEta(any(EtaRequest.class)))
                .thenReturn(new EtaResponse("Pending"));

        when(context.withSessionData(any(Consumer.class))).thenReturn(context);

        StepResult result = etaValidationStep.onExecute(context);

        assertTrue(result.isSuccess());
        verify(context).withSessionData(any(Consumer.class));
    }

    @Test
    void shouldNotMarkManualReviewWhenApiReturnsAccepted() {
        when(userInformation.passportNumber()).thenReturn("MEX9876540");
        when(homeOfficeHttpClient.validateEta(any(EtaRequest.class)))
                .thenReturn(new EtaResponse("Accepted"));

        when(context.withSessionData(any(Consumer.class))).thenReturn(context);

        StepResult result = etaValidationStep.onExecute(context);

        assertTrue(result.isSuccess());
        verify(context).withSessionData(any(Consumer.class));
    }
}
