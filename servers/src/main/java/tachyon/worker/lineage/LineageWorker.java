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

package tachyon.worker.lineage;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;

import tachyon.Constants;
import tachyon.conf.TachyonConf;
import tachyon.heartbeat.HeartbeatContext;
import tachyon.heartbeat.HeartbeatThread;
import tachyon.util.ThreadFactoryUtils;
import tachyon.util.network.NetworkAddressUtils;
import tachyon.util.network.NetworkAddressUtils.ServiceType;
import tachyon.worker.WorkerBase;
import tachyon.worker.WorkerContext;
import tachyon.worker.WorkerIdRegistry;
import tachyon.worker.block.BlockDataManager;

/**
 * This class is responsible for managing all top level components of the lineage worker.
 */
public final class LineageWorker extends WorkerBase {
  /** Logic for managing lineage file persistence */
  private final LineageDataManager mLineageDataManager;
  /** Client for lineage master communication. */
  private final LineageMasterClient mLineageMasterWorkerClient;
  /** Configuration object */
  private final TachyonConf mTachyonConf;

  /** The service that persists files for lineage checkpointing */
  private Future<?> mFilePersistenceService;

  public LineageWorker(BlockDataManager blockDataManager) throws IOException {
    super(Executors.newFixedThreadPool(3, ThreadFactoryUtils.build("lineage-worker-heartbeat-%d",
        true)));
    Preconditions.checkState(WorkerIdRegistry.getWorkerId() != 0, "Failed to register worker");

    mTachyonConf = WorkerContext.getConf();
    mLineageDataManager =
        new LineageDataManager(Preconditions.checkNotNull(blockDataManager));

    // Setup MasterClientBase
    mLineageMasterWorkerClient = new LineageMasterClient(
        NetworkAddressUtils.getConnectAddress(ServiceType.MASTER_RPC, mTachyonConf), mTachyonConf);
  }

  public void start() {
    mFilePersistenceService =
        getExecutorService().submit(new HeartbeatThread(HeartbeatContext.WORKER_LINEAGE_SYNC,
            new LineageWorkerMasterSyncExecutor(mLineageDataManager, mLineageMasterWorkerClient),
            mTachyonConf.getInt(Constants.WORKER_LINEAGE_HEARTBEAT_INTERVAL_MS)));
  }

  public void stop() {
    if (mFilePersistenceService != null) {
      mFilePersistenceService.cancel(true);
    }
    mLineageMasterWorkerClient.close();
    getExecutorService().shutdown();
  }
}
