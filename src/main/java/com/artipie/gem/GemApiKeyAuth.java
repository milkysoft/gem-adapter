/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.gem;

import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.cactoos.text.Base64Decoded;

/**
 * {@link AuthScheme} implementation for gem api key decoding.
 * @since 0.4
 */
public final class GemApiKeyAuth implements AuthScheme {

    /**
     * Concrete implementation for User Identification.
     */
    private final Authentication auth;

    /**
     * Ctor.
     * @param auth Concrete implementation for User Identification.
     */
    public GemApiKeyAuth(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public CompletionStage<Result> authenticate(final Iterable<Map.Entry<String, String>> headers) {
        return new RqHeaders(headers, Authorization.NAME).stream()
            .findFirst()
            .map(
                str -> {
                    final CompletionStage<Result> res;
                    if (str.startsWith(BasicAuthScheme.NAME)) {
                        res = new BasicAuthScheme(this.auth).authenticate(headers);
                    } else {
                        res = CompletableFuture.completedFuture(
                            Optional.of(str)
                                .map(Base64Decoded::new)
                                .map(dec -> dec.toString().split(":"))
                                .flatMap(
                                    cred -> this.auth.user(cred[0].trim(), cred[1].trim())
                                )
                                .<Result>map(SuccessByToken::new)
                                .orElseGet(FailureByToken::new)
                        );
                    }
                    return res;
                }
            )
            .get();
    }

    /**
     * Successful result with authenticated user.
     *
     * @since 0.5.4
     */
    private static class SuccessByToken implements AuthScheme.Result {

        /**
         * Authenticated user.
         */
        private final Authentication.User usr;

        /**
         * Ctor.
         *
         * @param user Authenticated user.
         */
        SuccessByToken(final Authentication.User user) {
            this.usr = user;
        }

        @Override
        public Optional<Authentication.User> user() {
            return Optional.of(this.usr);
        }

        @Override
        public String challenge() {
            return "";
        }
    }

    /**
     * Failed result without authenticated user.
     *
     * @since 0.5.4
     */
    private static class FailureByToken implements AuthScheme.Result {

        @Override
        public Optional<Authentication.User> user() {
            return Optional.empty();
        }

        @Override
        public String challenge() {
            return "";
        }
    }
}
