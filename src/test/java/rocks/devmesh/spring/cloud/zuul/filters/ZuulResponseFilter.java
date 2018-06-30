/*
 * Copyright 2018 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package rocks.devmesh.spring.cloud.zuul.filters;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.SessionContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.filters.http.HttpOutboundSyncFilter;
import com.netflix.zuul.message.Headers;
import com.netflix.zuul.message.http.HttpResponseMessage;
import com.netflix.zuul.niws.RequestAttempts;
import com.netflix.zuul.passport.CurrentPassport;
import com.netflix.zuul.stats.status.StatusCategory;
import com.netflix.zuul.stats.status.StatusCategoryUtils;
import lombok.extern.slf4j.Slf4j;

import static com.netflix.zuul.constants.ZuulHeaders.CONNECTION;
import static com.netflix.zuul.constants.ZuulHeaders.KEEP_ALIVE;
import static com.netflix.zuul.constants.ZuulHeaders.X_ORIGINATING_URL;
import static com.netflix.zuul.constants.ZuulHeaders.X_ZUUL;
import static com.netflix.zuul.constants.ZuulHeaders.X_ZUUL_ERROR_CAUSE;
import static com.netflix.zuul.constants.ZuulHeaders.X_ZUUL_FILTER_EXECUTION_STATUS;
import static com.netflix.zuul.constants.ZuulHeaders.X_ZUUL_INSTANCE;
import static com.netflix.zuul.constants.ZuulHeaders.X_ZUUL_PROXY_ATTEMPTS;
import static com.netflix.zuul.constants.ZuulHeaders.X_ZUUL_STATUS;

/**
 * Sample Response Filter - adding custom response headers for better analysis of how the request was proxied
 * <p>
 * Author: Arthur Gonigberg
 * Date: December 21, 2017
 */
@Slf4j
public class ZuulResponseFilter extends HttpOutboundSyncFilter {

    private static final DynamicBooleanProperty SEND_RESPONSE_HEADERS =
            new DynamicBooleanProperty("zuul.responseFilter.send.headers", true);

    @Override
    public int filterOrder() {
        return 999;
    }

    @Override
    public boolean shouldFilter(HttpResponseMessage request) {
        return true;
    }

    @Override
    public HttpResponseMessage apply(HttpResponseMessage response) {
        SessionContext context = response.getContext();

        if (SEND_RESPONSE_HEADERS.get()) {
            Headers headers = response.getHeaders();

            StatusCategory statusCategory = StatusCategoryUtils.getStatusCategory(response);
            if (statusCategory != null) {
                headers.set(X_ZUUL_STATUS, statusCategory.name());
            }

            RequestAttempts attempts = RequestAttempts.getFromSessionContext(response.getContext());
            String headerStr = "";
            if (attempts != null) {
                headerStr = attempts.toString();
            }
            headers.set(X_ZUUL_PROXY_ATTEMPTS, headerStr);

            headers.set(X_ZUUL, "zuul");
            headers.set(X_ZUUL_INSTANCE, System.getenv("EC2_INSTANCE_ID"));
            headers.set(CONNECTION, KEEP_ALIVE);
            headers.set(X_ZUUL_FILTER_EXECUTION_STATUS, context.getFilterExecutionSummary().toString());
            headers.set(X_ORIGINATING_URL, response.getInboundRequest().reconstructURI());

            if (response.getStatus() >= 400 && context.getError() != null) {
                Throwable error = context.getError();
                headers.set(X_ZUUL_ERROR_CAUSE,
                        error instanceof ZuulException ? ((ZuulException) error).getErrorCause() : "UNKNOWN_CAUSE");
            }

            if (response.getStatus() >= 500) {
                log.info("Passport: {}", CurrentPassport.fromSessionContext(context));
            }
        }

        if (context.debugRequest()) {
            Debug.getRequestDebug(context).forEach(s -> log.info("REQ_DEBUG: " + s));
            Debug.getRoutingDebug(context).forEach(s -> log.info("ZUUL_DEBUG: " + s));
        }

        return response;
    }
}
