/*
 * Copyright (C) 2016  Vadim Zadorozhny
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
package com.geekyvad.android.workerserviceapp.wrk.counter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.geekyvad.android.workerservice.util.LogUtils;
import com.geekyvad.android.workerservice.wrk.ThreadWorker;
import com.geekyvad.android.workerserviceapp.wrk.MainService;
import com.geekyvad.android.workerserviceapp.wrk.restartable.RestartableCounter;
import org.greenrobot.eventbus.EventBus;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class SimpleCounter extends ThreadWorker
{
  public static final String TYPE = "MyWorker1"; // SimpleCounter.class.getSimpleName();
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
        LogUtils.LOGV( TAG, "" + i );
      } catch( InterruptedException e ) {
        e.printStackTrace();
        // Do not break - simulate worker that needs time to exit
        // break;
      }
    }
    // to reproduce bug
    //RestartableCounter.setRange( getContext(), 10, 12 );
  }

  private static final String TAG = LogUtils.makeLogTag( SimpleCounter.class );
}
