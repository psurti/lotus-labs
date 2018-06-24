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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.lotuslabs.tree4.MutableTreeNode;
import com.lotuslabs.tree4.TreePath;

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


	@FunctionalInterface
	interface KeyGenerator<K, V, I> {
		K generate(V value, I seq);
	}

	public SSMutableTreeNode() {
		super();
	}

	public SSMutableTreeNode(String key, String userObject, boolean allowsChildren) {
		super(key, userObject, allowsChildren);
	}

	public SSMutableTreeNode(String key, String userObject) {
		super(key, userObject);
	}

	public SSMutableTreeNode(String userObject) {
		super(userObject);
	}

	public static SSMutableTreeNode valueOf(String[] valuePairs) {
		return valueOf(valuePairs, (String value, Integer seq)-> "K"+seq, ':');
	}


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

	public static void main(String[] args) {
		SSMutableTreeNode mutableTreeNode = valueOf(new String[] {
				"H:G", // find g; find h
				"F:G", // find g;(found) find f
				"G:D", // find d; (not found) - add to root; find g (found) -- add to d
				"E:D",
				"A:E",
				"B:C",
				"C:E",
				"D:0",
				"Z:Y",
				"Y:X",
				"X:0"
		});

		assertEquals(" K8:0    |___K3:D    |   |___K1:G    |   |   |___K0:H    |   |   |___K2:F    |   |___K4:E    |   |   |___K5:A    |   |   |___K7:C    |   |   |   |___K6:B    |___K11:X    |   |___K10:Y    |   |   |___K9:Z"
				, mutableTreeNode.generateTreeOutput().replaceAll(System.lineSeparator(), " "));
		assertEquals("E not matched", "E", mutableTreeNode.get(new TreePath<>(new String[] {"K8", "K3", "K4"})).getUserObject());
	}
}
