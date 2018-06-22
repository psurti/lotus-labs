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
}
