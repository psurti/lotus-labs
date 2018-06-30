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

import com.lotuslabs.tree4.MutableTreeNode;

/**
 * TreeNode with K as String and V as String
 * @author psurti
 *
 */
public class SSMutableTreeNode extends MutableTreeNode<String, String> {

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
	 * Construct treenode based on key pairs
	 *
	 * @param keyPairs
	 * @param delimiter
	 * @return mutable tree node based tuples of key pairs
	 */
	public static SSMutableTreeNode valueOf(String[] keyPairs, char delimiter) {
		SSMutableTreeNode root = null;

		Map<String,SSMutableTreeNode> ledger = new HashMap<>();
		for (int i = 0; i < keyPairs.length; i++ ) {
			String[] parts = keyPairs[i].split("\\" + delimiter);

			if (! ledger.containsKey(parts[0]) ) {
				ledger.put(parts[0], new SSMutableTreeNode(parts[0], null));
			}

			// find if child exists
			// find if parent exists
			SSMutableTreeNode childNode = ledger.get(parts[0]);

			if (! ledger.containsKey(parts[1])) {
				ledger.put(parts[1], new SSMutableTreeNode(parts[1],null));
			}

			SSMutableTreeNode parentNode = ledger.get(parts[1]);

			parentNode.add(childNode);

			if (parentNode.getParent() == null && parentNode != root) {
				root = parentNode;
			}
		}
		return root;
	}

	/**
	 * Construct tree node based on value pairs and default key generation
	 *
	 * @param valuePairs tuple of <child,parent> pairs
	 * @return a mutable treenode
	 */
	public static SSMutableTreeNode valueOf(String[] valuePairs) {
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
			keyGen = (String value, Integer seq)-> "K"+seq;

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
}
