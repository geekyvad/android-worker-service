package com.geekyvad.workerserviceapp.svc.counter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.geekyvad.workerservice.wrk.ThreadWorker;
import com.geekyvad.workerserviceapp.svc.MainService;
import org.greenrobot.eventbus.EventBus;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class SimpleCounter extends ThreadWorker
{
  public static final String TYPE = "MyWorker1";
  public static final String NAME = "CountWorker";

  public static void startWorker( @NonNull Context context )
  {
    Intent intent = MainService.createStartWorkerIntent( context, TYPE, NAME, null );
    MainService.processIntent( context, intent );
  }

  public static void stopWorker( @NonNull Context context, boolean wait )
  {
    MainService
        .processIntent( context, MainService.createStopWorkerIntent( context, NAME, wait ) );
  }

  public static final int COUNT_LIMIT = 10;

  @Override
  public void onCreate( @Nullable Bundle params )
  {
    EventBus.getDefault().postSticky( SimpleCounterEvents.CounterStatus.obtain()
        .setCompleted( false )
        .setCount( 0 ) );
  }

  @Override
  public void onDestroy()
  {
    EventBus.getDefault().postSticky( SimpleCounterEvents.CounterStatus.obtain().setCompleted( true ) );
  }

  @Override
  public void onRun()
  {
    for( int i = 0; i < COUNT_LIMIT; i++ ) {
      try {
        EventBus.getDefault().postSticky( SimpleCounterEvents.CounterStatus.obtain().setCount( i ) );
        if( i + 1 < COUNT_LIMIT ) {
          sleep( 1000 );
        }
      } catch( InterruptedException e ) {
        e.printStackTrace();
        break;
      }
    }
  }
}
