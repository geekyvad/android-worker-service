package com.geekyvad.workerservice.wrk;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.geekyvad.workerservice.util.LogUtils;
import com.geekyvad.workerservice.svc.IWorker;
import com.geekyvad.workerservice.svc.WorkerManager;
import com.geekyvad.workerservice.svc.WorkerServiceConfig;
import com.geekyvad.workerservice.svc.WorkerServiceEvents;
import org.greenrobot.eventbus.EventBus;

/**
 * @author Vadim Zadorozhny
 */
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
