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

public class WorkerServiceEvents
{
  public static class BaseWorkerEvent
  {
    public BaseWorkerEvent()
    {

    }

    public BaseWorkerEvent( String workerName )
    {
      this.workerName = workerName;
    }

    public String workerName;
  }

  public static class WorkerStarted extends BaseWorkerEvent
  {
    public WorkerStarted()
    {
    }

    public WorkerStarted( String workerName )
    {
      super( workerName );
    }
  }

  public static class WorkerStopped extends BaseWorkerEvent
  {
    public WorkerStopped()
    {
    }

    public WorkerStopped( String workerName )
    {
      super( workerName );
    }
  }

  public static class WorkerAlreadyStopped extends BaseWorkerEvent
  {
    public WorkerAlreadyStopped()
    {
    }

    public WorkerAlreadyStopped( String workerName )
    {
      super( workerName );
    }
  }

  public static class WorkerAlreadyRunning extends BaseWorkerEvent
  {
    public WorkerAlreadyRunning()
    {
    }

    public WorkerAlreadyRunning( String workerName )
    {
      super( workerName );
    }
  }


  /**
   * Sticky
   */
  public static class ServiceActivityStatus
  {
    public boolean started;
    public boolean shuttingDown;
  }

}
