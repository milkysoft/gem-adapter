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

import com.artipie.http.auth.Identities;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.rq.RequestLine;
import io.reactivex.Flowable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * A test for api key endpoint.
 *
 * @since 0.2
 * @checkstyle StringLiteralsConcatenationCheck (500 lines)
 */
public class ApiKeyTest {

    @Test
    public void keyIsReturned() {
        final String token = "aGVsbG86d29ybGQ=";
        final ArrayList<Map.Entry<String, String>> headers = new ArrayList<>(0);
        headers.add(new Authorization(String.format("Basic %s", token)));
        MatcherAssert.assertThat(
            new ApiKeySlice(Identities.ANONYMOUS).response(
                new RequestLine("GET", "/").toString(),
                headers,
                Flowable.empty()
            ), new RsHasBody(token.getBytes(StandardCharsets.UTF_8))
        );
    }
}
