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
import java.util.LinkedList;
import java.util.List;

/**
 * Gem metadata tree.
 * @param <T> Format type
 * @since 1.0
 */
public final class TreeNode<T> implements Iterable<TreeNode<T>> {

    /**
     * Elements ind.
     */
    private final List<TreeNode<T>> elementsind;

    /**
     * Data for current node.
     */
    private final T data;

    /**
     * Parent for current node.
     */
    private TreeNode<T> parent;

    /**
     * Children for current node.
     */
    private final List<TreeNode<T>> children;

    /**
     * Constructs Node.
     * @param data Is data
     */
    public TreeNode(final T data) {
        this.data = data;
        this.children = new LinkedList<>();
        this.elementsind = new LinkedList<>();
    }

    /**
     * Initialize node.
     */
    public void init() {
        this.elementsind.add(this);
    }

    /**
     * Get children for current node.
     * @return List of Children
     */
    public List<TreeNode<T>> getchildren() {
        return this.children;
    }

    /**
     * Get current node data.
     * @return Data for current node
     */
    public T getdata() {
        return this.data;
    }

    /**
     * Check whether node is root.
     * @return Boolean object
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * Check if current node is leaf.
     * @return Boolean object
     */
    public boolean isLeaf() {
        return this.children.size() == 0;
    }

    /**
     * Add child node.
     * @param child Is child
     * @return TreeNode object
     */
    public TreeNode<T> addChild(final T child) {
        final TreeNode<T> childnode = new TreeNode<>(child);
        childnode.init();
        childnode.parent = this;
        this.children.add(childnode);
        this.registerChildForSearch(childnode);
        return childnode;
    }

    /**
     * Get current node level from root.
     * @return Int object
     */
    public int getLevel() {
        final int res;
        if (this.isRoot()) {
            res = 0;
        } else {
            res = this.parent.getLevel() + 1;
        }
        return res;
    }

    /**
     * Search for node.
     * @param cmp Is comparator
     * @return TreeNode object
     */
    public TreeNode<T> findTreeNode(final Comparable<T> cmp) {
        TreeNode<T> res = null;
        for (final TreeNode<T> element : this.elementsind) {
            final T eldata = element.data;
            if (cmp.compareTo(eldata) == 0) {
                res = element;
                break;
            }
        }
        return res;
    }

    @Override
    public String toString() {
        final String res;
        if (this.data == null) {
            res = "[data null]";
        } else {
            res = this.data.toString();
        }
        return res;
    }

    @Override
    public Iterator<TreeNode<T>> iterator() {
        return new TreeNodeIter<T>(this);
    }

    /**
     * Add search child node.
     * @param node Is child
     */
    private void registerChildForSearch(final TreeNode<T> node) {
        this.elementsind.add(node);
        if (this.parent != null) {
            this.parent.registerChildForSearch(node);
        }
    }
}

