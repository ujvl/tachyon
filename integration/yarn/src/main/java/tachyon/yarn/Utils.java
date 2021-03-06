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

package tachyon.yarn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

/**
 * YARN related utils.
 */
public final class Utils {
  public static final String TACHYON_SETUP_SCRIPT = "tachyon-yarn-setup.sh";

  private Utils() {} // prevent instantiation

  /**
   * Creates a local resource for a file on HDFS.
   *
   * @param yarnConf YARN configuration
   * @param resourcePath known path of resource file on HDFS
   * @throws IOException if the file can not be found on HDFS
   */
  public static LocalResource createLocalResourceOfFile(YarnConfiguration yarnConf,
      String resourcePath) throws IOException {
    LocalResource localResource = Records.newRecord(LocalResource.class);

    Path jarHdfsPath = new Path(resourcePath);
    FileStatus jarStat = FileSystem.get(yarnConf).getFileStatus(jarHdfsPath);
    localResource.setResource(ConverterUtils.getYarnUrlFromPath(jarHdfsPath));
    localResource.setSize(jarStat.getLen());
    localResource.setTimestamp(jarStat.getModificationTime());
    localResource.setType(LocalResourceType.FILE);
    localResource.setVisibility(LocalResourceVisibility.PUBLIC);
    return localResource;
  }

  /**
   * Enum representing types of containers run by the yarn setup script. The strings here correspond
   * with the strings in integration/bin/tachyon-yarn-setup.sh.
   */
  public enum YarnContainerType {
    APPLICATION_MASTER("application-master"),
    TACHYON_MASTER("tachyon-master"),
    TACHYON_WORKER("tachyon-worker");

    private final String mName;

    YarnContainerType(String name) {
      mName = name;
    }

    public String getName() {
      return mName;
    }
  }

  public static String buildCommand(YarnContainerType containerType) {
    return buildCommand(containerType, new HashMap<String, String>());
  }

  /**
   * Creates a command string for running the Tachyon yarn setup script for the given type of yarn
   * container.
   *
   * @param containerType the type of container to build the command for
   * @param args arguments to pass to to the setup script
   * @return the built command string
   */
  public static String buildCommand(YarnContainerType containerType, Map<String, String> args) {
    CommandBuilder commandBuilder =
        new CommandBuilder("./" + TACHYON_SETUP_SCRIPT).addArg(containerType.getName());
    for (Entry<String, String> argsEntry : args.entrySet()) {
      commandBuilder.addArg(argsEntry.getKey(), argsEntry.getValue());
    }
    // Redirect stdout and stderr to yarn log files
    commandBuilder.addArg("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout");
    commandBuilder.addArg("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr");
    return commandBuilder.toString();
  }
}
