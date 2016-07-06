package com.geekyvad.workerservice;

/**
 * @author Vadim Zadorozhny
 */
public interface BindableRunnable extends Runnable
{
  void bind( Object b );
  Object getBoundObject();
}
