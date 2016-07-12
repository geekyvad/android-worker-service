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
package com.geekyvad.android.workerserviceapp.svc.restartable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.geekyvad.android.workerservice.wrk.HandlerWorker;
import com.geekyvad.android.workerserviceapp.svc.MainService;
import org.greenrobot.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class RestartableCounter extends HandlerWorker
{
  public static final String TYPE = "MyWorker2";
  public static final String NAME = "RestartableCountWorker";

  public static void createWorker( @NonNull Context context )
  {
    Intent intent = MainService.createStartWorkerIntent( context, TYPE, NAME, null );
    MainService.processIntent( context, intent );
  }

  public static void stopWorker( @NonNull Context context, boolean wait )
  {
    MainService
        .processIntent( context, MainService.createStopWorkerIntent( context, NAME, wait ) );
  }

  public enum Command
  {
    START,
    STOP,
    RANGE,
  }

  public static void setRange( @NonNull Context context, int start, int end )
  {
    Message msg = new Message();
    msg.what = Command.RANGE.ordinal();
    msg.arg1 = start;
    msg.arg2 = end;
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
    Intent intent = MainService.createWorkerMessageIntent( context, NAME, msg );
    MainService.processIntent( context, intent );
  }

  public static void start( @NonNull Context context )
  {
    Message msg = new Message();
    msg.what = Command.START.ordinal();
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
  }

  public static void stop( @NonNull Context context )
  {
    Message msg = new Message();
    msg.what = Command.STOP.ordinal();
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
  }

  public static final int COUNT_LIMIT = 20;

  @Override
  public void onCreate( @Nullable Bundle params )
  {
    mEnd = COUNT_LIMIT;
  }

  @Override
  public void onMessage( Message message )
  {
    Command cmd = Command.values()[ message.what ];
    switch( cmd ) {
    case START:
      restartCounter();
      break;
    case STOP:
      cancelSubscription();
      break;
    case RANGE:
      mStart = message.arg1;
      mEnd = message.arg2;
      break;
    }
  }

  @Override
  public void onDestroy()
  {
    cancelSubscription();
  }

  private void restartCounter()
  {
    cancelSubscription();
    rx.Observable<Long> counterObservable = rx.Observable.interval( 100, 1000, TimeUnit.MILLISECONDS );
    mSubscription = counterObservable
        .doOnSubscribe( new Action0()
        {
          @Override
          public void call()
          {
            EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCompleted( false ) );
          }
        } )
        .map( new Func1<Long, Integer>()
        {
          @Override
          public Integer call( Long aLong )
          {
            return aLong.intValue();
          }
        } )
    .take( mEnd - mStart + 1)
    .observeOn( AndroidSchedulers.from( getLooper() ) )
    .subscribe( new rx.Observer<Integer>() {
      @Override
      public void onNext( Integer integer )
      {
        EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCount( integer + mStart ) );
      }

      @Override
      public void onCompleted()
      {
        EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCompleted( true ) );
      }

      @Override
      public void onError( Throwable e )
      {

      }
    });
  }

  private void cancelSubscription()
  {
    if( mSubscription != null && !mSubscription.isUnsubscribed() ) {
      mSubscription.unsubscribe();
    }
    EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCompleted( true ) );
  }

  private int mStart;
  private int mEnd;

  rx.Subscription mSubscription;
}
