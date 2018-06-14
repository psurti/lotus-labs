package com.lotuslabs.tree4;

import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A Mutable TreeNode that supports key and value.
 * The key is optional but can be used get a node based on a xpath
 * The value is the user object that needs to be stored as part of the treenode
 *
 * If keys are not set the caller may use the enumerations to find a node
 * Computational cost to retrieve using keys is O(n)
 *
 * PENDING:
 * Allow storing children in sorted order (by key or value)
 *
 * Not Thread-Safe Mutable Tree Node
 * Return TreeNode<V> to client if immutable-access is needed
 *
 * @author psurti
 *
 * @param <V>
 */
public class MutableTreeNode<K extends Serializable,V> implements TreeNode<K, V>, Cloneable, Serializable
{
	private static final String USER_OBJECT = "userObject";
	private static final String LINE_SEPARATOR = "line.separator";
	private static final String TREE_HAS_ZERO_LEAVES = "tree has zero leaves";
	private static final String SIBLING_HAS_DIFFERENT_PARENT = "sibling has different parent";
	private static final String CHILD_OF_PARENT_IS_NOT_A_SIBLING = "child of parent is not a sibling";
	private static final String NODE_IS_NOT_A_CHILD = "node is not a child";
	private static final String NODES_SHOULD_BE_NULL = "nodes should be null";
	private static final String ARGUMENT_IS_NOT_A_CHILD = "argument is not a child";
	private static final String NEW_CHILD_IS_AN_ANCESTOR = "new child is an ancestor";
	private static final String NEW_CHILD_IS_NULL = "new child is null";
	private static final String NODE_DOES_NOT_ALLOW_CHILDREN = "node does not allow children";
	private static final String ARGUMENT_IS_NULL = "argument is null";
	private static final String NO_MORE_ELEMENTS = "No more elements";
	private static final String NODE_HAS_NO_CHILDREN = "node has no children";
	private static final String WSV_PAD = String.join("", Collections.nCopies(3, " "))+"|";
	private static final String US_PAD = String.join("", Collections.nCopies(3, "_"));

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -6943319042067178854L;

	/** this node's parent, or null if this node has no parent */
	private MutableTreeNode<K,V>   parent;

	/** array of children, may be null if this node has no children */
	private List<MutableTreeNode<K,V>> children;

	/** optional tree key */
	private transient K key;

	/** optional user object */
	private transient  V userObject;

	/** true if the node is able to have children */
	private boolean allowsChildren;


	/**
	 * Creates a tree node that has no parent and no children, but which
	 * allows children.
	 */
	public MutableTreeNode() {
		this(null);
	}

	/**
	 * Constructor
	 *
	 * @param key
	 * @param userObject
	 */
	public MutableTreeNode(K key, V userObject) {
		this(key, userObject, true);
	}

	/**
	 * Creates a tree node with no parent, no children, but which allows
	 * children, and initializes it with the specified user object.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 *                   the node's data
	 */
	public MutableTreeNode(V userObject) {
		this(null, userObject, true);
	}

	/**
	 * Creates a tree node with no parent, no children, initialized with
	 * the specified user object, and that allows children only if
	 * specified.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 *        the node's data
	 * @param allowsChildren if true, the node is allowed to have child
	 *        nodes -- otherwise, it is always a leaf node
	 */
	public MutableTreeNode(K key, V userObject, boolean allowsChildren) {
		super();
		parent = null;
		this.allowsChildren = allowsChildren;
		this.key = key;
		this.userObject = userObject;
	}


	//
	//  Primitives
	//

	/**
	 * Removes <code>newChild</code> from its present parent (if it has a
	 * parent), sets the child's parent to this node, and then adds the child
	 * to this node's child array at index <code>childIndex</code>.
	 * <code>newChild</code> must not be null and must not be an ancestor of
	 * this node.
	 *
	 * @param   newChild        the MutableTreeNode to insert under this node
	 * @param   childIndex      the index in this node's child array
	 *                          where this node is to be inserted
	 * @exception       ArrayIndexOutOfBoundsException  if
	 *                          <code>childIndex</code> is out of bounds
	 * @exception       IllegalArgumentException        if
	 *                          <code>newChild</code> is null or is an
	 *                          ancestor of this node
	 * @exception       IllegalStateException   if this node does not allow
	 *                                          children
	 * @see     #isNodeDescendant
	 */
	public void insert(MutableTreeNode<K,V> newChild, int childIndex) {
		if (!allowsChildren) {
			throw new IllegalStateException(NODE_DOES_NOT_ALLOW_CHILDREN);
		} else if (newChild == null) {
			throw new IllegalArgumentException(NEW_CHILD_IS_NULL);
		} else if (isNodeAncestor(newChild)) {
			throw new IllegalArgumentException(NEW_CHILD_IS_AN_ANCESTOR);
		}

		MutableTreeNode<K,V> oldParent = newChild.getParent();

		if (oldParent != null) {
			oldParent.remove(newChild);
		}
		newChild.setParent(this);
		if (children == null) {
			children = new ArrayList<>();
		}
		children.add(childIndex, newChild);
	}

	/**
	 * Removes the child at the specified index from this node's children
	 * and sets that node's parent to null. The child node to remove
	 * must be a <code>MutableTreeNode</code>.
	 *
	 * @param   childIndex      the index in this node's child array
	 *                          of the child to remove
	 * @exception       ArrayIndexOutOfBoundsException  if
	 *                          <code>childIndex</code> is out of bounds
	 */
	public void remove(int childIndex) {
		MutableTreeNode<K,V> child = getChildAt(childIndex);
		children.remove(childIndex);
		child.setParent(null);
	}

	/**
	 * Sets this node's parent to <code>newParent</code> but does not
	 * change the parent's child array.  This method is called from
	 * <code>insert()</code> and <code>remove()</code> to
	 * reassign a child's parent, it should not be messaged from anywhere
	 * else.
	 *
	 * @param   newParent       this node's new parent
	 */
	@Transient
	public void setParent(MutableTreeNode<K,V> newParent) {
		parent = newParent;
	}

	/**
	 * Returns this node's parent or null if this node has no parent.
	 *
	 * @return  this node's parent TreeNode, or null if this node has no parent
	 */
	@Override
	public MutableTreeNode<K,V> getParent() {
		return parent;
	}

	/**
	 * Returns the child at the specified index in this node's child array.
	 *
	 * @param   index   an index into this node's child array
	 * @exception       ArrayIndexOutOfBoundsException  if <code>index</code>
	 *                                          is out of bounds
	 * @return  the TreeNode in this node's child array at  the specified index
	 */
	@Override
	public MutableTreeNode<K,V> getChildAt(int index) {
		if (children == null) {
			throw new ArrayIndexOutOfBoundsException(NODE_HAS_NO_CHILDREN);
		}
		return children.get(index);
	}

	/**
	 * Returns the number of children of this node.
	 *
	 * @return  an int giving the number of children of this node
	 */
	@Override
	public int getChildCount() {
		if (children == null) {
			return 0;
		} else {
			return children.size();
		}
	}

	/**
	 * Returns the index of the specified child in this node's child array.
	 * If the specified node is not a child of this node, returns
	 * <code>-1</code>.  This method performs a linear search and is O(n)
	 * where n is the number of children.
	 *
	 * @param   aChild  the TreeNode to search for among this node's children
	 * @exception       IllegalArgumentException        if <code>aChild</code>
	 *                                                  is null
	 * @return  an int giving the index of the node in this node's child
	 *          array, or <code>-1</code> if the specified node is a not
	 *          a child of this node
	 */
	@Override
	public int getIndex(TreeNode<K,V> aChild) {
		if (aChild == null) {
			throw new IllegalArgumentException(ARGUMENT_IS_NULL);
		}

		if (!isNodeChild(aChild)) {
			return -1;
		}
		return children.indexOf(aChild);        // linear search
	}

	/**
	 * Creates and returns a forward-order enumeration of this node's
	 * children.  Modifying this node's child array invalidates any child
	 * enumerations created before the modification.
	 *
	 * @return  an Enumeration of this node's children
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Iterator<MutableTreeNode<K,V>> iterator() {
		if (children == null) {
			return Collections.emptyIterator();
		} else {
			return children.iterator();
		}
	}

	/**
	 * Determines whether or not this node is allowed to have children.
	 * If <code>allows</code> is false, all of this node's children are
	 * removed.
	 * <p>
	 * Note: By default, a node allows children.
	 *
	 * @param   allows  true if this node is allowed to have children
	 */
	public void setAllowsChildren(boolean allows) {
		if (allows != allowsChildren) {
			allowsChildren = allows;
			if (!allowsChildren) {
				removeAllChildren();
			}
		}
	}

	/**
	 * Returns true if this node is allowed to have children.
	 *
	 * @return  true if this node allows children, else false
	 */
	@Override
	public boolean getAllowsChildren() {
		return allowsChildren;
	}

	/**
	 * Sets the user object for this node to <code>userObject</code>.
	 *
	 * @param   userObject      the Object that constitutes this node's
	 *                          user-specified data
	 * @see     #getUserObject
	 * @see     #toString
	 */
	public void setUserObject(V userObject) {
		this.userObject = userObject;
	}

	/**
	 * Returns this node's user object.
	 *
	 * @return  the Object stored at this node by the user
	 * @see     #setUserObject
	 * @see     #toString
	 */
	@Override
	public V getUserObject() {
		return userObject;
	}


	//
	//  Derived methods
	//

	/**
	 * Removes the subtree rooted at this node from the tree, giving this
	 * node a null parent.  Does nothing if this node is the root of its
	 * tree.
	 */
	public void removeFromParent() {
		MutableTreeNode<K,V> myParent = getParent();
		if (myParent != null) {
			myParent.remove(this);
		}
	}

	/**
	 * Removes <code>aChild</code> from this node's child array, giving it a
	 * null parent.
	 *
	 * @param   aChild  a child of this node to remove
	 * @exception       IllegalArgumentException        if <code>aChild</code>
	 *                                  is null or is not a child of this node
	 */
	public void remove(MutableTreeNode<K,V> aChild) {
		if (aChild == null) {
			throw new IllegalArgumentException(ARGUMENT_IS_NULL);
		}

		if (!isNodeChild(aChild)) {
			throw new IllegalArgumentException(ARGUMENT_IS_NOT_A_CHILD);
		}
		remove(getIndex(aChild));       // linear search
	}

	/**
	 * Removes all of this node's children, setting their parents to null.
	 * If this node has no children, this method does nothing.
	 */
	public void removeAllChildren() {
		for (int i = getChildCount()-1; i >= 0; i--) {
			remove(i);
		}
	}

	/**
	 * Removes <code>newChild</code> from its parent and makes it a child of
	 * this node by adding it to the end of this node's child array.
	 *
	 * @see             #insert
	 * @param   newChild        node to add as a child of this node
	 * @exception       IllegalArgumentException    if <code>newChild</code>
	 *                                          is null
	 * @exception       IllegalStateException   if this node does not allow
	 *                                          children
	 */
	public void add(MutableTreeNode<K,V> newChild) {
		if(newChild != null && newChild.getParent() == this)
			insert(newChild, getChildCount() - 1);
		else
			insert(newChild, getChildCount());
	}



	//
	//  Tree Queries
	//

	/**
	 * Returns true if <code>anotherNode</code> is an ancestor of this node
	 * -- if it is this node, this node's parent, or an ancestor of this
	 * node's parent.  (Note that a node is considered an ancestor of itself.)
	 * If <code>anotherNode</code> is null, this method returns false.  This
	 * operation is at worst O(h) where h is the distance from the root to
	 * this node.
	 *
	 * @see             #isNodeDescendant
	 * @see             #getSharedAncestor
	 * @param   anotherNode     node to test as an ancestor of this node
	 * @return  true if this node is a descendant of <code>anotherNode</code>
	 */
	public boolean isNodeAncestor(TreeNode<K,V> anotherNode) {
		if (anotherNode == null) {
			return false;
		}

		TreeNode<K,V> ancestor = this;

		do {
			if (ancestor == anotherNode) {
				return true;
			}
		} while((ancestor = ancestor.getParent()) != null);

		return false;
	}

	/**
	 * Returns true if <code>anotherNode</code> is a descendant of this node
	 * -- if it is this node, one of this node's children, or a descendant of
	 * one of this node's children.  Note that a node is considered a
	 * descendant of itself.  If <code>anotherNode</code> is null, returns
	 * false.  This operation is at worst O(h) where h is the distance from the
	 * root to <code>anotherNode</code>.
	 *
	 * @see     #isNodeAncestor
	 * @see     #getSharedAncestor
	 * @param   anotherNode     node to test as descendant of this node
	 * @return  true if this node is an ancestor of <code>anotherNode</code>
	 */
	public boolean isNodeDescendant(MutableTreeNode<K,V> anotherNode) {
		if (anotherNode == null)
			return false;

		return anotherNode.isNodeAncestor(this);
	}

	/**
	 * Returns the nearest common ancestor to this node and <code>aNode</code>.
	 * Returns null, if no such ancestor exists -- if this node and
	 * <code>aNode</code> are in different trees or if <code>aNode</code> is
	 * null.  A node is considered an ancestor of itself.
	 *
	 * @see     #isNodeAncestor
	 * @see     #isNodeDescendant
	 * @param   aNode   node to find common ancestor with
	 * @return  nearest ancestor common to this node and <code>aNode</code>,
	 *          or null if none
	 */
	public MutableTreeNode<K,V> getSharedAncestor(MutableTreeNode<K,V> aNode) {
		if (aNode == this) {
			return this;
		} else if (aNode == null) {
			return null;
		}

		int level1;
		int level2;
		int diff;
		MutableTreeNode<K,V> node1;
		MutableTreeNode<K,V> node2;

		level1 = getLevel();
		level2 = aNode.getLevel();

		if (level2 > level1) {
			diff = level2 - level1;
			node1 = aNode;
			node2 = this;
		} else {
			diff = level1 - level2;
			node1 = this;
			node2 = aNode;
		}

		// Go up the tree until the nodes are at the same level
		while (diff > 0) {
			node1 = node1.getParent();
			diff--;
		}

		// Move up the tree until we find a common ancestor.  Since we know
		// that both nodes are at the same level, we won't cross paths
		// unknowingly (if there is a common ancestor, both nodes hit it in
		// the same iteration).

		do {
			if (node1 == node2) {
				return node1;
			}
			node1 = node1.getParent();
			node2 = node2.getParent();
		} while (node1 != null);// only need to check one -- they're at the
		// same level so if one is null, the other is

		if (node1 != null || node2 != null) {
			throw new IllegalArgumentException(NODES_SHOULD_BE_NULL);
		}

		return null;
	}


	/**
	 * Returns true if and only if <code>aNode</code> is in the same tree
	 * as this node.  Returns false if <code>aNode</code> is null.
	 *
	 * @see     #getSharedAncestor
	 * @see     #getRoot
	 * @return  true if <code>aNode</code> is in the same tree as this node;
	 *          false if <code>aNode</code> is null
	 */
	public boolean isNodeRelated(MutableTreeNode<K,V> aNode) {
		return (aNode != null) && (getRoot() == aNode.getRoot());
	}


	/**
	 * Returns the depth of the tree rooted at this node -- the longest
	 * distance from this node to a leaf.  If this node has no children,
	 * returns 0.  This operation is much more expensive than
	 * <code>getLevel()</code> because it must effectively traverse the entire
	 * tree rooted at this node.
	 *
	 * @see     #getLevel
	 * @return  the depth of the tree whose root is this node
	 */
	@Override
	public int getDepth() {
		MutableTreeNode<K,V>  last = null;
		Iterator<MutableTreeNode<K,V>> iter = breadthFirstEnumeration();

		while (iter.hasNext()) {
			last = iter.next();
		}

		if (last == null) {
			throw new IllegalArgumentException(NODES_SHOULD_BE_NULL);
		}

		return last.getLevel() - getLevel();
	}



	/**
	 * Returns the number of levels above this node -- the distance from
	 * the root to this node.  If this node is the root, returns 0.
	 *
	 * @see     #getDepth
	 * @return  the number of levels above this node
	 */
	@Override
	public int getLevel() {
		MutableTreeNode<K,V> ancestor;
		int levels = 0;

		ancestor = this;
		while((ancestor = ancestor.getParent()) != null){
			levels++;
		}

		return levels;
	}


	/**
	 * Returns the path from the root, to get to this node.  The last
	 * element in the path is this node.
	 *
	 * @return an array of TreeNode objects giving the path, where the
	 *         first element in the path is the root and the last
	 *         element is this node.
	 */
	public MutableTreeNode<K,V>[] getPath() {
		return getPathToRoot(this, 0);
	}

	/**
	 * Builds the parents of node up to and including the root node,
	 * where the original node is the last element in the returned array.
	 * The length of the returned array gives the node's depth in the
	 * tree.
	 *
	 * @param aNode  the TreeNode to get the path for
	 * @param depth  an int giving the number of steps already taken towards
	 *        the root (on recursive calls), used to size the returned array
	 * @return an array of TreeNodes giving the path from the root to the
	 *         specified node
	 */
	@SuppressWarnings("unchecked")
	protected MutableTreeNode<K,V>[] getPathToRoot(MutableTreeNode<K,V> aNode, int depth) {
		MutableTreeNode<K,V>[] retNodes;
		/* Check for null, in case someone passed in a null node, or
           they passed in an element that isn't rooted at root. */
		if(aNode == null) {
			if(depth == 0)
				return null;
			else {
				// new TreeNode[]
				retNodes = (MutableTreeNode<K,V>[]) Array.newInstance(MutableTreeNode.class, depth);
			}
		}
		else {
			depth++;
			retNodes = getPathToRoot(aNode.getParent(), depth);
			retNodes[retNodes.length - depth] = aNode;
		}
		return retNodes;
	}

	/**
	 * Returns the user object path, from the root, to get to this node.
	 * If some of the TreeNodes in the path have null user objects, the
	 * returned path will contain nulls.
	 */
	@SuppressWarnings("unchecked")
	public V[] getUserObjectPath() {
		MutableTreeNode<K,V>[] realPath = getPath();
		V[] retPath = (V[]) new Object[realPath.length];

		for(int counter = 0; counter < realPath.length; counter++)
			retPath[counter] = realPath[counter]
					.getUserObject();
		return retPath;
	}


	/**
	 * Returns the key path, from the root, to get to this node.
	 * If some of the TreeNodes in the path have null , the
	 * returned path will contain nulls.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TreePath<K> getTreePath() {
		MutableTreeNode<K, V>[] realPath = getPath();
		Serializable[] retPath = new Serializable[realPath.length];

		for(int counter = 0; counter < realPath.length; counter++)
			retPath[counter] = realPath[counter].key;

		return new TreePath<>((K[])retPath);
	}

	/**
	 * Returns the root of the tree that contains this node.  The root is
	 * the ancestor with a null parent.
	 *
	 * @see     #isNodeAncestor
	 * @return  the root of the tree that contains this node
	 */
	public MutableTreeNode<K,V> getRoot() {
		MutableTreeNode<K,V> ancestor = this;
		MutableTreeNode<K,V> previous;

		do {
			previous = ancestor;
			ancestor = ancestor.getParent();
		} while (ancestor != null);

		return previous;
	}


	/**
	 * Returns true if this node is the root of the tree.  The root is
	 * the only node in the tree with a null parent; every tree has exactly
	 * one root.
	 *
	 * @return  true if this node is the root of its tree
	 */
	public boolean isRoot() {
		return getParent() == null;
	}


	/**
	 * Returns the node that follows this node in a preorder traversal of this
	 * node's tree.  Returns null if this node is the last node of the
	 * traversal.  This is an inefficient way to traverse the entire tree; use
	 * an enumeration, instead.
	 *
	 * @see     #preorderEnumeration
	 * @return  the node that follows this node in a preorder traversal, or
	 *          null if this node is last
	 */
	public MutableTreeNode<K,V> getNextNode() {
		if (getChildCount() == 0) {
			// No children, so look for nextSibling
			MutableTreeNode<K,V> nextSibling = getNextSibling();

			if (nextSibling == null) {
				MutableTreeNode<K,V> aNode = getParent();

				do {
					if (aNode == null) {
						return null;
					}

					nextSibling = aNode.getNextSibling();
					if (nextSibling != null) {
						return nextSibling;
					}

					aNode = aNode.getParent();
				} while(true);
			} else {
				return nextSibling;
			}
		} else {
			return getChildAt(0);
		}
	}


	/**
	 * Returns the node that precedes this node in a preorder traversal of
	 * this node's tree.  Returns <code>null</code> if this node is the
	 * first node of the traversal -- the root of the tree.
	 * This is an inefficient way to
	 * traverse the entire tree; use an enumeration, instead.
	 *
	 * @see     #preorderEnumeration
	 * @return  the node that precedes this node in a preorder traversal, or
	 *          null if this node is the first
	 */
	public MutableTreeNode<K,V> getPreviousNode() {
		MutableTreeNode<K,V> previousSibling;
		MutableTreeNode<K,V> myParent = getParent();

		if (myParent == null) {
			return null;
		}

		previousSibling = getPreviousSibling();

		if (previousSibling != null) {
			if (previousSibling.getChildCount() == 0)
				return previousSibling;
			else
				return previousSibling.getLastLeaf();
		} else {
			return myParent;
		}
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in preorder.  The first node returned by the enumeration's
	 * <code>nextElement()</code> method is this node.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see     #postorderEnumeration
	 * @return  an enumeration for traversing the tree in preorder
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<MutableTreeNode<K,V>> preOrderEnumeration() {
		return new PreorderEnumeration(this);
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in post-order.  The first node returned by the enumeration's
	 * <code>nextElement()</code> method is the leftmost leaf.  This is the
	 * same as a depth-first traversal.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see     #depthFirstEnumeration
	 * @see     #preorderEnumeration
	 * @return  an enumeration for traversing the tree in postorder
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<MutableTreeNode<K,V>> postOrderEnumeration() {
		return new PostorderEnumeration(this);
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in breadth-first order.  The first node returned by the
	 * enumeration's <code>nextElement()</code> method is this node.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see     #depthFirstEnumeration
	 * @return  an enumeration for traversing the tree in breadth-first order
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<MutableTreeNode<K,V>> breadthFirstEnumeration() {
		return new BreadthFirstEnumeration(this);
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in depth-first order.  The first node returned by the
	 * enumeration's <code>nextElement()</code> method is the leftmost leaf.
	 * This is the same as a postorder traversal.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see     #breadthFirstEnumeration
	 * @see     #postorderEnumeration
	 * @return  an enumeration for traversing the tree in depth-first order
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<MutableTreeNode<K,V>> depthFirstEnumeration() {
		return postOrderEnumeration();
	}

	/**
	 * Creates and returns an enumeration that follows the path from
	 * <code>ancestor</code> to this node.  The enumeration's
	 * <code>nextElement()</code> method first returns <code>ancestor</code>,
	 * then the child of <code>ancestor</code> that is an ancestor of this
	 * node, and so on, and finally returns this node.  Creation of the
	 * enumeration is O(m) where m is the number of nodes between this node
	 * and <code>ancestor</code>, inclusive.  Each <code>nextElement()</code>
	 * message is O(1).<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see             #isNodeAncestor
	 * @see             #isNodeDescendant
	 * @exception       IllegalArgumentException if <code>ancestor</code> is
	 *                                          not an ancestor of this node
	 * @return  an enumeration for following the path from an ancestor of
	 *          this node to this one
	 */
	public Iterator<MutableTreeNode<K,V>> pathFromAncestorEnumeration(TreeNode<K,V> ancestor) {
		return new PathBetweenNodesEnumeration(ancestor, this);
	}


	//
	//  Child Queries
	//

	/**
	 * Returns true if <code>aNode</code> is a child of this node.  If
	 * <code>aNode</code> is null, this method returns false.
	 *
	 * @return  true if <code>aNode</code> is a child of this node; false if
	 *                  <code>aNode</code> is null
	 */
	public boolean isNodeChild(TreeNode<K,V> aNode) {
		boolean retval;

		if (aNode == null) {
			retval = false;
		} else {
			if (getChildCount() == 0) {
				retval = false;
			} else {
				retval = (aNode.getParent() == this);
			}
		}

		return retval;
	}


	/**
	 * Returns this node's first child.  If this node has no children,
	 * throws NoSuchElementException.
	 *
	 * @return  the first child of this node
	 * @exception       NoSuchElementException  if this node has no children
	 */
	public MutableTreeNode<K,V> getFirstChild() {
		if (getChildCount() == 0) {
			throw new NoSuchElementException(NODE_HAS_NO_CHILDREN);
		}
		return getChildAt(0);
	}


	/**
	 * Returns this node's last child.  If this node has no children,
	 * throws NoSuchElementException.
	 *
	 * @return  the last child of this node
	 * @exception       NoSuchElementException  if this node has no children
	 */
	public MutableTreeNode<K,V> getLastChild() {
		if (getChildCount() == 0) {
			throw new NoSuchElementException(NODE_HAS_NO_CHILDREN);
		}
		return getChildAt(getChildCount()-1);
	}


	/**
	 * Returns the child in this node's child array that immediately
	 * follows <code>aChild</code>, which must be a child of this node.  If
	 * <code>aChild</code> is the last child, returns null.  This method
	 * performs a linear search of this node's children for
	 * <code>aChild</code> and is O(n) where n is the number of children; to
	 * traverse the entire array of children, use an enumeration instead.
	 *
	 * @see             #children
	 * @exception       IllegalArgumentException if <code>aChild</code> is
	 *                                  null or is not a child of this node
	 * @return  the child of this node that immediately follows
	 *          <code>aChild</code>
	 */
	public MutableTreeNode<K,V> getChildAfter(TreeNode<K,V> aChild) {
		if (aChild == null) {
			throw new IllegalArgumentException(ARGUMENT_IS_NULL);
		}

		int index = getIndex(aChild);           // linear search

		if (index == -1) {
			throw new IllegalArgumentException(NODE_IS_NOT_A_CHILD);
		}

		if (index < getChildCount() - 1) {
			return getChildAt(index + 1);
		} else {
			return null;
		}
	}


	/**
	 * Returns the child in this node's child array that immediately
	 * precedes <code>aChild</code>, which must be a child of this node.  If
	 * <code>aChild</code> is the first child, returns null.  This method
	 * performs a linear search of this node's children for <code>aChild</code>
	 * and is O(n) where n is the number of children.
	 *
	 * @exception       IllegalArgumentException if <code>aChild</code> is null
	 *                                          or is not a child of this node
	 * @return  the child of this node that immediately precedes
	 *          <code>aChild</code>
	 */
	public MutableTreeNode<K,V> getChildBefore(TreeNode<K,V> aChild) {
		if (aChild == null) {
			throw new IllegalArgumentException(ARGUMENT_IS_NULL);
		}

		int index = getIndex(aChild);           // linear search

		if (index == -1) {
			throw new IllegalArgumentException(ARGUMENT_IS_NOT_A_CHILD);
		}

		if (index > 0) {
			return getChildAt(index - 1);
		} else {
			return null;
		}
	}


	//
	//  Sibling Queries
	//


	/**
	 * Returns true if <code>anotherNode</code> is a sibling of (has the
	 * same parent as) this node.  A node is its own sibling.  If
	 * <code>anotherNode</code> is null, returns false.
	 *
	 * @param   anotherNode     node to test as sibling of this node
	 * @return  true if <code>anotherNode</code> is a sibling of this node
	 */
	public boolean isNodeSibling(TreeNode<K,V> anotherNode) {
		boolean retval;

		if (anotherNode == null) {
			retval = false;
		} else if (anotherNode == this) {
			retval = true;
		} else {
			MutableTreeNode<K,V>  myParent = getParent();
			retval = (myParent != null && myParent == anotherNode.getParent());

			if (retval && !getParent()
					.isNodeChild(anotherNode)) {
				throw new IllegalStateException(SIBLING_HAS_DIFFERENT_PARENT);
			}
		}

		return retval;
	}


	/**
	 * Returns the number of siblings of this node.  A node is its own sibling
	 * (if it has no parent or no siblings, this method returns
	 * <code>1</code>).
	 *
	 * @return  the number of siblings of this node
	 */
	public int getSiblingCount() {
		MutableTreeNode<K,V> myParent = getParent();

		if (myParent == null) {
			return 1;
		} else {
			return myParent.getChildCount();
		}
	}


	/**
	 * Returns the next sibling of this node in the parent's children array.
	 * Returns null if this node has no parent or is the parent's last child.
	 * This method performs a linear search that is O(n) where n is the number
	 * of children; to traverse the entire array, use the parent's child
	 * enumeration instead.
	 *
	 * @see     #children
	 * @return  the sibling of this node that immediately follows this node
	 */
	public MutableTreeNode<K,V> getNextSibling() {
		MutableTreeNode<K,V> retval;

		MutableTreeNode<K,V> myParent = getParent();

		if (myParent == null) {
			retval = null;
		} else {
			retval = myParent.getChildAfter(this);      // linear search
		}

		if (retval != null && !isNodeSibling(retval)) {
			throw new IllegalArgumentException(CHILD_OF_PARENT_IS_NOT_A_SIBLING);
		}

		return retval;
	}


	/**
	 * Returns the previous sibling of this node in the parent's children
	 * array.  Returns null if this node has no parent or is the parent's
	 * first child.  This method performs a linear search that is O(n) where n
	 * is the number of children.
	 *
	 * @return  the sibling of this node that immediately precedes this node
	 */
	public MutableTreeNode<K,V> getPreviousSibling() {
		MutableTreeNode<K,V> retval;

		MutableTreeNode<K,V> myParent = getParent();

		if (myParent == null) {
			retval = null;
		} else {
			retval = myParent.getChildBefore(this);     // linear search
		}

		if (retval != null && !isNodeSibling(retval)) {
			throw new IllegalArgumentException(CHILD_OF_PARENT_IS_NOT_A_SIBLING);
		}

		return retval;
	}



	//
	//  Leaf Queries
	//

	/**
	 * Returns true if this node has no children.  To distinguish between
	 * nodes that have no children and nodes that <i>cannot</i> have
	 * children (e.g. to distinguish files from empty directories), use this
	 * method in conjunction with <code>getAllowsChildren</code>
	 *
	 * @see     #getAllowsChildren
	 * @return  true if this node has no children
	 */
	@Override
	public boolean isLeaf() {
		return (getChildCount() == 0);
	}


	/**
	 * Finds and returns the first leaf that is a descendant of this node --
	 * either this node or its first child's first leaf.
	 * Returns this node if it is a leaf.
	 *
	 * @see     #isLeaf
	 * @see     #isNodeDescendant
	 * @return  the first leaf in the subtree rooted at this node
	 */
	public MutableTreeNode<K,V> getFirstLeaf() {
		MutableTreeNode<K,V> node = this;

		while (!node.isLeaf()) {
			node = node.getFirstChild();
		}

		return node;
	}


	/**
	 * Finds and returns the last leaf that is a descendant of this node --
	 * either this node or its last child's last leaf.
	 * Returns this node if it is a leaf.
	 *
	 * @see     #isLeaf
	 * @see     #isNodeDescendant
	 * @return  the last leaf in the subtree rooted at this node
	 */
	public MutableTreeNode<K,V> getLastLeaf() {
		MutableTreeNode<K,V> node = this;

		while (!node.isLeaf()) {
			node = node.getLastChild();
		}

		return node;
	}


	/**
	 * Returns the leaf after this node or null if this node is the
	 * last leaf in the tree.
	 * <p>
	 * In this implementation of the <code>MutableNode</code> interface,
	 * this operation is very inefficient. In order to determine the
	 * next node, this method first performs a linear search in the
	 * parent's child-list in order to find the current node.
	 * <p>
	 * That implementation makes the operation suitable for short
	 * traversals from a known position. But to traverse all of the
	 * leaves in the tree, you should use <code>depthFirstEnumeration</code>
	 * to enumerate the nodes in the tree and use <code>isLeaf</code>
	 * on each node to determine which are leaves.
	 *
	 * @see     #depthFirstEnumeration
	 * @see     #isLeaf
	 * @return  returns the next leaf past this node
	 */
	public MutableTreeNode<K,V> getNextLeaf() {
		MutableTreeNode<K,V> nextSibling;
		MutableTreeNode<K,V> myParent = getParent();

		if (myParent == null)
			return null;

		nextSibling = getNextSibling(); // linear search

		if (nextSibling != null)
			return nextSibling.getFirstLeaf();

		return myParent.getNextLeaf();  // tail recursion
	}


	/**
	 * Returns the leaf before this node or null if this node is the
	 * first leaf in the tree.
	 * <p>
	 * In this implementation of the <code>MutableNode</code> interface,
	 * this operation is very inefficient. In order to determine the
	 * previous node, this method first performs a linear search in the
	 * parent's child-list in order to find the current node.
	 * <p>
	 * That implementation makes the operation suitable for short
	 * traversals from a known position. But to traverse all of the
	 * leaves in the tree, you should use <code>depthFirstEnumeration</code>
	 * to enumerate the nodes in the tree and use <code>isLeaf</code>
	 * on each node to determine which are leaves.
	 *
	 * @see             #depthFirstEnumeration
	 * @see             #isLeaf
	 * @return  returns the leaf before this node
	 */
	public MutableTreeNode<K,V> getPreviousLeaf() {
		MutableTreeNode<K,V> previousSibling;
		MutableTreeNode<K,V> myParent = getParent();

		if (myParent == null)
			return null;

		previousSibling = getPreviousSibling(); // linear search

		if (previousSibling != null)
			return previousSibling.getLastLeaf();

		return myParent.getPreviousLeaf();              // tail recursion
	}


	/**
	 * Returns the total number of leaves that are descendants of this node.
	 * If this node is a leaf, returns <code>1</code>.  This method is O(n)
	 * where n is the number of descendants of this node.
	 *
	 * @see     #isNodeAncestor
	 * @return  the number of leaves beneath this node
	 */
	public int getLeafCount() {
		int count = 0;

		MutableTreeNode<K,V> node;
		Iterator<MutableTreeNode<K,V>> iter = breadthFirstEnumeration(); // order matters not

		while (iter.hasNext()) {
			node = iter.next();
			if (node.isLeaf()) {
				count++;
			}
		}

		if (count < 1) {
			throw new IllegalArgumentException(TREE_HAS_ZERO_LEAVES);
		}

		return count;
	}


	//
	//  Overrides
	//

	/**
	 * Returns the result of sending <code>toString()</code> to this node's
	 * user object, or the empty string if the node has no user object.
	 *
	 * @see     #getUserObject
	 */
	@Override
	public String toString() {
		if (userObject == null) {
			return "";
		} else {
			return userObject.toString();
		}

	}

	/**
	 * Overridden to make clone public.  Returns a shallow copy of this node;
	 * the new node has no parent or children and has a reference to the same
	 * user object, if any.
	 *
	 * @return  a copy of this node
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		MutableTreeNode<K,V> newNode;

		try {
			newNode = (MutableTreeNode<K,V>)super.clone();

			// shallow copy -- the new node has no parent or children
			newNode.children = null;
			newNode.parent = null;

		} catch (CloneNotSupportedException e) {
			// Won't happen because we implement Cloneable
			throw new UnsupportedOperationException(e.toString());
		}

		return newNode;
	}


	// Serialization support.
	private void writeObject(ObjectOutputStream s) throws IOException {
		Object[]             tValues;

		s.defaultWriteObject();
		// Save the userObject, if its Serializable.
		if(userObject != null && userObject instanceof Serializable) {
			tValues = new Object[2];
			tValues[0] = USER_OBJECT;
			tValues[1] = userObject;
		}
		else
			tValues = new Object[0];
		s.writeObject(tValues);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		V[]      tValues;

		s.defaultReadObject();

		tValues = (V[])s.readObject();

		if(tValues.length > 0 && tValues[0].equals(USER_OBJECT))
			userObject = tValues[1];
	}

	private final class PreorderEnumeration implements Iterator<MutableTreeNode<K,V>> {
		private final ArrayDeque<Iterator<MutableTreeNode<K,V>>> stack = new ArrayDeque<>();

		public PreorderEnumeration(MutableTreeNode<K,V> rootNode) {
			super();
			List<MutableTreeNode<K,V>> v = new ArrayList<>(1);
			v.add(rootNode);     // PENDING: don't really need a vector
			stack.push(v.iterator());
		}

		@Override
		public boolean hasNext() {
			return (!stack.isEmpty() && stack.peek().hasNext());
		}

		@Override
		public MutableTreeNode<K,V> next() {
			Iterator<MutableTreeNode<K,V>> enumer = stack.peek();
			MutableTreeNode<K,V>  node = enumer.next();
			Iterator<MutableTreeNode<K,V>> iter = node.iterator();

			if (!enumer.hasNext()) {
				stack.pop();
			}
			if (iter.hasNext()) {
				stack.push(iter);
			}
			return node;
		}

	}  // End of class PreorderEnumeration



	final class PostorderEnumeration implements Iterator<MutableTreeNode<K,V>> {
		protected MutableTreeNode<K,V> root;
		protected Iterator<MutableTreeNode<K,V>> children;
		protected Iterator<MutableTreeNode<K,V>> subtree;

		public PostorderEnumeration(MutableTreeNode<K,V> rootNode) {
			super();
			root = rootNode;
			children = root.iterator();
			subtree = Collections.emptyIterator();
		}

		@Override
		public boolean hasNext() {
			return root != null;
		}

		@Override
		public MutableTreeNode<K,V> next() {
			MutableTreeNode<K,V> retval;

			if (subtree.hasNext()) {
				retval = subtree.next();
			} else if (children.hasNext()) {
				subtree = new PostorderEnumeration(children.next());
				retval = subtree.next();
			} else {
				retval = root;
				root = null;
			}

			return retval;
		}

	}  // End of class PostorderEnumeration



	final class BreadthFirstEnumeration implements Iterator<MutableTreeNode<K,V>> {
		protected Queue<Iterator<MutableTreeNode<K,V>>> queue;

		public BreadthFirstEnumeration(MutableTreeNode<K,V> rootNode) {
			super();
			List<MutableTreeNode<K,V>> v = new ArrayList<>(1);
			v.add(rootNode);     // PENDING: don't really need a vector
			queue = new Queue<>();
			queue.enqueue(v.iterator());
		}

		@Override
		public boolean hasNext() {
			return (!queue.isEmpty() &&
					queue.firstObject().hasNext());
		}

		@Override
		public MutableTreeNode<K,V> next() {
			Iterator<MutableTreeNode<K,V>> iter = queue.firstObject();
			MutableTreeNode<K,V> node = iter.next();
			Iterator<MutableTreeNode<K,V>> childrenIter = node.iterator();

			if (!iter.hasNext()) {
				queue.dequeue();
			}
			if (childrenIter.hasNext()) {
				queue.enqueue(childrenIter);
			}
			return node;
		}


		// A simple queue with a linked list data structure.
		final class Queue<T> {
			QNode head; // null if empty
			QNode tail;

			final class QNode {
				public T   object;
				public QNode    next;   // null if end
				public QNode(T object, QNode next) {
					this.object = object;
					this.next = next;
				}
			}

			public void enqueue(T anObject) {
				if (head == null) {
					head = tail = new QNode(anObject, null);
				} else {
					tail.next = new QNode(anObject, null);
					tail = tail.next;
				}
			}

			public T dequeue() {
				if (head == null) {
					throw new NoSuchElementException(NO_MORE_ELEMENTS);
				}

				T retval = head.object;
				QNode oldHead = head;
				head = head.next;
				if (head == null) {
					tail = null;
				} else {
					oldHead.next = null;
				}
				return retval;
			}

			public T firstObject() {
				if (head == null) {
					throw new NoSuchElementException(NO_MORE_ELEMENTS);
				}

				return head.object;
			}

			public boolean isEmpty() {
				return head == null;
			}

		} // End of class Queue

	}  // End of class BreadthFirstEnumeration



	final class PathBetweenNodesEnumeration implements Iterator<MutableTreeNode<K,V>> {
		protected ArrayDeque<MutableTreeNode<K,V>> stack;

		public PathBetweenNodesEnumeration(TreeNode<K,V> ancestor,
				MutableTreeNode<K,V> descendant)
		{
			super();

			if (ancestor == null || descendant == null) {
				throw new IllegalArgumentException(ARGUMENT_IS_NULL);
			}

			MutableTreeNode<K,V> current;

			stack = new ArrayDeque<>();
			stack.push(descendant);

			current = descendant;
			while (current != ancestor) {
				current = current.getParent();
				if (current == null && descendant != ancestor) {
					throw new IllegalArgumentException("node " + ancestor +
							" is not an ancestor of " + descendant);
				}
				stack.push(current);
			}
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public MutableTreeNode<K,V> next() {
			try {
				return stack.pop();
			} catch (EmptyStackException e) {
				throw new NoSuchElementException(NO_MORE_ELEMENTS);
			}
		}

	} // End of class PathBetweenNodesEnumeration

	@Override
	public MutableTreeNode<K, V> get(TreePath<K> path, SearchStrategy strategy) {
		if (strategy == SearchStrategy.PRE_ORDER)
			return get(path, this.preOrderEnumeration());
		else
			return get(path);
	}

	/**
	 * Returns the node based on breadthFirstEnumaration
	 * debug:
	 * System.out.println( level+":"+checkNode.key + " " + checkNodeLevel );
	 */
	@Override
	public MutableTreeNode<K, V> get(TreePath<K> path) {
		return get(path, this.breadthFirstEnumeration());
	}

	/**
	 * Returns the node based on breadthFirstEnumaration
	 * debug:
	 * System.out.println( level+":"+checkNode.key + " " + checkNodeLevel );
	 */
	private MutableTreeNode<K, V> get(TreePath<K> path, Iterator<MutableTreeNode<K, V>> enumeration) {
		MutableTreeNode<K,V> ret = null;
		K[] keyArr = path.getPath();
		Iterator<MutableTreeNode<K,V>>  iter = enumeration;
		for(int level = 0, len = keyArr.length; level < len;) {
			if (iter.hasNext()) {
				MutableTreeNode<K, V> checkNode = iter.next();
				K k = keyArr[level];
				boolean keyMatch = k.equals(checkNode.key);
				int checkNodeLevel = checkNode.getLevel();
				/* key and level matches */
				if (checkNodeLevel == level && keyMatch) {
					ret = checkNode;
					level++;
					/* tree moved on to another level whether match or no-match */
				} else if (checkNodeLevel > level) {
					ret = null;
					break;
				}
			} else {
				ret = null;
				break;
			}
		}
		return ret;
	}



	@Override
	public MutableTreeNode<K, V> find(K uniqueKey) {
		return find( uniqueKey, this.breadthFirstEnumeration());
	}

	@Override
	public MutableTreeNode<K, V> find(K uniqueKey, SearchStrategy strategy) {
		if (strategy == SearchStrategy.PRE_ORDER)
			return find(uniqueKey, this.preOrderEnumeration());
		else
			return find(uniqueKey);
	}

	/**
	 * Returns the node based on breadthFirstEnumaration
	 * debug:
	 * System.out.println( level+":"+checkNode.key + " " + checkNodeLevel );
	 */
	private MutableTreeNode<K, V> find(K uniqueKey, Iterator<MutableTreeNode<K, V>> enumeration) {
		MutableTreeNode<K,V> ret = null;
		Iterator<MutableTreeNode<K,V>>  iter = enumeration;
		while (uniqueKey != null && iter.hasNext()) {
			MutableTreeNode<K, V> checkNode = iter.next();
			boolean keyMatch = uniqueKey.equals(checkNode.key);
			/* key matches */
			if (keyMatch) {
				ret = checkNode;
				break;
			}
		}
		return ret;
	}


	/**
	 * Generates a pseudo pyramid -based tree diagram
	 * starting from this node
	 *
	 * @return
	 */
	public String generatePyramidOutput() {
		List<String> ret = new ArrayList<>();
		Iterator<MutableTreeNode<K,V>> iter = this.breadthFirstEnumeration();
		int level = 0;
		while( iter.hasNext() ) {
			MutableTreeNode<K,V> tNode = iter.next();
			int nodeLevel = tNode.getLevel();
			if (level != nodeLevel) {
				level = nodeLevel;
				ret.add(System.getProperty(LINE_SEPARATOR));
			}

			ret.add(tNode.getUserObject() + ",");
		}
		return String.join("", ret);
	}

	/**
	 * Generates a text-based tree diagram
	 * starting from this node
	 *
	 * @return
	 */
	public String generateTreeOutput() {
		List<String> ret = new ArrayList<>();
		Iterator<MutableTreeNode<K,V>> iter = this.preOrderEnumeration();
		int level = 0;
		Deque<String> stack = new ArrayDeque<>();
		while(iter.hasNext()) {
			MutableTreeNode<K,V> tNode = iter.next();
			int nodeLevel = tNode.getLevel();
			if (nodeLevel > level) {
				level = nodeLevel;
				stack.push(WSV_PAD);
			} else if (nodeLevel < level) {
				for (int k = nodeLevel; k < level; k++) {
					if (!stack.isEmpty())
						stack.pop();
				}
				level = nodeLevel;
			}
			ret.add(System.getProperty(LINE_SEPARATOR));
			if (!stack.isEmpty()) {
				ret.add(getPath(stack));
				ret.add(US_PAD);
			}
			ret.add(tNode.key + ":" + tNode.getUserObject());
		}

		return String.join("", ret);
	}

	/*
	 * return the path
	 */
	private String getPath(Deque<String> stack) {
		StringBuilder ret = new StringBuilder();
		for (String str : stack) {
			ret.append(str);
		}
		return ret.toString();
	}
} // End of class MutableTreeNode
