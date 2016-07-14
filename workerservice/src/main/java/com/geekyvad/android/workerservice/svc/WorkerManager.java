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

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.support.annotation.NonNull;
import com.geekyvad.android.workerservice.wrk.HandlerWorker;
import com.geekyvad.android.workerservice.util.LogUtils;
import com.geekyvad.android.workerservice.wrk.ThreadWorker;
import org.greenrobot.eventbus.EventBus;

import java.util.*;

public class WorkerManager
{
  /* Options and Flags */

  static final int FLAG_DEFAULT = 0;
  static final int FLAG_WAIT = 0x1;

  /* Initialization */

  public WorkerManager( Application app, WorkerService service, Handler serviceHandler )
  {
    mContext = app.getApplicationContext();
    mWorkerService = service;
    mMainSvcHandler = serviceHandler;
  }

  public void registerWorkerClass( String workerType, Class< ? extends IWorker> workerClass )
  {
    mWorkerClasses.put( workerType, workerClass );
  }

  /* Operations */

  /**
   * Could block because waits for finishing of existing worker if any
   * @param workerType Worker name as defined in worker's class
   * @param startIntent Starting intent that was passed to service's onStartCommand
   */
  synchronized protected void startWorker( String workerType, String workerName, Intent startIntent )
  {
    LogUtils.LOGV( TAG, String.format( "startWorker: %s (%s)", workerType, workerName ) );
    Class<? extends IWorker> workerClass = mWorkerClasses.get( workerType );
    if( workerClass == null ) {
      throw new IllegalArgumentException( "Unknown worker type: " + workerType );
    }
    if( mBlockNewWorkers ) {
      LogUtils.LOGE( TAG, "Worker manager is blocked - No more workers allowed!" );
      return;
    }

    // Do nothing if worker is already running
    IWorker existingWorker = getWorker( workerName );
    if( existingWorker != null ) {
      LogUtils.LOGD( TAG, "Worker is already running: " + workerName );
      EventBus.getDefault().post( new WorkerServiceEvents.WorkerAlreadyRunning( workerName ) );
      return;
    }

    // Start worker
    IWorker worker;
    try {
      worker = workerClass.newInstance();
      Bundle workerParams = startIntent.getBundleExtra( WorkerService.PARAM_WORKER_PARAMS );
      worker.init( mContext, workerName, workerParams, this );
      mWorkers.put( workerName, worker );
    } catch( InstantiationException e ) {
      LogUtils.LOGE( TAG, "Error creating worker object: " + workerType, e );
      if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ) {
        throw new RuntimeException( e );
      } else {
        return;
      }
    } catch( IllegalAccessException e ) {
      LogUtils.LOGE( TAG, "Error creating worker object: " + workerType, e );
      if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ) {
        throw new RuntimeException( e );
      } else {
        return;
      }
    }
    LogUtils.LOGD( TAG, "Worker started: " + worker.getWorkerName() + " [" + workerType + "]" );
  }

  synchronized protected void stopWorker( String workerName, int flags )
  {
    LogUtils.LOGV( TAG, "stopWorker: " + workerName );

    // Stop worker
    Thread wrkThread = (Thread) mWorkers.get( workerName );
    if( wrkThread != null && wrkThread.isAlive() ) {
      LogUtils.LOGD( TAG, "Worker is running: " + workerName );
      IWorker iworker = (IWorker) wrkThread;

      // Send stop request
      if( wrkThread instanceof HandlerWorker ) {
        // Handler worker need to be treated more softly
        HandlerWorker handlerWorker = (HandlerWorker) wrkThread;
        handlerWorker.sendQuit();
      } else if( wrkThread instanceof ThreadWorker ) {
        wrkThread.interrupt();
      }

      // Wait for worker to finish if flag is set
      if( ( flags & FLAG_WAIT ) != 0 ) {
        LogUtils.LOGV( TAG, "Waiting for worker exit: " + workerName );
        try {
          wrkThread.join();
        } catch( InterruptedException e ) {
          LogUtils.LOGW( TAG,
              "Interrupted while waiting for worker exit " + workerName + "; aborting" );
        }
      }

      LogUtils.LOGD( TAG, "Worker stopped: " + workerName );
    } else {
      LogUtils.LOGD( TAG, "Worker already stopped: " + workerName );
      EventBus.getDefault().post( new WorkerServiceEvents.WorkerAlreadyStopped( workerName ) );
      if( getWorkerCount() == 0 ) {
        notifyNoMoreWorkers();
      }
    }

  }

  public void sendWorkerMessage( @NonNull String workerName, Message message )
  {
    LogUtils.LOGV( TAG, "sendWorkerMessage: " + workerName );
    IWorker worker = getWorker( workerName );
    if( worker == null ) {
      // Worker is not running
      if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ) {
        throw new IllegalStateException( "Worker " + workerName + " is NOT running" );
      }
      LogUtils.LOGE( TAG, "Can't send message to worker " + workerName + " because it's not running" );
      return;
    }
    if( ! ( worker instanceof HandlerWorker ) ) {
      // Worker type can't receive messages
      throw new IllegalArgumentException( "Worker " + workerName +
          " can't receive messages because it's not of type "
          + HandlerWorker.class.getSimpleName() );
    }

    HandlerWorker handlerWorker = (HandlerWorker) worker;
    // Safely obtain handler of the worker
    // (if worker just started it may happen it didn't initialized handler yet)
    synchronized( handlerWorker ) {
      while( handlerWorker.getHandler() == null ) {
        try {
          LogUtils.LOGV( TAG, "Waiting while worker " + handlerWorker.getWorkerName()
              + " is creating its handler" );
          handlerWorker.wait();
        } catch( InterruptedException e ) {
          // Get out from the method as worker service likely being killed
          return;
        }
      }
    }
    handlerWorker.getHandler().sendMessage( message );
  }

  synchronized protected void shutdown( int flags )
  {
    LogUtils.LOGV( TAG, "Shutting down all workers" );
    mBlockNewWorkers = true;
    Set<String> workerNames = mWorkers.keySet();
    for( String wName : workerNames ) {
      stopWorker( wName, flags );
    }
    mContext = null;
    mMainSvcHandler = null;
    mWorkers.clear();
    mWorkers = null;
  }

  /* Internal message handling */

  public void onWorkerExit( final IWorker worker )
  {
    LogUtils.LOGV( TAG, "onWorkerExit: " + worker.getWorkerName() );
    // Post to main service thread we've honestly killed the worker
    if( mMainSvcHandler != null ) {
      LogUtils.LOGV( TAG, "Posting handleOnWorkerExit" );
      mMainSvcHandler.post( new Runnable()
      {
        @Override
        public void run()
        {
          handleOnWorkerExit( worker );
        }
      } );
    }
  }

  protected synchronized void handleOnWorkerExit( IWorker worker )
  {
    LogUtils.LOGD( TAG, "Worker finished: " + worker.getWorkerName() );
    String workerName = worker.getWorkerName();
    LogUtils.LOGV( TAG, "Handling onWorkerExit: " + workerName );
    if( mWorkers != null ) {
      if( !mWorkers.containsKey( workerName ) ) {
        LogUtils.LOGW( TAG, "Worker already removed: " + workerName );
        return;
      }
      mWorkers.remove( workerName );
      LogUtils.LOGV( TAG, "Worker removed: " + workerName );
      if( getWorkerCount() == 0 ) {
        notifyNoMoreWorkers();
      }
    } else {
      LogUtils.LOGV( TAG, "Workers map already cleaned up" );
    }
  }

  protected void notifyNoMoreWorkers()
  {
    // Notify service that there are no more workers
    LogUtils.LOGD( TAG, "No more workers" );
    Message msg = mMainSvcHandler.obtainMessage(
        WorkerService.MSG_WHAT_INTERNAL_STATUS, WorkerService.MSG_ARG1_NO_MORE_WORKERS, 0 );
    mMainSvcHandler.sendMessage( msg );
  }

  /* Workers API */

  public int getWorkerCount()
  {
    return mWorkers.size();
  }

  public List<String> getRunningWorkers()
  {
    return new ArrayList<>( mWorkers.keySet() );
  }

  public IWorker getWorker( @NonNull String workerName )
  {
    return mWorkers.get( workerName );
  }

  public void setBlockNewWorkers( boolean block )
  {
    mBlockNewWorkers = block;
  }

  public boolean isBlockNewWorkers()
  {
    return mBlockNewWorkers;
  }

  public WorkerService getWorkerService()
  {
    return mWorkerService;
  }

  public void postToUiThread( Runnable runnable )
  {
    if( mMainHandler == null ) {
      mMainHandler = new Handler( Looper.getMainLooper() );
    }
    mMainHandler.post( runnable );
  }

  /* Data members */

  private Context mContext;
  private WorkerService mWorkerService;
  private Handler mMainSvcHandler;
  private Map<String, IWorker> mWorkers = new HashMap<>();
  private final Map<String, Class<? extends IWorker>> mWorkerClasses = new HashMap<>();
  /**
   * If true - new workers will be rejected
   */
  protected volatile boolean mBlockNewWorkers;

  protected  Handler mMainHandler;

  private static final String TAG = LogUtils.makeLogTag( WorkerManager.class );
}
