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

import java.util.Iterator;
import java.util.Optional;

/**
 * Gem metadata parser.
 * @param <T> Format type
 * @since 1.0
 */
public final class TreeNodeIter<T> implements Iterator<TreeNode<T>> {

    /**
     * Json Gem info format.
     */
    enum ProcessStages {
        /**
         * Json Gem info format.
         */
        PROCESSPARENT,

        /**
         * Json Gem info format.
         */
        PROCESSCHILDCURNODE,

        /**
         * Json Gem info format.
         */
        PROCESSCHILDSUBNODE,

        /**
         * Json Gem info format.
         */
        PROCESSNULL
    }

    /**
     * Json Gem info format.
     */
    private final TreeNode<T> thetreenode;

    /**
     * Json Gem info format.
     */
    private Optional<TreeNode<T>> thenext;

    /**
     * Json Gem info format.
     */
    private final Iterator<TreeNode<T>> ccurnodeiter;

    /**
     * Json Gem info format.
     */
    private Iterator<TreeNode<T>> csubnodeiter;

    /**
     * Json Gem info format.
     */
    private ProcessStages donext;

    /**
     * Json Gem info format.
     * @param thetreenode Is node
     */
    public TreeNodeIter(final TreeNode<T> thetreenode) {
        this.thetreenode = thetreenode;
        this.donext = ProcessStages.PROCESSPARENT;
        this.ccurnodeiter = thetreenode.getchildren().iterator();
    }

    @Override
    public boolean hasNext() {
        boolean result = false;
        if (this.donext == ProcessStages.PROCESSPARENT) {
            this.thenext = Optional.of(this.thetreenode);
            this.donext = ProcessStages.PROCESSCHILDCURNODE;
            result = true;
        } else if (this.donext == ProcessStages.PROCESSCHILDCURNODE) {
            if (this.ccurnodeiter.hasNext()) {
                final TreeNode<T> childdirect = this.ccurnodeiter.next();
                this.csubnodeiter = childdirect.iterator();
                this.donext = ProcessStages.PROCESSCHILDSUBNODE;
                result = this.hasNext();
            } else {
                this.donext = ProcessStages.PROCESSNULL;
                result = false;
            }
        } else if (this.donext == ProcessStages.PROCESSCHILDSUBNODE) {
            if (this.csubnodeiter.hasNext()) {
                this.thenext = Optional.of(this.csubnodeiter.next());
                result = true;
            } else {
                this.thenext = Optional.empty();
                this.donext = ProcessStages.PROCESSCHILDCURNODE;
                result = this.hasNext();
            }
        }
        return result;
    }

    @Override
    public TreeNode<T> next() {
        TreeNode<T> res = null;
        if (this.thenext.isPresent()) {
            res = this.thenext.get();
        }
        return res;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
