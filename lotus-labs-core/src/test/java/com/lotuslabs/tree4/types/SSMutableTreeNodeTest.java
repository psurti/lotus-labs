package com.lotuslabs.tree4.types;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.lotuslabs.tree4.TreeNode;
import com.lotuslabs.tree4.TreePath;

@RunWith(JUnit4.class)
public class SSMutableTreeNodeTest {

	SSMutableTreeNode mutableTreeNode;
	TreeNode<String,String> treeNode;

	@Before
	public void setUp() {
		mutableTreeNode = SSMutableTreeNode.valueOf(new String[] {
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


		treeNode = mutableTreeNode;
	}

	@Test
	public void testGenerateTreeOuput_root() {
		assertEquals(" K8:0    |___K3:D    |   |___K1:G    |   |   |___K0:H    |   |   |___K2:F    |   |___K4:E    |   |   |___K5:A    |   |   |___K7:C    |   |   |   |___K6:B    |___K11:X    |   |___K10:Y    |   |   |___K9:Z"
				, mutableTreeNode.generateTreeOutput().replaceAll(System.lineSeparator(), " "));
	}

	@Test
	public void testGet() {
		assertEquals("E not matched", "E",
				mutableTreeNode.get(new TreePath<>(new String[] {"K8", "K3", "K4"})).getUserObject());

	}

}
