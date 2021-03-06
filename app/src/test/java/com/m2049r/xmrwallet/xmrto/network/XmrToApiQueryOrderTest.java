/*
 * Copyright (c) 2017 m2049r et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.xmrto.network;

import com.m2049r.xmrwallet.xmrto.api.XmrToCallback;
import com.m2049r.xmrwallet.xmrto.XmrToError;
import com.m2049r.xmrwallet.xmrto.XmrToException;
import com.m2049r.xmrwallet.xmrto.api.QueryOrderStatus;
import com.m2049r.xmrwallet.xmrto.api.XmrToApi;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;

public class XmrToApiQueryOrderTest {

    static final SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    static Date ParseDate(String dateString) throws ParseException {
        return DATETIME_FORMATTER.parse(dateString.replaceAll("Z$", "+0000"));
    }


    private MockWebServer mockWebServer;

    private XmrToApi xmrToApi;

    private OkHttpClient okHttpClient = new OkHttpClient();
    private Waiter waiter;

    @Mock
    XmrToCallback<QueryOrderStatus> mockQueryXmrToCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);

        xmrToApi = new XmrToApiImpl(okHttpClient, mockWebServer.url("/"));
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void orderStatus_shouldBePostMethod()
            throws InterruptedException {

        xmrToApi.queryOrderStatus("xmrto - efMsiU", mockQueryXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
    }

    @Test
    public void orderStatus_shouldBeContentTypeJson()
            throws InterruptedException {

        xmrToApi.queryOrderStatus("xmrto - efMsiU", mockQueryXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"));
    }

    @Test
    public void orderStatus_shouldContainValidBody()
            throws InterruptedException {

        final String validBody = "{\"uuid\":\"xmrto - efMsiU\"}";

        xmrToApi.queryOrderStatus("xmrto - efMsiU", mockQueryXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertEquals(validBody, body);
    }

    @Test
    public void orderStatus_wasSuccessfulShouldRespondWithOrder()
            throws TimeoutException {

//TODO: state enum
// TODO dates are dates
        final String state = "UNPAID";
        final double btcAmount = 0.1;
        final String btcDestAddress = "1FhnVJi2V1k4MqXm2nHoEbY5LV7FPai7bb";
        final String uuid = "xmrto - efMsiU";
        final int btcNumConfirmationsBeforePurge = 144;
        final String createdAt = "2017-11-17T12:20:02Z";
        final String expiresAt = "2017-11-17T12:35:02Z";
        final int secondsTillTimeout = 882;
        final double xmrAmountTotal = 6.464;
        final double xmrAmountRemaining = 6.464;
        final int xmrNumConfirmationsRemaining = -1;
        final double xmrPriceBtc = 0.0154703;
        final String xmrReceivingSubaddress = "83BGzCTthheE2KxNTBPnPJjJUthYPfDfCf3ENSVQcpga8RYSxNz9qCz1qp9MLye9euMjckGi11cRdeVGqsVqTLgH8w5fJ1D";
        final int xmrRecommendedMixin = 5;

        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockQueryOrderResponse(
                        state,
                        btcAmount,
                        btcDestAddress,
                        uuid,
                        btcNumConfirmationsBeforePurge,
                        createdAt,
                        expiresAt,
                        secondsTillTimeout,
                        xmrAmountTotal,
                        xmrAmountRemaining,
                        xmrNumConfirmationsRemaining,
                        xmrPriceBtc,
                        xmrReceivingSubaddress,
                        xmrRecommendedMixin));
        mockWebServer.enqueue(jsonMockResponse);

        xmrToApi.queryOrderStatus(uuid, new XmrToCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(final QueryOrderStatus orderStatus) {
                waiter.assertEquals(orderStatus.getState().toString(), state);
                waiter.assertEquals(orderStatus.getBtcAmount(), btcAmount);
                waiter.assertEquals(orderStatus.getBtcDestAddress(), btcDestAddress);
                waiter.assertEquals(orderStatus.getUuid(), uuid);
                waiter.assertEquals(orderStatus.getBtcNumConfirmationsThreshold(), btcNumConfirmationsBeforePurge);
                try {
                    waiter.assertEquals(orderStatus.getCreatedAt(), ParseDate(createdAt));
                    waiter.assertEquals(orderStatus.getExpiresAt(), ParseDate(expiresAt));
                } catch (ParseException ex) {
                    waiter.fail(ex);
                }
                waiter.assertEquals(orderStatus.getSecondsTillTimeout(), secondsTillTimeout);
                waiter.assertEquals(orderStatus.getIncomingAmountTotal(), xmrAmountTotal);
                waiter.assertEquals(orderStatus.getRemainingAmountIncoming(), xmrAmountRemaining);
                waiter.assertEquals(orderStatus.getIncomingNumConfirmationsRemaining(), xmrNumConfirmationsRemaining);
                waiter.assertEquals(orderStatus.getIncomingPriceBtc(), xmrPriceBtc);
                waiter.assertEquals(orderStatus.getReceivingSubaddress(), xmrReceivingSubaddress);
                waiter.assertEquals(orderStatus.getRecommendedMixin(), xmrRecommendedMixin);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void orderStatus_wasNotSuccessfulShouldCallOnError()
            throws TimeoutException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        xmrToApi.queryOrderStatus("xmrto - efMsiU", new XmrToCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(final QueryOrderStatus orderStatus) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof XmrToException);
                waiter.assertTrue(((XmrToException) e).getCode() == 500);
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void orderStatus_orderNotFoundShouldCallOnError()
            throws TimeoutException {
        mockWebServer.enqueue(new MockResponse().
                setResponseCode(404).
                setBody("{\"error_msg\":\"requested order not found\",\"error\":\"XMRTO-ERROR-006\"}"));
        xmrToApi.queryOrderStatus("xmrto - efMsiU", new XmrToCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(final QueryOrderStatus orderStatus) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof XmrToException);
                XmrToException xmrEx = (XmrToException) e;
                waiter.assertTrue(xmrEx.getCode() == 404);
                waiter.assertNotNull(xmrEx.getError());
                waiter.assertEquals(xmrEx.getError().getErrorId(), XmrToError.Error.XMRTO_ERROR_006);
                waiter.assertEquals(xmrEx.getError().getErrorMsg(), "requested order not found");
                waiter.resume();
            }

        });
        waiter.await();
    }

    private String createMockQueryOrderResponse(
            final String state,
            final double btcAmount,
            final String btcDestAddress,
            final String uuid,
            final int btcNumConfirmationsBeforePurge,
            final String createdAt,
            final String expiresAt,
            final int secondsTillTimeout,
            final double xmrAmountTotal,
            final double xmrAmountRemaining,
            final int xmrNumConfirmationsRemaining,
            final double xmrPriceBtc,
            final String xmrReceivingSubaddress,
            final int xmrRecommendedMixin
    ) {
        return "{\n" +
                "    \"incoming_price_btc\": \"" + xmrPriceBtc + "\",\n" +
                "    \"uuid\":\"" + uuid + "\",\n" +
                "    \"state\":\"" + state + "\",\n" +
                "    \"btc_amount\":\"" + btcAmount + "\",\n" +
                "    \"btc_dest_address\":\"" + btcDestAddress + "\",\n" +
                "    \"receiving_subaddress\":\"" + xmrReceivingSubaddress + "\",\n" +
                "    \"created_at\":\"" + createdAt + "\",\n" +
                "    \"expires_at\":\"" + expiresAt + "\",\n" +
                "    \"seconds_till_timeout\":\"" + secondsTillTimeout + "\",\n" +
                "    \"incoming_amount_total\":\"" + xmrAmountTotal + "\",\n" +
                "    \"remaining_amount_incoming\":\"" + xmrAmountRemaining + "\",\n" +
                "    \"incoming_num_confirmations_remaining\":\"" + xmrNumConfirmationsRemaining + "\",\n" +
                "    \"recommended_mixin\":\"" + xmrRecommendedMixin + "\",\n" +
                "    \"btc_num_confirmations_threshold\":\"" + btcNumConfirmationsBeforePurge + "\",\n"
                + "}";
    }
}