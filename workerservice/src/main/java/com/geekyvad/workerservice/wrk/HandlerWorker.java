package com.geekyvad.workerservice.wrk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.*;
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
abstract public class HandlerWorker extends HandlerThread implements IWorker
{
  /* Config */

  public static final int DEFAULT_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND;

  /* Subclasses must override */

  @Override
  abstract public void onCreate( @Nullable Bundle params );

  /**
   * For convenience this method is being called by {@link MessageHandler#handleMessage(Message msg)}.
   * @param message  Message posted to handler
   */
  abstract public void onMessage( Message message );

  /**
   * Release any resources. Called even if exception raised in onRun().
   */
  @Override
  abstract public void onDestroy();

  /* Message handling */

  //@SuppressLint( "HandlerLeak" )
  public class MessageHandler extends Handler
  {
    @SuppressWarnings( "unused" )
    public MessageHandler()
    {
      super();
    }

    @SuppressWarnings( "unused" )
    public MessageHandler( Callback callback )
    {
      super( callback );
    }

    public MessageHandler( Looper looper )
    {
      super( looper );
    }

    @SuppressWarnings( "unused" )
    public MessageHandler( Looper looper, Callback callback )
    {
      super( looper, callback );
    }

    @Override
    public void handleMessage( Message msg )
    {
      onMessage( msg );
    }
  } // MessageHandler

  /**
   * Base implementation creates {@link HandlerWorker.MessageHandler}.
   * Subclasses may create their own handler but it must be subclass of
   * {@link HandlerWorker.MessageHandler}.<br>
   * This method called from caller's thread (not worker thread)
   */
  public MessageHandler onCreateMessageHandler( Looper looper )
  {
    return new MessageHandler( looper );
  }

  /* Construction */

  public HandlerWorker()
  {
    super( "Unnamed handler worker", DEFAULT_PRIORITY );
  }

  /**
   * Initializes and starts worker.<br>
   * Called from caller's thread (not from worker thread).
   */
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
      EventBus.getDefault().post( new WorkerServiceEvents.WorkerStarted( getWorkerName() ) );
      LogUtils.LOGV( TAG, "Entering message loop" );
      super.run();
    } catch( Exception e ) {
      LogUtils.LOGE( TAG, String.format( "Worker %s thrown exception: %s", getName(), e.toString() ) );
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

  @Override
  protected void onLooperPrepared()
  {
    super.onLooperPrepared();
    synchronized( this ) {
      mHandler = onCreateMessageHandler( getLooper() );
      notifyAll();
    }
    LogUtils.LOGV( TAG, "Calling " + getWorkerName() + ".onCreate()" );
    onCreate( getParams() );
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

  public MessageHandler getHandler()
  {
    return mHandler;
  }

  public void sendQuit()
  {
    if( isAlive() ) {
      if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
        quitSafely();
      } else {
        quit();
      }
    }
  }

  /* Data members */

  private Context mContext;
  private Bundle mParams;
  private String mWorkerName;
  private WorkerManager mManager;

  private MessageHandler mHandler;


  private static final String TAG = LogUtils.makeLogTag( HandlerWorker.class );

}
