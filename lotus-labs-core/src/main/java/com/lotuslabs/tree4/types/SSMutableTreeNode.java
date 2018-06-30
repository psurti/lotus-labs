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

/**
 * TreeNode with K as String and V as String
 * @author psurti
 *
 */
public class SSMutableTreeNode extends SVMutableTreeNode<String> {

	/**
	 * Serial
	 */
	private static final long serialVersionUID = -101304720231586990L;

	/**
	 * Constructor with no parametres
	 */
	public SSMutableTreeNode() {
		super();
	}

	/**
	 * Constructor with key,value
	 *
	 * @param key
	 * @param userObject
	 * @param allowsChildren
	 */
	public SSMutableTreeNode(String key, String userObject, boolean allowsChildren) {
		super(key, userObject, allowsChildren);
	}

	/**
	 * Constructor with key,value
	 *
	 * @param key
	 * @param userObject
	 */
	public SSMutableTreeNode(String key, String userObject) {
		super(key, userObject);
	}

	/**
	 * Constructor with value object
	 *
	 * @param userObject
	 */
	public SSMutableTreeNode(String userObject) {
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
	public static SSMutableTreeNode withValues(String[] valuePairs) {
		return valueOf(valuePairs, (String value, Integer seq)-> "K"+seq, ':');
	}

	/**
	 * Construct tree node based on value pairs and supplied key generation
	 *
	 * @param valuePairs tuple of <child,parent> pairs
	 * @param keyGen key generator
	 * @param delimiter
	 * @return a mutable treenode
	 */
	public static SSMutableTreeNode valueOf(String[] valuePairs, KeyGenerator<String,String,Integer> keyGen, char delimiter ) {

		/*
		 * [0] - parent-child
		 * [1] - associate new child to existing parent
		 * [2] - update parent of existing parent
		 * [3]
		 *
		 *
		 */
		SSMutableTreeNode root = null;
		if (keyGen==null)
			keyGen = (String value, Integer seq)-> "K"+value;

			Map<String,SSMutableTreeNode> ledger = new HashMap<>();
			int k = 0;
			for (int i = 0; i < valuePairs.length; i++ ) {
				String[] parts = valuePairs[i].split("\\" + delimiter);

				if (! ledger.containsKey(parts[0]) ) {
					String key = keyGen.generate(parts[0], k++);
					ledger.put(parts[0], new SSMutableTreeNode(key,parts[0]));
				}

				// find if child exists
				// find if parent exists
				SSMutableTreeNode childNode = ledger.get(parts[0]);

				if (! ledger.containsKey(parts[1])) {
					String key = keyGen.generate(parts[1], k++);
					ledger.put(parts[1], new SSMutableTreeNode(key,parts[1]));
				}

				SSMutableTreeNode parentNode = ledger.get(parts[1]);

				parentNode.add(childNode);

				if (parentNode.getParent() == null && parentNode != root) {
					root = parentNode;
				}
			}

			return root;
	}

	public static SSMutableTreeNode withPaths(String[] paths, char delimiter) {
		SSMutableTreeNode root = new SSMutableTreeNode();
		SSMutableTreeNode matchNode = root;
		for (int i = 0; i < paths.length; i++) {
			String singlePath = paths[i];
			String[] strTreePaths = singlePath.split("\\"+delimiter);
			for (int j = 0; j < paths.length; j++) {
				SSMutableTreeNode foundNode = matchNode.find(paths[j], matchNode.iterator());
				if (foundNode == null) {
					foundNode = new SSMutableTreeNode(strTreePaths[j], null);
					matchNode.add(foundNode);
				}
				matchNode = foundNode;
			}

		}
		if (root.childCount() > 1 )
			throw new IllegalArgumentException( "no single root" );

		SSMutableTreeNode newRoot = root.getFirstChild();
		if (newRoot != null ) newRoot.removeFromParent();
		return newRoot;

	}
}
