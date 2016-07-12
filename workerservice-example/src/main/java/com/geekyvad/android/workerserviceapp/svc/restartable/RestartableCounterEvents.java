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

import org.greenrobot.eventbus.EventBus;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class RestartableCounterEvents
{
  public static class CounterStatus
  {
    public int count;
    public boolean completed;

    public CounterStatus()
    {
      completed = true;
    }

    public CounterStatus setCount( int c )
    {
      this.count = c;
      return this;
    }

    public CounterStatus setCompleted( boolean completed )
    {
      this.completed = completed;
      return this;
    }

    public static CounterStatus obtain()
    {
      CounterStatus status = EventBus.getDefault().getStickyEvent( CounterStatus.class );
      if( status == null ) {
        status = new CounterStatus();
      }
      return status;
    }
  }

}
