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

import com.lotuslabs.tree4.TreeNode.SearchStrategy;

@RunWith(JUnit4.class)
public class MutableTreeNodePerfTest {

	MutableTreeNode<String,String> mutableTreeNode;
	TreeNode<String,String> treeNode;

	@Before
	public void setUp() {
		List<String> mappings = new ArrayList<>();
		String loop0 = "0";
		String loop1 = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		createLevel(mappings, loop1, loop0, 3);
		long start = System.currentTimeMillis();
		mutableTreeNode = createStringTreeNode(mappings.toArray(new String[0]));
		long stop = System.currentTimeMillis();
		System.out.println( "createTree time:" + (stop-start));
		treeNode = mutableTreeNode;

		System.out.println( "######################");

	}


	private void createLevel(List<String> mappings, String loop, String parent, int level) {
		if (level == 0)
			return;
		for( char x : loop.toCharArray()) {
			mappings.add(""+x+parent+":"+parent);
			parent = ""+x+parent;
			createLevel(mappings, loop, parent, level-1);
		}
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

	@Test(timeout=50)
	public void testGet() {
		long start = System.currentTimeMillis();
		String[] query = {
				"K0",
				"K00",
				"KA00",
				"KBA00",
				"KCBA00",
				"K0CBA00",
				"KA0CBA00",
				"KBA0CBA00",
				"KCBA0CBA00",
				"K0CBA0CBA00"
		};
		System.out.println( treeNode.get(new TreePath<>(query)));
		long stop = System.currentTimeMillis();
		System.out.println( "get time(ms): " + (stop-start));
	}

	@Test(timeout=50)
	public void testFind_BreadthFirst() {
		long start = System.currentTimeMillis();
		//String q="K0CBA0CBA00";
		String q="KQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA00";
		System.out.println( treeNode.find(q, SearchStrategy.BREADTH_FIRST) );
		long stop = System.currentTimeMillis();
		System.out.println( "BF find time(ms): " + (stop-start));
	}

	@Test(timeout=50)
	public void testFind_PreOrder() {
		long start = System.currentTimeMillis();
		//String q="K0CBA0CBA00";
		String q="KQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA00";
		System.out.println( treeNode.find(q, SearchStrategy.PRE_ORDER) );
		long stop = System.currentTimeMillis();
		System.out.println( "PO find time(ms): " + (stop-start));
	}

	public static void main(String[] args) {
		new MutableTreeNodePerfTest().setUp();
	}

}
