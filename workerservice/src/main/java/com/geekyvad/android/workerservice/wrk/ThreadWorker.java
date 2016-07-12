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
package com.geekyvad.android.workerservice.wrk;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.geekyvad.android.workerservice.util.LogUtils;
import com.geekyvad.android.workerservice.svc.IWorker;
import com.geekyvad.android.workerservice.svc.WorkerManager;
import com.geekyvad.android.workerservice.svc.WorkerServiceConfig;
import com.geekyvad.android.workerservice.svc.WorkerServiceEvents;
import org.greenrobot.eventbus.EventBus;

abstract public class ThreadWorker extends Thread implements IWorker
{
  /* Config */

  public static final int DEFAULT_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND;

  /* Subclasses must override */

  abstract public void onRun();

  /* Construction */

  /**
   * Default constructor is required
   */
  public ThreadWorker()
  {
    setPriority( DEFAULT_PRIORITY );
  }

  /**
   * Initializes and starts worker.<br>
   * Called from caller's thread (not from worker thread).
   */
  @Override
  public void init( @NonNull Context context, @NonNull String workerName, @Nullable Bundle params,
      @NonNull WorkerManager manager )
  {
    mContext = context;
    mWorkerName = workerName;
    mParams = params;
    mManager = manager;
    setName( getWorkerName() );
    start();
  }

  @Override
  public String getWorkerName()
  {
    return mWorkerName;
  }

  /* Workflow */

  @Override
  final public void run()
  {
    try {
      LogUtils.LOGV( TAG, "Calling " + getWorkerName() + ".onCreate()" );
      EventBus.getDefault().post( new WorkerServiceEvents.WorkerStarted( getWorkerName() ) );
      onCreate( getParams() );
      LogUtils.LOGV( TAG, "Calling " + getWorkerName() + ".onRun()" );
      onRun();
    } catch( Exception e ) {
      LogUtils.LOGE( TAG, String.format( "Worker %s thrown exception: ", getName() ) );
      if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ) {
        throw e;
      }
    } finally {
      try {
        LogUtils.LOGV( TAG, "Calling " + getWorkerName() + ".onDestroy()" );
        onDestroy();
        EventBus.getDefault().post( new WorkerServiceEvents.WorkerStopped( getWorkerName() ) );
      } catch( Exception e ) {
        LogUtils.LOGE( TAG, String.format(
            "Worker %s thrown exception during destroy: ", getName() ), e );
      }
      // Notify manager that worker is about exiting
      mManager.onWorkerExit( this );

      // Cleanup
      mContext = null;
      mParams = null;
      mManager = null;
    }
  }

  /* Internal functionality */

  protected Context getContext()
  {
    return mContext;
  }

  @Nullable
  public Bundle getParams()
  {
    return mParams;
  }

  @NonNull
  protected WorkerManager getManager()
  {
    return mManager;
  }

  /* Data members */

  private Context mContext;
  private Bundle mParams;
  private String mWorkerName;
  private WorkerManager mManager;

  @SuppressWarnings( "unused" )
  /**
   * If true, worker is being restarted by the worker manager.
   * This flag is for internal use by {@link WorkerManager}.
   */
  private boolean mRestarting;

  private static final String TAG = LogUtils.makeLogTag( ThreadWorker.class );
}
