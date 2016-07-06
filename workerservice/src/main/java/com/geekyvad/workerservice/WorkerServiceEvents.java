package com.geekyvad.workerservice;

/**
 * @author Vadim Zadorozhny
 */
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
