package com.lotuslabs.tree4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MutableTreeNodePerfTest {

	MutableTreeNode<String,String> mutableTreeNode;
	TreeNode<String,String> treeNode;

	@Before
	public void setUp() {
		List<String> mappings = new ArrayList<>();
		String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String parents = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		int level = 2;
		int id =0;
		for( char c : parents.toCharArray()) {
			for (char a : alpha.toCharArray()) {
				if (a == c) continue;
				String rel0 = (id+100) + ":" + String.valueOf(a) + (id);
				String rel = String.valueOf(a) + (id) + ":" + String.valueOf(c);
				System.out.println( rel );
				mappings.add(rel0);
				mappings.add(rel);
				id++;
			}
		}
		mutableTreeNode = createStringTreeNode(mappings.toArray(new String[0]));
		treeNode = mutableTreeNode;

		System.out.println( "################################");
		System.out.println( treeNode.get(new TreePath<>(new String[] { "K0","KA", "KB26", "K126"})));
	}


	public MutableTreeNode<String,String> createStringTreeNode(String[] pairs) {

		/*
		 * [0] - parent-child
		 * [1] - associate new child to existing parent
		 * [2] - update parent of existing parent
		 * [3]
		 *
		 *
		 */

		MutableTreeNode<String,String> root = new MutableTreeNode<>("K0","0");
		Map<String,MutableTreeNode<String,String>> ledger = new HashMap<>();
		ledger.put("0", root);


		for (int i = 0; i < pairs.length; i++ ) {
			String[] parts = pairs[i].split("\\:");

			//			System.out.println("insert: " + parts[1] + "<-" + parts[0]);
			if (! ledger.containsKey(parts[0]) )
				ledger.put(parts[0], new MutableTreeNode<>("K"+parts[0],parts[0]));

			// find if child exists
			// find if parent exists;
			MutableTreeNode<String,String> childNode = ledger.get(parts[0]);

			if (! ledger.containsKey(parts[1]))
				ledger.put(parts[1], new MutableTreeNode<>("K"+parts[1],parts[1]));

			MutableTreeNode<String,String> parentNode = ledger.get(parts[1]);

			parentNode.add(childNode);

			if (parentNode.getParent() == null && parentNode != root)
				root.add(parentNode);
		}
		return root;
	}



	@Test
	public void testGenerateTreeOuput_root() {
		String actual = mutableTreeNode.generateTreeOutput();
		TreePath<String> p0 = mutableTreeNode.getTreePath();
		System.out.println( "----" + p0 + "----" );
		System.out.println( actual );
		System.out.println("");
		Assert.assertTrue(actual, true);
	}


	public static void main(String[] args) {
		new MutableTreeNodePerfTest().setUp();
	}

}
