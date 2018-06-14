/**
 *
 */
package com.lotuslabs.tree4;

import java.io.Serializable;
import java.util.Iterator;

/**
 * A node of a tree interface that
 * provides read-only access
 *
 * @author psurti
 */
public interface TreeNode<K extends Serializable,V> {
	/*
	 * Supported strategy
	 * for use with method <code>get</code>
	 */
	enum SearchStrategy {
		PRE_ORDER,
		BREADTH_FIRST,
	}

	/**
	 * Returns the child <code>TreeNode</code> at index
	 * <code>childIndex</code>.
	 */
	TreeNode<K,V> getChildAt(int childIndex);

	/**
	 * Returns the number of children <code>TreeNode</code>s the receiver
	 * contains.
	 */
	int getChildCount();

	/**
	 * Returns the parent <code>TreeNode</code> of the receiver.
	 */
	TreeNode<K,V> getParent();

	/**
	 * Returns the index of <code>node</code> in the receivers children.
	 * If the receiver does not contain <code>node</code>, -1 will be
	 * returned.
	 */
	int getIndex(TreeNode<K,V> node);

	/**
	 * Returns true if the receiver allows children.
	 */
	boolean getAllowsChildren();

	/**
	 * Returns true if the receiver is a leaf.
	 */
	boolean isLeaf();

	/**
	 * Return depth
	 */
	int getDepth();

	/**
	 * Returns the level of this node
	 */
	int getLevel();

	/*
	 * Returns children of this tree node only
	 */
	<T extends TreeNode<K,V>> Iterator<T> iterator();

	/*
	 * Traverses the entire tree in pre-order
	 */
	<T extends TreeNode<K,V>> Iterator<T> preOrderEnumeration();
	/*
	 * Traverses the entire tree in post-order
	 */
	<T extends TreeNode<K,V>> Iterator<T> postOrderEnumeration();
	/*
	 * Traverses the entire tree in breadth-first order
	 */
	<T extends TreeNode<K,V>> Iterator<T> breadthFirstEnumeration();
	/*
	 * Traverses the entire tree in depth-first order (post-order)
	 */
	<T extends TreeNode<K,V>> Iterator<T> depthFirstEnumeration();

	/*
	 * Returns a user object
	 */
	V getUserObject();

	/*
	 * Returns the tree-path for this tree-node
	 */
	TreePath<K> getTreePath();


	/**
	 * Returns the tree-node based on the tree-path
	 * if the path is not found it will return null
	 * It defaults to breadth-first enumeration
	 *
	 * @param path
	 * @return
	 */
	TreeNode<K,V> get(TreePath<K> path);

	/**
	 * Returns the tree-node based on the tree-path
	 * if the the path is not found it will return null
	 * The enumeration used is based on the search strategy
	 * with support of pre-order or breadth-first strategy
	 *
	 * @param path
	 * @param strategy
	 * @return
	 */
	TreeNode<K, V> get(TreePath<K> path, SearchStrategy strategy);

	/**
	 * Returns the tree-node based on an unique key
	 * in the entire tree. If the tree node is not found
	 * it will return null. It uses breadth-first enumeration
	 *
	 * @param uniqueKey
	 * @return
	 */
	TreeNode<K,V> find( K uniqueKey );

	/**
	 * Returns the tree-node based on an unique key
	 * in the entire tree. If the tree node is not found
	 * it will return null. The enumeration used is based
	 * on the search strategy with support of pre-order
	 * or breadth-first strategy
	 *
	 * @param uniqueKey is a unique key across the entire tree
	 * @param strategy allows pre-order or breadth-first enumeration
	 * @return null if node not found
	 */
	TreeNode<K,V> find( K uniqueKey, SearchStrategy strategy);

}
