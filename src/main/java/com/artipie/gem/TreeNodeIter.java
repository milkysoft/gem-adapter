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

/**
 * Gem metadata parser.
 * @since 1.0
 */
public class TreeNodeIter<T> implements Iterator<TreeNode<T>> {

    /**
     * Json Gem info format.
     */
    enum ProcessStages {
        PROCESSPARENT, PROCESSCHILDCURNODE, PROCESSCHILDSUBNODE
    }

    /**
     * Json Gem info format.
     */
    private final TreeNode<T> treeNode;

    /**
     * Json Gem info format.
     * @param treeNode Is node
     */
    public TreeNodeIter(final TreeNode<T> treeNode) {
        this.treeNode = treeNode;
        this.donext = ProcessStages.PROCESSPARENT;
        this.cCurNodeIter = treeNode.children.iterator();
    }

    /**
     * Json Gem info format.
     */
    private ProcessStages donext;

    /**
     * Json Gem info format.
     */
    private TreeNode<T> thenext;

    /**
     * Json Gem info format.
     */
    private final Iterator<TreeNode<T>> cCurNodeIter;

    /**
     * Json Gem info format.
     */
    private Iterator<TreeNode<T>> cSubNodeIter;

    @Override
    public boolean hasNext() {
        boolean result = false;
        if (this.donext == ProcessStages.PROCESSPARENT) {
            this.thenext = this.treeNode;
            this.donext = ProcessStages.PROCESSCHILDCURNODE;
            result = true;
        } else if (this.donext == ProcessStages.PROCESSCHILDCURNODE) {
            if (this.cCurNodeIter.hasNext()) {
                final TreeNode<T> childdirect = this.cCurNodeIter.next();
                cSubNodeIter = childdirect.iterator();
                this.donext = ProcessStages.PROCESSCHILDSUBNODE;
                result = hasNext();
            }
            else {
                this.donext = null;
                result = false;
            }
        } else if (this.donext == ProcessStages.PROCESSCHILDSUBNODE) {
            if (this.cSubNodeIter.hasNext()) {
                this.thenext = this.cSubNodeIter.next();
                result = true;
            }
            else {
                this.thenext = null;
                this.donext = ProcessStages.PROCESSCHILDCURNODE;
                result = hasNext();
            }
        }
        return result;
    }

    @Override
    public TreeNode<T> next() {
        return this.thenext;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
