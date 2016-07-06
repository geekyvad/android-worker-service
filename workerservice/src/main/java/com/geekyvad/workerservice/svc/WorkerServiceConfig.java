package com.geekyvad.workerservice.svc;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class WorkerServiceConfig
{

  public static WorkerServiceConfig getInstance()
  {
    return ourInstance;
  }

  public WorkerServiceConfig setThrowRuntimeExceptions( boolean throwRuntimeExceptions )
  {
    mThrowRuntimeExceptions = throwRuntimeExceptions;
    return this;
  }

  public boolean isThrowRuntimeExceptions()
  {
    return mThrowRuntimeExceptions;
  }

  public WorkerServiceConfig setVerboseLoggable( boolean verboseLoggable )
  {
    mVerboseLoggable = verboseLoggable;
    return this;
  }

  public boolean isVerboseLoggable()
  {
    return mVerboseLoggable;
  }

  private WorkerServiceConfig()
  {
  }

  private static WorkerServiceConfig ourInstance = new WorkerServiceConfig();

  private boolean mThrowRuntimeExceptions;
  private boolean mVerboseLoggable;
}
