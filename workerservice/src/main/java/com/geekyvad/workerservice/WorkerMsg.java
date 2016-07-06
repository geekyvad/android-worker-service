package com.geekyvad.workerservice;

/**
 * @author Vadim Zadorozhny
 */
abstract public class WorkerMsg implements BindableRunnable
{
  @Override
  public void bind( Object b )
  {
    mBoundObject = b;
  }

  @Override
  public Object getBoundObject()
  {
    return mBoundObject;
  }

  private Object mBoundObject;
}
