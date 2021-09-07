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

import java.nio.file.Path;

/**
 * Gem metadata parser.
 * @since 1.0
 */
public interface GemMeta {

    /**
     * Extract Gem info.
     * @param gem Path to gem
     * @return JSON object
     */
    MetaInfo info(Path gem);

    /**
     * Extract Gem dependencies.
     * @param gempath Path to gem
     * @return Bytes object
     */
    byte[] dependencies(Path gempath);

    /**
     * Gem info metadata format.
     * @since 1.0
     */
    interface MetaFormat {

        /**
         * Print info string.
         * @param name Key
         * @param value String
         */
        void print(String name, String value);

        /**
         * Print info child.
         * @param name Key
         * @param value Node
         */
        void print(String name, MetaInfo value);
    }

    /**
     * Metadata info.
     * @since 1.0
     */
    interface MetaInfo {
        /**
         * Print meta info using format.
         * @param fmt Meta format
         */
        void print(MetaFormat fmt);
    }
}
