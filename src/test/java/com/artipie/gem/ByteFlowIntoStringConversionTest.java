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

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * A test for {@link ByteFlowIntoStringConversion}.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ByteFlowIntoStringConversionTest {

    @Test
    public void simple() {
        final Publisher<ByteBuffer> flow = Flowable.fromArray(
            ByteBuffer.wrap("123".getBytes()),
            ByteBuffer.wrap("\n".getBytes()),
            ByteBuffer.wrap("123".getBytes()),
            ByteBuffer.wrap("\n".getBytes()),
            ByteBuffer.wrap("123".getBytes())
        );
        MatcherAssert.assertThat(
            new ByteFlowIntoStringConversion(flow).string().blockingGet(),
            new IsEqual<>("123\n123\n123")
        );
    }
}
