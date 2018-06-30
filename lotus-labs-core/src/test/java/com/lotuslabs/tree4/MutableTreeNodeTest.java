package com.lotuslabs.tree4;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.lotuslabs.tree4.types.SSMutableTreeNode;

@RunWith(JUnit4.class)
public class MutableTreeNodeTest {

	MutableTreeNode<String,String> mutableTreeNode;
	TreeNode<String,String> treeNode;

	@Before
	public void setUp() {
		mutableTreeNode = SSMutableTreeNode.valueOf((new String[] {
				"H:G", // find g; find h;
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
		}));

		treeNode = mutableTreeNode;
	}

	@Test
	public void testGeneratePyramidOuput_root() {
		String actual = mutableTreeNode.generatePyramidOutput();
		TreePath<String> p0 = mutableTreeNode.getTreePath();
		System.out.println( "----" + p0 + "----" );
		System.out.println( actual );
		System.out.println("");
		Assert.assertTrue(actual, true);
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


	@Test
	public void testGenerateTreeOuput_leaf() {
		TreePath<String> p0 = new TreePath<>(new String[] {"K0","KD","KG","KF"});
		System.out.println( "----" + p0 + "----" );
		MutableTreeNode<String,String> kf = mutableTreeNode.get(p0);
		String actual = kf.generateTreeOutput();
		System.out.println( actual );
		System.out.println("");
		Assert.assertTrue(actual, true);
	}


	@Test
	public void testGenerateTreeOuput_subTree() {
		TreePath<String> p0 = new TreePath<>(new String[] {"K0","KD","KE"});
		System.out.println( "----" + p0 + "----" );
		MutableTreeNode<String,String> ke = mutableTreeNode.get(p0);
		String actual = ke.generateTreeOutput();
		System.out.println( actual );
		System.out.println("");
		Assert.assertTrue(actual, true);
	}


	@Test
	public void testPreOrderEnumeration() {
		StringBuilder actuals = new StringBuilder();
		Iterator<MutableTreeNode<String,String>> preorderEnumeration = mutableTreeNode.preOrderEnumeration();
		for( ; preorderEnumeration.hasNext(); ) {
			MutableTreeNode<String,String> elem = preorderEnumeration.next();
			actuals.append( "/"+elem  );
		}
		Assert.assertEquals("/0/D/G/H/F/E/A/C/B/X/Y/Z", actuals.toString());
	}

	@Test
	public void testPostOrderEnumeration() {
		StringBuilder actuals = new StringBuilder();
		Iterator<MutableTreeNode<String,String>> postOrderEnumeration = mutableTreeNode.postOrderEnumeration();
		for( ; postOrderEnumeration.hasNext(); ) {
			MutableTreeNode<String,String> elem = postOrderEnumeration.next();
			actuals.append( "/"+elem  );
		}
		Assert.assertEquals("/H/F/G/A/B/C/E/D/Z/Y/X/0", actuals.toString());
	}

	@Test
	public void testBreadthFirstEnumeration() {
		StringBuilder actuals = new StringBuilder();
		Iterator<MutableTreeNode<String,String>> breadthFirstEnumeration = mutableTreeNode.breadthFirstEnumeration();
		for( ; breadthFirstEnumeration.hasNext(); ) {
			MutableTreeNode<String,String> elem = breadthFirstEnumeration.next();
			actuals.append( "/"+elem  );
		}
		Assert.assertEquals("/0/D/X/G/E/Y/H/F/A/C/Z/B", actuals.toString());
	}

	@Test
	public void testIterator() {
		List<String> actuals = new ArrayList<>();
		Iterator<MutableTreeNode<String,String>> iter = mutableTreeNode.iterator();
		while (iter.hasNext()) {
			MutableTreeNode<String,String> val = iter.next();
			actuals.add( val.getUserObject() );
		}
		Assert.assertArrayEquals("failed to get correct children", new String[] {"D","X"}, actuals.toArray());
	}

	@Test
	public void testGet_leafPath() {
		TreeNode<String,String> node = mutableTreeNode.get(treePathParameters()[0]);
		Assert.assertNotNull(node);
	}

	@Test
	public void testGet_subInvalidPath() {
		TreeNode<String,String> node = mutableTreeNode.get(treePathParameters()[1]);
		Assert.assertNull("Invalid path within depth of tree",node);
	}

	@Test
	public void testGet_extendedInvalidLeaf() {
		TreeNode<String,String> node = mutableTreeNode.get(treePathParameters()[2]);
		Assert.assertNull("Invalid path beyond depth of tree", node);
	}

	@Test
	public void testValueOf() {
		TreePath<String>[] treePaths = treePathParameters();
		MutableTreeNode<String, Object> root = MutableTreeNode.valueOf(treePaths);
		Assert.assertEquals(2, root.childCount());
	}

	public static TreePath<String>[] treePathParameters() {
		@SuppressWarnings("unchecked")
		TreePath<String>[] px = (TreePath<String>[]) Array.newInstance(TreePath.class,3);
		px[0]=new TreePath<>(new String[] {"K0","KD","KG","KF"});
		px[1]=new TreePath<>(new String[] {"K0","KG"});
		px[2]=new TreePath<>(new String[] {"K0","KD","KG","KF","KX"});
		return px;
	}

	@Test
	public void testWriteObject() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.println( this.mutableTreeNode.generateTreeOutput());

		ObjectOutputStream output =
				new ObjectOutputStream(new FileOutputStream("object.data"));
		output.writeObject(this.mutableTreeNode);
		output.close();

		ObjectInputStream input =
				new ObjectInputStream(new FileInputStream("object.data"));

		@SuppressWarnings("unchecked")
		MutableTreeNode<String,String> clonedTreeNode = (MutableTreeNode<String, String>) input.readObject();
		input.close();
		System.out.println( clonedTreeNode.generateTreeOutput());
		Assert.assertEquals(this.mutableTreeNode.generateTreeOutput(), clonedTreeNode.generateTreeOutput());
	}

	public static void main(String[] args) {
		new MutableTreeNodeTest().setUp();
	}

}
