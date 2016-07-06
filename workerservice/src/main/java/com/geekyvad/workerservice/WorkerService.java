package com.geekyvad.workerservice;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Vadim Zadorozhny
 */
abstract public class WorkerService extends Service
{

  /* Service commands */

  public static final String ACTION_START_WORKER = "action.StartWorker";
  public static final String ACTION_STOP_WORKER = "action.StopWorker";

  /**
   * String containing worker type as defined in appropriate worker's TYPE constant.
   */
  public static final String PARAM_WORKER_TYPE = "param.WorkerType";

  /**
   * String containing worker name as it depending on worker type and parameters.
   * Note that service does nothing if worker with given name is already running.
   */
  public static final String PARAM_WORKER_NAME = "param.WorkerName";

  /**
   * Bundle containing parameters for worker.
   */
  public static final String PARAM_WORKER_PARAMS = "param.WorkerParams";

  /**
   * Boolean.
   */
  public static final String PARAM_WAIT_FOR_WORKER_EXIT = "param.WaitWrkExit";

  /**
   * Boolean.
   */
  public static final String PARAM_FORCE_SERVICE_SHUTDOWN = "param.ForceShutdown";

  /**
   * Shutdown worker manager, all workers and stopSelf for service. By default it waits
   * for workers when they exit.
   */
  public static final String ACTION_SHUTDOWN_SERVICE = "action.ShutdownService";

  public static final String ACTION_SEND_WORKER_MSG = "action.SendWorkerMsg";

  public static final String PARAM_WORKER_MSG = "WorkerMsgParam.MSG";

  public static final String DEBUG_ACTION_LOG_ACTIVE_WORKER_NAMES = "debugAction.LogActiveWorkers";

  /* Initialization */

  /**
   * Must be called from your {@link Application#onCreate()}
   * @param workerServiceClass Class that you've derived from {@link WorkerService} and put to your
   *                           app's manifest
   */
  public static void setWorkerServiceClass( Class workerServiceClass )
  {
    stWorkerServiceClass = workerServiceClass;
    LogUtils.LOGD( TAG, "Worker service class assigned: " + workerServiceClass.getName() );
  }

  /* Action helpers */

  public static void processIntent( @NonNull Context context, Intent intent )
  {
    context.startService( intent );
  }

  public static void shutdownService( @NonNull Context context, boolean wait, boolean force )
  {
    // Prepare intent
    Intent intent = createActionIntent( context, ACTION_SHUTDOWN_SERVICE );
    if( wait ) {
      intent.putExtra( PARAM_WAIT_FOR_WORKER_EXIT, true );
    }
    if( force ) {
      intent.putExtra( PARAM_FORCE_SERVICE_SHUTDOWN, true );
    }

    // Update service status sticky event - service is shutting down
    WorkerServiceEvents.ServiceActivityStatus status = obtainServiceStatusEvent();
    status.shuttingDown = true;
    EventBus.getDefault().postSticky( status );

    processIntent( context, intent );
  }

  /* Intent helpers */

  public static Intent createStartWorkerIntent( @NonNull Context context, @NonNull String workerType,
      @NonNull String workerName, @Nullable Bundle workerParams )
  {
    Intent intent = createActionIntent( context, ACTION_START_WORKER );
    intent.putExtra( PARAM_WORKER_TYPE, workerType );
    intent.putExtra( PARAM_WORKER_NAME, workerName );
    if( workerParams != null ) {
      intent.putExtra( PARAM_WORKER_PARAMS, workerParams );
    }
    return intent;
  }

  public static Intent createStopWorkerIntent( @NonNull Context context, @NonNull String workerName,
      boolean wait )
  {
    Intent intent = createActionIntent( context, ACTION_STOP_WORKER );
    intent.putExtra( PARAM_WORKER_TYPE, workerName );
    if( wait ) {
      intent.putExtra( PARAM_WAIT_FOR_WORKER_EXIT, true );
    }
    return intent;
  }

  public static Intent createWorkerMessageIntent( @NonNull Context context,
      @NonNull String workerName, Message message )
  {
    Intent intent = createActionIntent( context, ACTION_SEND_WORKER_MSG );
    intent.putExtra( PARAM_WORKER_TYPE, workerName );
    intent.putExtra( PARAM_WORKER_MSG, message );
    return intent;
  }

  public static Intent createActionIntent( @NonNull Context context, @NonNull String action )
  {
    Intent intent = new Intent( context, stWorkerServiceClass );
    intent.setAction( action );
    return intent;
  }

  public static WorkerServiceEvents.ServiceActivityStatus obtainServiceStatusEvent()
  {
    WorkerServiceEvents.ServiceActivityStatus status =
        EventBus.getDefault().getStickyEvent( WorkerServiceEvents.ServiceActivityStatus.class );
    if( status == null ) {
      status = new WorkerServiceEvents.ServiceActivityStatus();
    }
    return status;
  }

  /* Overridables */

  /** Override to register your workers */
  abstract protected void onRegisterWorkerClasses( WorkerManager workerManager );

  protected WorkerServiceConfig onCreateConfiguration()
  {
    return WorkerServiceConfig.getInstance();
  }

  protected ServiceHandler onCreateServiceHandler( HandlerThread serviceThread )
  {
    return new ServiceHandler( serviceThread.getLooper() );
  }

  protected WorkerManager onCreateWorkerManager( Application app, ServiceHandler serviceHandler )
  {
    return new WorkerManager( getApplication(), mServiceHandler );
  }

  /* Lifecycle */

  public WorkerService()
  {
    LogUtils.LOGV( TAG, "Constructor" );
  }

  @Override
  public void onCreate()
  {
    LogUtils.LOGV( TAG, "onCreate" );
    super.onCreate();

    mConfiguration = onCreateConfiguration();

    // Start up the thread running the service.  Note that we create a
    // separate thread because the service normally runs in the process's
    // main thread, which we don't want to block.  We also make it
    // background priority so CPU-intensive work will not disrupt our UI.
    HandlerThread thread = new HandlerThread( "ServiceHandlerThread",
        Process.THREAD_PRIORITY_BACKGROUND );
    thread.start();

    // Get the HandlerThread's Looper and use it for our Handler
    mServiceHandler = onCreateServiceHandler( thread );

    // Worker manager will notify main service when last worker finished its job
    mWorkerManager = onCreateWorkerManager( getApplication(), mServiceHandler );
    onRegisterWorkerClasses( mWorkerManager );

    // Notify that service has started
    stServiceCreated.set( true );
    WorkerServiceEvents.ServiceActivityStatus status = obtainServiceStatusEvent();
    status.started = true;
    status.shuttingDown = false;
    EventBus.getDefault().postSticky( status );
  }

  @Override
  public void onDestroy()
  {
    LogUtils.LOGV( TAG, "onDestroy" );
    mServiceHandler = null;
    super.onDestroy();
    // Notify that service has stopped
    WorkerServiceEvents.ServiceActivityStatus status = obtainServiceStatusEvent();
    status.shuttingDown = false;
    status.started = false;
    EventBus.getDefault().postSticky( status );
    // Clear 'shutting down' flag

    stServiceCreated.set( false );
    mConfiguration = null;
  }

  private static final int START_MODE = START_NOT_STICKY;

  @Override
  public int onStartCommand( Intent intent, int flags, int startId )
  {
    if( WorkerServiceConfig.getInstance().isVerboseLoggable() ) {
      String logging = "onStartCommand\n  " + intent;
      if( intent != null ) {
        String action = intent.getAction();
        if( action != null && !action.isEmpty() ) {
          if( ACTION_START_WORKER.equals( action ) ) {
            String workerType = intent.getStringExtra( PARAM_WORKER_TYPE );
            String workerName = intent.getStringExtra( PARAM_WORKER_NAME );
            logging += String.format( "\n  Worker: %s (%s)", workerType, workerName );
          }
        }
      }
      LogUtils.LOGV( TAG, logging );
    }

    if( mQuitting ) {
      // Mo more commands
      LogUtils.LOGE( TAG, "Service is shutting down! New commands not accepted." );
      return START_MODE;
    }

    //noinspection StatementWithEmptyBody
    if( intent != null ) {
      // Send intent to handler thread
      Message msg = mServiceHandler.obtainMessage( MSG_WHAT_SERVICE_COMMAND );
      Bundle msgData = new Bundle();
      msgData.putParcelable( MSG_DATA_INTENT, intent );
      msg.setData( msgData );
      mServiceHandler.sendMessage( msg );
    } else {
      // TODO handle service restarted by the system
    }

    return START_MODE;
  }

  @Override
  public IBinder onBind( Intent intent )
  {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException( "Not yet implemented" );
  }

  public static boolean isCreated()
  {
    return stServiceCreated != null && stServiceCreated.get();
  }

  /* Message handling */

  static final int MSG_WHAT_SERVICE_COMMAND = 1;
  static final int MSG_WHAT_INTERNAL_STATUS = 2;
  static final int MSG_ARG1_NO_MORE_WORKERS = 1;
  static final String MSG_DATA_INTENT = "handlerMsgData.intent";

  protected class ServiceHandler extends Handler
  {
    public ServiceHandler( Looper looper )
    {
      super( looper );
    }

    @Override
    public void handleMessage( Message msg )
    {
      LogUtils.LOGV( TAG, "handleMessage" );
      if( MSG_WHAT_SERVICE_COMMAND == msg.what ) {
        onCommand( msg );
      } else if( MSG_WHAT_INTERNAL_STATUS == msg.what ) {
        onInternalStatus( msg );
      } else if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ){
        throw new UnsupportedOperationException( "Unknown message command: " + msg.arg1 );
      }
    }

    private void onCommand( Message msg )
    {
      LogUtils.LOGV( TAG, "onCommand" );
      Bundle msgData = msg.getData();
      Intent intent = msgData.getParcelable( MSG_DATA_INTENT );
      if( intent == null ) {
        throw new IllegalArgumentException( "Intent is null" );
      }
      String action = intent.getAction();
      if( action != null ) {
        LogUtils.LOGV( TAG, "Action: " + action );
        if( ACTION_START_WORKER.equals( action ) ) {
          handleStartWorker( intent );
        } else if( ACTION_SEND_WORKER_MSG.equals( action ) ) {
          handleSendWorkerMsg( intent );
        } else if( ACTION_STOP_WORKER.equals( action ) ) {
          handleStopWorker( intent );
        } else if( ACTION_SHUTDOWN_SERVICE.equals( action ) ) {
          onShutdown( intent );
        } else if( DEBUG_ACTION_LOG_ACTIVE_WORKER_NAMES.equals( action ) ) {
          onLogActiveWorkers( intent );
        } else if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ) {
          throw new UnsupportedOperationException( "Unknown action: " + action );
        }
      } else if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ) {
        throw new UnsupportedOperationException( "Action is null" );
      }

    }

    private void onInternalStatus( Message msg )
    {
      LogUtils.LOGV( TAG, "onInternalStatus" );
      //noinspection StatementWithEmptyBody
      if( MSG_ARG1_NO_MORE_WORKERS == msg.arg1 ) {
        // Do not shutdown service when no more workers because it ends up unpredictably
        // if last worker initiates start of new workers and exits immediately. Service
        // committing shutdown and does not accept new workers to start.
        //onShutdown( null );
      } else if( WorkerServiceConfig.getInstance().isThrowRuntimeExceptions() ){
        throw new UnsupportedOperationException( "Unknown message command arg2: " + msg.arg2 );
      }
    }

    private void onShutdown( Intent intent )
    {
      LogUtils.LOGV( TAG, "onShutdown" );
      if( handleShutdown( intent ) ) {
        Looper looper = getLooper();
        // Quit looper
        if( looper != null ) {
          if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
            looper.quitSafely();
          } else {
            looper.quit();
          }
        }
        stopSelf();
      } else {
        LogUtils.LOGI( TAG, "Shutdown cancelled" );
        WorkerServiceEvents.ServiceActivityStatus status = obtainServiceStatusEvent();
        status.shuttingDown = false;
        EventBus.getDefault().postSticky( status );
      }
    }

    private void onLogActiveWorkers( Intent intent )
    {
      StringBuilder bld = new StringBuilder( "Active worker list:" );
      for( String wrkName : mWorkerManager.getRunningWorkers() ) {
        bld.append( "\n  " ).append( wrkName );
      }
      LogUtils.LOGV( TAG, bld.toString() );
    }

    private final String TAG = LogUtils.makeLogTag( "WorkerServiceHandler" );

  } // ServiceHandler

  private void handleStartWorker( Intent intent )
  {
    String workerType = intent.getStringExtra( PARAM_WORKER_TYPE );
    String workerName = intent.getStringExtra( PARAM_WORKER_NAME );
    mWorkerManager.startWorker( workerType, workerName, intent );
  }

  private void handleSendWorkerMsg( Intent intent )
  {
    String workerName = intent.getStringExtra( PARAM_WORKER_TYPE );
    Message message = intent.getParcelableExtra( PARAM_WORKER_MSG );
    mWorkerManager.sendWorkerMessage( workerName, message );
  }

  private void handleStopWorker( Intent intent )
  {
    String workerName = intent.getStringExtra( PARAM_WORKER_TYPE );
    boolean wait = intent.getBooleanExtra( PARAM_WAIT_FOR_WORKER_EXIT, false );
    mWorkerManager.stopWorker( workerName,
        wait ? WorkerManager.FLAG_WAIT : WorkerManager.FLAG_DEFAULT );
  }

  private boolean handleShutdown( Intent intent )
  {
    // Fix: long shutdown prevents starting workers if user exited and immediately launched app again
    //mQuitting = true;
    boolean wait = intent == null || intent.getBooleanExtra( PARAM_WAIT_FOR_WORKER_EXIT, true );
    mWorkerManager.shutdown( wait ? WorkerManager.FLAG_WAIT : WorkerManager.FLAG_DEFAULT );

    boolean proceedShutdown = true;
    // Check if new messages received during shutdown
    if( mServiceHandler.hasMessages( MSG_WHAT_SERVICE_COMMAND ) ) {
      LogUtils.LOGD( TAG, "There are pending commands in service's queue" );
      proceedShutdown = false;
      if( intent != null && intent.getBooleanExtra( PARAM_FORCE_SERVICE_SHUTDOWN, false ) ) {
        LogUtils.LOGD( TAG, "But service is FORCED to shutdown" );
        proceedShutdown = true;
      }
    }

    if( !proceedShutdown ) {
      // Restore worker manager
      mWorkerManager = new WorkerManager( getApplication(), mServiceHandler );
      mQuitting = false;
    }

    return proceedShutdown;
  }

  /* Data members */

  private static Class stWorkerServiceClass;

  private WorkerManager mWorkerManager;
  private ServiceHandler mServiceHandler;
  private WorkerServiceConfig mConfiguration;

  /**
   * Controls ability of server to receive commands during shutdown
   */
  private boolean mQuitting;

  private static AtomicBoolean stServiceCreated = new AtomicBoolean( false );

  private static final String TAG = LogUtils.makeLogTag( WorkerService.class );
}
