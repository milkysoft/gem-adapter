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
 * Gem metadata parser.
 * @since 1.0
 */
public class TreeNode<T> implements Iterable<TreeNode<T>> {

    /**
     * Json Gem info format.
     */
    public T data;

    /**
     * Json Gem info format.
     */
    public TreeNode<T> parent;

    /**
     * Json Gem info format.
     */
    public List<TreeNode<T>> children;

    /**
     * Extract Gem info.
     * @return Boolean object
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * Extract Gem info.
     * @return Boolean object
     */
    public boolean isLeaf() {
        return this.children.size() == 0;
    }

    /**
     * Json Gem info format.
     */
    private List<TreeNode<T>> elementsIndex;

    /**
     * Extract Gem info.
     * @param data Is data
     */
    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<TreeNode<T>>();
        this.elementsIndex = new LinkedList<TreeNode<T>>();
        this.elementsIndex.add(this);
    }

    /**
     * Extract Gem info.
     * @param child Is child
     * @return TreeNoode object
     */
    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
        this.registerChildForSearch(childNode);
        return childNode;
    }

    /**
     * Extract Gem info.
     * @return Int object
     */
    public int getLevel() {
        if (this.isRoot()) {
            return 0;
        } else {
            return this.parent.getLevel() + 1;
        }
    }

    /**
     * Extract Gem info.
     * @param node Is child
     */
    private void registerChildForSearch(TreeNode<T> node) {
        elementsIndex.add(node);
        if (this.parent != null) {
            this.parent.registerChildForSearch(node);
        }
    }

    /**
     * Extract Gem info.
     * @param cmp Is child
     * @return TreeNoode object
     */
    public TreeNode<T> findTreeNode(Comparable<T> cmp) {
        for (TreeNode<T> element : this.elementsIndex) {
            T elData = element.data;
            if (cmp.compareTo(elData) == 0) {
                return element;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return this.data != null ? this.data.toString() : "[data null]";
    }

    @Override
    public Iterator<TreeNode<T>> iterator() {
        TreeNodeIter<T> iter = new TreeNodeIter<T>(this);
        return iter;
    }
}

