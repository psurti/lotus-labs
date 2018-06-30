/*
 * Licensed to surti-labs under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Surti-labs licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.lotuslabs.tree4.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.lotuslabs.tree4.MutableTreeNode;

/**
 * TreeNode with K as String and V as String
 * @author psurti
 *
 */
public class SVMutableTreeNode<V> extends MutableTreeNode<String, V> {

	/**
	 * Serial
	 */
	private static final long serialVersionUID = -101304720231586990L;

	/**
	 * Constructor with no parametres
	 */
	public SVMutableTreeNode() {
		super();
	}

	/**
	 * Constructor with key,value
	 *
	 * @param key
	 * @param userObject
	 * @param allowsChildren
	 */
	public SVMutableTreeNode(String key, V userObject, boolean allowsChildren) {
		super(key, userObject, allowsChildren);
	}

	/**
	 * Constructor with key,value
	 *
	 * @param key
	 * @param userObject
	 */
	public SVMutableTreeNode(String key, V userObject) {
		super(key, userObject);
	}

	/**
	 * Constructor with value object
	 *
	 * @param userObject
	 */
	public SVMutableTreeNode(V userObject) {
		super(userObject);
	}

	@FunctionalInterface
	interface KeyGenerator<K, V, I> {
		K generate(V value, I seq);
	}

	/**
	 * Construct tree node based on value pairs and default key generation
	 *
	 * @param valuePairs tuple of <child,parent> pairs
	 * @return a mutable treenode
	 */
	public static <V> SVMutableTreeNode<V> withKeys(String[] keyPairs) {
		return valueOf(keyPairs, ':');
	}

	/**
	 * Construct treenode based on key pairs
	 *
	 * @param keyPairs
	 * @param delimiter
	 * @return mutable tree node based tuples of key pairs
	 */
	public static <V> SVMutableTreeNode<V> valueOf(String[] keyPairs, char delimiter) {
		SVMutableTreeNode<V> root = null;

		Map<String,SVMutableTreeNode<V>> ledger = new HashMap<>();
		for (int i = 0; i < keyPairs.length; i++ ) {
			String[] parts = keyPairs[i].split("\\" + delimiter);

			if (! ledger.containsKey(parts[0]) ) {
				ledger.put(parts[0], new SVMutableTreeNode<V>(parts[0], null));
			}

			// find if child exists
			// find if parent exists
			SVMutableTreeNode<V> childNode = ledger.get(parts[0]);

			if (! ledger.containsKey(parts[1])) {
				ledger.put(parts[1], new SVMutableTreeNode<V>(parts[1],null));
			}

			SVMutableTreeNode<V> parentNode = ledger.get(parts[1]);

			parentNode.add(childNode);

			if (parentNode.getParent() == null && parentNode != root) {
				root = parentNode;
			}
		}
		return root;
	}

	public static <V> SVMutableTreeNode<V> withPaths(Map<String,V> propertyMap, char delimiter) {
		SVMutableTreeNode<V> root = new SVMutableTreeNode<>();
		Set<String> keySet = propertyMap.keySet();
		for (String singlePath : keySet) {
			SVMutableTreeNode<V> matchNode = root;
			String[] strTreePaths = singlePath.split("\\"+delimiter);
			for (int j = 0; j < strTreePaths.length; j++) {
				SVMutableTreeNode<V> foundNode = matchNode.find(strTreePaths[j], matchNode.iterator());
				if (foundNode == null) {
					foundNode = new SVMutableTreeNode<>(strTreePaths[j], null);
					matchNode.add(foundNode);
				}
				matchNode = foundNode;
			}
			matchNode.setUserObject(propertyMap.get(singlePath));
		}
		if (root.childCount() > 1 )
			throw new IllegalArgumentException( "no single root" );

		SVMutableTreeNode<V> newRoot = root.getFirstChild();
		if (newRoot != null ) newRoot.removeFromParent();
		return newRoot;

	}
}
