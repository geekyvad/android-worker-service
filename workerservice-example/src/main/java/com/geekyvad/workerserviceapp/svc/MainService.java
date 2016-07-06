package com.geekyvad.workerserviceapp.svc;

import com.geekyvad.workerservice.svc.WorkerManager;
import com.geekyvad.workerservice.svc.WorkerService;
import com.geekyvad.workerservice.svc.WorkerServiceConfig;
import com.geekyvad.workerserviceapp.svc.counter.SimpleCounter;
import com.geekyvad.workerserviceapp.svc.restartable.RestartableCounter;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class MainService extends WorkerService
{

  @Override
  protected void onRegisterWorkerClasses( WorkerManager workerManager )
  {
    workerManager.registerWorkerClass( SimpleCounter.TYPE, SimpleCounter.class );
    workerManager.registerWorkerClass( RestartableCounter.TYPE, RestartableCounter.class );
  }

  @Override
  protected WorkerServiceConfig onCreateConfiguration()
  {
    return super.onCreateConfiguration()
        .setThrowRuntimeExceptions( true );
  }
}
