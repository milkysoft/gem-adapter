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

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Identities;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import java.util.Map;
import java.util.Optional;
import org.cactoos.text.Base64Decoded;

/**
 * {@link Identities} implementation for gem api key decoding.
 * @since 0.4
 */
public final class GemApiKeyIdentities implements Identities {

    /**
     * Concrete implementation for User Identification.
     */
    private final Authentication auth;

    /**
     * Ctor.
     * @param auth Concrete implementation for User Identification.
     */
    public GemApiKeyIdentities(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public Optional<Authentication.User> user(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        return new RqHeaders(headers, Authorization.NAME).stream()
            .findFirst()
            .map(
                str -> {
                    final String res;
                    final String basic = "Basic ";
                    if (str.startsWith(basic)) {
                        res = str.substring(basic.length());
                    } else {
                        res = str;
                    }
                    return res;
                }
            )
            .map(Base64Decoded::new)
            .map(dec -> dec.toString().split(":"))
            .flatMap(
                cred -> this.auth.user(cred[0].trim(), cred[1].trim())
            );
    }
}
