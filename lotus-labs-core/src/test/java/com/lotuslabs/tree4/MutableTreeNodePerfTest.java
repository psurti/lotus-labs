package com.lotuslabs.tree4;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.lotuslabs.tree4.TreeNode.SearchStrategy;
import com.lotuslabs.tree4.types.SVMutableTreeNode;
import com.lotuslabs.utils.Repeat;
import com.lotuslabs.utils.RepeatRule;

@RunWith(JUnit4.class)
public class MutableTreeNodePerfTest {

	@Rule
	public RepeatRule repeatRule = new RepeatRule();

	MutableTreeNode<String,String> mutableTreeNode;
	TreeNode<String,String> treeNode;

	@Before
	public void setUp() {
		List<String> mappings = new ArrayList<>();
		String loop0 = "0";
		String loop1 = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		createLevel(mappings, loop1, loop0, 3);
		long start = System.currentTimeMillis();
		mutableTreeNode = SVMutableTreeNode.withStringValues(mappings.toArray(new String[0]), null, ':');
		long stop = System.currentTimeMillis();
		System.out.println( "createTree time:" + (stop-start) + " totalNodes:" + mutableTreeNode.totalCount());
		treeNode = mutableTreeNode;
	}

	@After
	public void tearDown() {
		System.out.println("---");
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

	@Test
	public void testGenerateTreeOuput_root() {
		String actual = mutableTreeNode.generateTreeOutput();
		TreePath<String> p0 = mutableTreeNode.getTreePath();
		System.out.println( "----" + p0 + "----" );
		System.out.println( actual );
		System.out.println("");
		Assert.assertTrue(actual, true);
	}

	@Test(timeout=150)
	@Repeat(10)
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

		System.out.println( treeNode.get(new TreePath<>(query)).toString() );
		long stop = System.currentTimeMillis();
		System.out.println( "get time(ms): " + (stop-start));
	}

	@Test(timeout=150)
	@Repeat(10)
	public void testFind_BreadthFirst() {
		long start = System.currentTimeMillis();
		//String q="K0CBA0CBA00";
		String q="KQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA00";
		System.out.println( treeNode.find(q, SearchStrategy.BREADTH_FIRST).toString() );
		long stop = System.currentTimeMillis();
		System.out.println( "BF find time(ms): " + (stop-start));
	}

	@Test(timeout=150)
	@Repeat(10)
	public void testFind_PreOrder() {
		long start = System.currentTimeMillis();
		//String q="K0CBA0CBA00";
		String q="KQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA0ZYXWVUTSRQPONMLKJIHGFEDCBA00";
		System.out.println( treeNode.find(q, SearchStrategy.PRE_ORDER).toString() );
		long stop = System.currentTimeMillis();
		System.out.println( "PreO find time(ms): " + (stop-start));
	}

	public static void main(String[] args) {
		MutableTreeNodePerfTest perfTest = new MutableTreeNodePerfTest();
		perfTest.setUp();
		perfTest.testGet();
	}

}
