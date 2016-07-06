package com.geekyvad.workerserviceapp.svc.restartable;

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
