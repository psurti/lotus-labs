package com.lotuslabs.tree4.types;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
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
		mutableTreeNode = SSMutableTreeNode.withValues(new String[] {
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

	@Test
	public void testGet_type() {
		Class<?> actualClass = mutableTreeNode.get(new TreePath<>(new String[] {"K8", "K3", "K4"})).getClass();
		System.out.println( "actualClass=" + actualClass);
		assertEquals(SSMutableTreeNode.class, actualClass);
	}

	@Test
	public void testWithPaths_KeyString() {
		String[] flatProperties = new String[] {
				"spring.datasource.url",
				"spring.datasource.username",
				"spring.datasource.password",
				"spring.datasource.testWhileIdle",
				"spring.datasource.validationQuery",
				"spring.jpa.show-sql",
				"spring.jpa.hibernate.ddl-auto",
				"spring.jpa.hibernate.naming-strategy",
				"spring.jpa.properties.hibernate.dialect"
		};
		SSMutableTreeNode tree = SSMutableTreeNode.withPaths(flatProperties,'.');
		System.out.println( tree.generateTreeOutput() );
	}


	@Test
	public void testWithPaths_Map() {
		Map<String,String> propertyMap = new HashMap<>();
		propertyMap.put("spring.datasource.url", "jdbc:mysql://localhost:3306/netgloo_blog?useSSL=false");
		propertyMap.put("spring.datasource.username","root");
		propertyMap.put("spring.datasource.password","root");
		propertyMap.put("spring.datasource.testWhileIdle","true");
		propertyMap.put("spring.datasource.validationQuery","SELECT 1");
		propertyMap.put("spring.jpa.show-sql","true");
		propertyMap.put("spring.jpa.hibernate.ddl-auto","update");
		propertyMap.put("spring.jpa.hibernate.naming-strategy","org.hibernate.cfg.ImprovedNamingStrategy");
		propertyMap.put("spring.jpa.properties.hibernate.dialect","org.hibernate.dialect.MySQL5Dialect");

		SVMutableTreeNode<String> tree = SVMutableTreeNode.<String>withPaths(propertyMap,'.');
		System.out.println( tree.generateTreeOutput() );
		SVMutableTreeNode<String> node = tree.get(new TreePath<>(new String[] {"spring", "jpa", "hibernate", "ddl-auto"}));
		System.out.println( node.getTreePath() + "|spring.jpa.hibernate.ddl-auto=" + node.getUserObject());
		Assert.assertEquals("update", node.getUserObject());
	}

}
