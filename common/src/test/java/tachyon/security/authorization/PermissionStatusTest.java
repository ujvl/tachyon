/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.security.authorization;

import org.junit.Assert;
import org.junit.Test;

import org.powermock.reflect.Whitebox;

import tachyon.Constants;
import tachyon.conf.TachyonConf;
import tachyon.security.LoginUser;
import tachyon.security.authentication.AuthType;
import tachyon.security.authentication.PlainSaslServer;

/**
 * Tests the {@link PermissionStatus} class.
 */
public final class PermissionStatusTest {

  /**
   * Tests the {@link PermissionStatus#getDirDefault()} method.
   */
  @Test
  public void permissionStatusTest() {
    PermissionStatus permissionStatus =
        new PermissionStatus("user1", "group1", FileSystemPermission.getDefault());

    verifyPermissionStatus("user1", "group1", (short) 0777, permissionStatus);

    permissionStatus = PermissionStatus.getDirDefault();

    verifyPermissionStatus("", "", (short) 0777, permissionStatus);
  }

  /**
   * Tests the {@link PermissionStatus#applyUMask(FileSystemPermission)} method.
   */
  @Test
  public void applyUMaskTest() {
    FileSystemPermission umaskPermission = new FileSystemPermission((short)0022);
    PermissionStatus permissionStatus =
        new PermissionStatus("user1", "group1", FileSystemPermission.getDefault());
    permissionStatus = permissionStatus.applyUMask(umaskPermission);

    Assert.assertEquals(FileSystemAction.ALL, permissionStatus.getPermission().getUserAction());
    Assert.assertEquals(FileSystemAction.READ_EXECUTE,
        permissionStatus.getPermission().getGroupAction());
    Assert.assertEquals(FileSystemAction.READ_EXECUTE,
        permissionStatus.getPermission().getOtherAction());
    Assert.assertEquals(0755, permissionStatus.getPermission().toShort());
  }

  /**
   * Tests the {@link PermissionStatus#get(TachyonConf, boolean)} method.
   *
   * @throws Exception thrown if the status cannot be retrieved
   */
  @Test
  public void getPermissionStatusTest() throws Exception {
    TachyonConf conf = new TachyonConf();
    PermissionStatus permissionStatus;

    // no authentication
    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.NOSASL.getAuthName());
    permissionStatus = PermissionStatus.get(conf, true);
    verifyPermissionStatus("", "", (short) 0000, permissionStatus);

    // authentication is enabled, and remote is true
    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.SIMPLE.getAuthName());
    PlainSaslServer.AuthorizedClientUser.set("test_client_user");
    permissionStatus = PermissionStatus.get(conf, true);
    verifyPermissionStatus("test_client_user", "", (short) 0755, permissionStatus);

    // authentication is enabled, and remote is false
    Whitebox.setInternalState(LoginUser.class, "sLoginUser", (String) null);
    System.setProperty(Constants.SECURITY_LOGIN_USERNAME, "test_login_user");
    permissionStatus = PermissionStatus.get(conf, false);
    verifyPermissionStatus("test_login_user", "", (short) 0755, permissionStatus);
    System.clearProperty(Constants.SECURITY_LOGIN_USERNAME);
  }

  private void verifyPermissionStatus(String user, String group, short permission,
      PermissionStatus permissionStatus) {
    Assert.assertEquals(user, permissionStatus.getUserName());
    Assert.assertEquals(group, permissionStatus.getGroupName());
    Assert.assertEquals(permission, permissionStatus.getPermission().toShort());
  }
}
