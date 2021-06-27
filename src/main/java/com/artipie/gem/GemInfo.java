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
import java.util.List;
import javax.json.JsonObject;

/**
 * Gem info.
 *
 * @since 1.0
 */
public interface GemInfo {

    /**
     * Get Ruby specification for arbitrary gem.
     * @param gempath Full path to gem file or null
     * @return RubyObject specification
     */
    JsonObject getinfo(Path gempath);

    /**
     * Get Ruby specification for arbitrary gem.
     * @param gempaths Full paths to gem files
     * @return RubyObject specification
     */
    String getDependencies(List<Path> gempaths);

    /**
     * Synchronized decorator.
     * @since 1.0
     */
    final class Synchronized implements GemInfo {

        /**
         * Origin gem info instance.
         */
        private final GemInfo origin;

        /**
         * Wrap origin with synchronized decoradtor.
         * @param origin Gem info
         */
        public Synchronized(final GemInfo origin) {
            this.origin = origin;
        }

        @Override
        public JsonObject getinfo(final Path path) {
            synchronized (this.origin) {
                return this.origin.getinfo(path);
            }
        }

        @Override
        public String getDependencies(final List<Path> paths) {
            synchronized (this.origin) {
                return this.origin.getDependencies(paths);
            }
        }
    }
}
