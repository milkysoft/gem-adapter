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

import com.artipie.gem.GemMeta.MetaFormat;
import com.artipie.gem.GemMeta.MetaInfo;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * New JSON format for Gem meta info.
 * @since 1.0
 * @todo #122:30min Add tests for this class.
 *  Check if it can add plain string values to JSON,
 *  and check it can support nested tree structures formatting.
 */
public final class JsonMetaFormat implements MetaFormat {

    /**
     * JSON builder.
     */
    private final JsonObjectBuilder builder;

    /**
     * New JSON format.
     * @param builder JSON builder
     */
    public JsonMetaFormat(final JsonObjectBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void print(final String name, final String value) {
        this.builder.add(name, value);
    }

    @Override
    public void print(final String name, final MetaInfo value) {
        final JsonObjectBuilder child = Json.createObjectBuilder();
        value.print(new JsonMetaFormat(child));
        this.builder.add(name, child);
    }
}
