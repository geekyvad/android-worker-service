/*
 * Copyright (C) 2016 Vadim Zadorozhny
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.geekyvad.android.workerservice.svc;

import com.geekyvad.android.workerservice.util.LogUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class WorkerServiceStarter
{
  /**
   * Call from onCreate or onCreateView
   * @param callback Fragment or Activity
   */
  public void startService( Callback callback )
  {
    WorkerServiceEvents.ServiceActivityStatus status = EventBus.getDefault().getStickyEvent(
        WorkerServiceEvents.ServiceActivityStatus.class );
    if( status != null && status.shuttingDown ) {
      LogUtils.LOGD( TAG, "Worker service is shutting down - have to wait for complete" );
      // Have to wait until service shutdown complete before start it again
      mCallback = callback;
      mCallback.onOldInstanceShuttingDown();
      EventBus.getDefault().register( this );
    } else {
      // Service is ready to start workers
      callback.onStartWorkers();
    }
  }

  /**
   * Call from onDestroy
   */
  public void cancelStart()
  {
    EventBus.getDefault().unregister( this );
    mCallback = null;
  }

  public interface Callback
  {
    void onOldInstanceShuttingDown();
    void onStartWorkers();
  }

  @Subscribe( sticky = true, threadMode = ThreadMode.MAIN )
  @SuppressWarnings( "unused" )
  public void onServiceStatusChanged( WorkerServiceEvents.ServiceActivityStatus status )
  {
    if( ! status.shuttingDown ) {
      LogUtils.LOGV( TAG, "Worker service shutdown complete detected" );
      if( mCallback != null ) {
        mCallback.onStartWorkers();
        EventBus.getDefault().unregister( this );
        mCallback = null;
      }
    }
  }

  protected Callback mCallback;

  private static final String TAG = LogUtils.makeLogTag( WorkerServiceStarter.class );
}
