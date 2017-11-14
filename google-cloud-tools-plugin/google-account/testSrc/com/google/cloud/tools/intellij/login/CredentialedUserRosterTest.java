/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.tools.intellij.login;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.util.containers.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for {@link CredentialedUserRoster}.
 */
public class CredentialedUserRosterTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private CredentialedUserRoster users;
  private CredentialedUser user1;
  private CredentialedUser user2;
  private CredentialedUser user3;

  @Before
  public void setUp() {
    users = new CredentialedUserRoster();
    user1 = new CredentialedUser("user1");
    user2 = new CredentialedUser("user2");
    user3 = new CredentialedUser("user3");
  }

  @Test
  public void testAddUser() {
    Assert.assertEquals(0, users.numberOfUsers());

    users.addUser(user1);
    Assert.assertEquals(user1.getEmail(), users.getActiveUser().getEmail());

    users.addUser(user2);
    Assert.assertEquals(user2.getEmail(), users.getActiveUser().getEmail());
    Assert.assertTrue(user2.isActive());
    Assert.assertFalse(user1.isActive());

    Map<String, CredentialedUser> allUsers = users.getAllUsers();
    Assert.assertEquals(2, allUsers.size());
    Assert.assertNotNull(allUsers.get(user1.getEmail()));
    Assert.assertNotNull(allUsers.get(user2.getEmail()));
  }

  @Test
  public void testGetActiveUser() {
    Assert.assertNull(users.getActiveUser());

    users.addUser(user1);
    Assert.assertNotNull(users.getActiveUser());
    Assert.assertEquals(user1.getEmail(), users.getActiveUser().getEmail());

    users.addUser(user2);
    Assert.assertEquals(user2.getEmail(), users.getActiveUser().getEmail());

    users.addUser(user3);
    Assert.assertEquals(user3.getEmail(), users.getActiveUser().getEmail());

    users.removeUser(user2.getEmail());
    Assert.assertEquals("user3", users.getActiveUser().getEmail());

    users.removeUser(users.getActiveUser().getEmail());
    Assert.assertNull(users.getActiveUser());

    boolean exceptionThrown = false;
    try{
      users.setActiveUser(user2.getEmail());
    }
    catch (IllegalArgumentException ex) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);

    users.setActiveUser(user1.getEmail());
    Assert.assertTrue(user1.isActive());
  }

  @Test
  public void testGetAllUsers() {
    Assert.assertEquals(0, users.getAllUsers().size());

    users.addUser(user1);
    Assert.assertEquals(1, users.getAllUsers().size());

    users.addUser(user2);
    users.addUser(user3);
    Assert.assertEquals(3, users.getAllUsers().size());

    users.addUser(user3);

    Assert.assertTrue(users.removeUser(user1.getEmail()));
    Assert.assertEquals(2, users.getAllUsers().size());
  }

  @Test
  public void testIsActiveUserAvailable() {
    Assert.assertFalse(users.isActiveUserAvailable());

    users.addUser(user1);
    Assert.assertTrue(users.isActiveUserAvailable());

    users.addUser(user2);
    users.addUser(user3);
    Assert.assertTrue(users.isActiveUserAvailable());

    users.removeUser(user2.getEmail());
    Assert.assertEquals(user3.getEmail(), users.getActiveUser().getEmail());

    users.removeUser(users.getActiveUser().getEmail());
    Assert.assertNull(users.getActiveUser());

    users.setActiveUser(user1.getEmail());
    Assert.assertTrue(user1.isActive());
  }

  @Test
  public void testNumberOfUsers() {
    Assert.assertEquals(0, users.numberOfUsers());

    users.addUser(user1);
    Assert.assertEquals(1, users.numberOfUsers());

    users.removeUser(user1.getEmail());
    Assert.assertEquals(0, users.numberOfUsers());

    users.addUser(user1);
    users.addUser(user2);
    Assert.assertEquals(2, users.numberOfUsers());
  }

  @Test
  public void testRemoveActiveUser() {
    Assert.assertNull(users.getActiveUser());

    users.addUser(user1);
    Assert.assertEquals(user1.getEmail(), users.getActiveUser().getEmail());
    users.removeActiveUser();
    Assert.assertNull(users.getActiveUser());
    Assert.assertFalse(user1.isActive());

    users.addUser(user2);
    users.addUser(user3);
    Assert.assertEquals(user3.getEmail(), users.getActiveUser().getEmail());
    users.removeActiveUser();
    Assert.assertNull(users.getActiveUser());
    Assert.assertFalse(user3.isActive());
  }

  @Test
  public void testRemoveUser() {
    Assert.assertFalse(users.removeUser(user1.getEmail()));

    users.addUser(user1);
    users.addUser(user2);
    Assert.assertEquals(user2.getEmail(), users.getActiveUser().getEmail());

    Assert.assertTrue(users.removeUser(user2.getEmail()));
    Assert.assertEquals(1, users.numberOfUsers());
    Assert.assertNull(users.getActiveUser());
  }

  @Test
  public void testSetActiveUser() {
    users.addUser(user1);
    users.addUser(user2);
    users.addUser(user3);

    Assert.assertEquals(user3.getEmail(), users.getActiveUser().getEmail());
    users.setActiveUser(user1.getEmail());
    Assert.assertEquals(user1.getEmail(), users.getActiveUser().getEmail());

    boolean exceptionThrown = false;
    try{
      users.setActiveUser("noUser");
    } catch (IllegalArgumentException ex) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
    Assert.assertTrue(user1.isActive());
  }

  @Test
  public void testSetAllUsers() {
    users.addUser(user1);

    Map<String, CredentialedUser> newUsers = new HashMap<String, CredentialedUser>();
    newUsers.put(user2.getEmail(), user2);
    newUsers.put(user3.getEmail(), user3);

    users.setAllUsers(newUsers);
    Map<String, CredentialedUser> setUsers = users.getAllUsers();
    Assert.assertEquals(2, setUsers.size());
    Assert.assertFalse(setUsers.containsKey(user1.getEmail()));
    Assert.assertTrue(setUsers.containsKey(user2.getEmail()));
    Assert.assertTrue(setUsers.containsKey(user3.getEmail()));
  }

  @Test
  public void testRemoveAllUsers() {
    Assert.assertEquals(0, users.numberOfUsers());

    users.addUser(user1);
    users.removeAllUsers();
    Assert.assertEquals(0, users.numberOfUsers());
    Assert.assertEquals(null, users.getActiveUser());

    users.addUser(user1);
    users.addUser(user2);
    users.addUser(user3);
    users.removeAllUsers();
    Assert.assertEquals(0, users.numberOfUsers());
    Assert.assertEquals(null, users.getActiveUser());
  }
}
