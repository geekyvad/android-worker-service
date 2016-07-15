package com.geekyvad.android.workerservice.svc;

import com.geekyvad.android.workerservice.util.LogUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Vadim Zadorozhny (created on 15.07.2016)
 */
public class WorkerServiceStarter
{
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
