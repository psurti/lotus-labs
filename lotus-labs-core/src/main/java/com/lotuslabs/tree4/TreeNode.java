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
	<T extends TreeNode<K,V>> T getChildAt(int childIndex);

	/**
	 * Return the next sibling for this node
	 * @return null if does not exist
	 */
	<T extends TreeNode<K,V>> T getNextSibling();

	/**
	 * Return the previous siblint for this node
	 * @return null if does not exist
	 */
	<T extends TreeNode<K,V>> T getPreviousSibling();

	/**
	 * Return the child after supplied one for this parent
	 * @param node one of the child node
	 * @return null if does not exist
	 */
	<T extends TreeNode<K,V>> T getChildAfter(TreeNode<K,V> node);

	/**
	 * Return the child before supplied one for this parent
	 * @param node one of the child node
	 * @return null if does not exist
	 */
	<T extends TreeNode<K,V>> T getChildBefore(TreeNode<K,V> node);

	/**
	 * Returns the number of children <code>TreeNode</code>s the receiver
	 * contains.
	 */
	int childCount();

	/**
	 * Returns the number of leaves in the entire tree
	 * @return
	 */
	int leafCount();

	/**
	 * Returns the parent <code>TreeNode</code> of the receiver.
	 */
	<T extends TreeNode<K,V>> T getParent();

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

	/**
	 * Is {@code aNode} a child
	 */
	boolean isNodeChild(TreeNode<K,V> aNode);

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
	<T extends TreeNode<K,V>> T get(TreePath<K> path);

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
	<T extends TreeNode<K,V>> T get(TreePath<K> path, SearchStrategy strategy);

	/**
	 * Returns the tree-node based on an unique key
	 * in the entire tree. If the tree node is not found
	 * it will return null. It uses breadth-first enumeration
	 *
	 * @param uniqueKey
	 * @return
	 */
	<T extends TreeNode<K,V>> T find( K uniqueKey );

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
	<T extends TreeNode<K,V>> T find( K uniqueKey, SearchStrategy strategy);

	/**
	 * Return the key
	 * @return
	 */
	K getKey();

	/**
	 * Return count all the nodes
	 * @return
	 */
	int totalCount();

}
