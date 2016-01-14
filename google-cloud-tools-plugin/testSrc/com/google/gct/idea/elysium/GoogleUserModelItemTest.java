package com.google.gct.idea.elysium;

import com.google.gct.login.CredentialedUser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.awt.Image;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

@RunWith(MockitoJUnitRunner.class)
public class GoogleUserModelItemTest {

  @Mock private CredentialedUser user;
  @Mock private Image image;

  private DefaultTreeModel model;

  @Before
  public void setUp() {
    Mockito.when(user.getEmail()).thenReturn("foo@example.com");
    Mockito.when(user.getName()).thenReturn("Jane Smith");
    Mockito.when(user.getPicture()).thenReturn(image);

    TreeNode root = new DefaultMutableTreeNode();
    model = new DefaultTreeModel(root);
  }

  @Test
  public void testGetters() {
    GoogleUserModelItem item = new GoogleUserModelItem(user, model);
    Assert.assertEquals(user, item.getCredentialedUser());
    Assert.assertEquals("foo@example.com", item.getEmail());
    Assert.assertEquals("Jane Smith", item.getName());
    Assert.assertEquals(image, item.getImage());
    Assert.assertFalse(item.isSynchronizing());
  }

}
