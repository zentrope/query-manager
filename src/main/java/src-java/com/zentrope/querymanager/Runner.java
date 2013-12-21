package com.zentrope.querymanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zentrope.querymanager.Embedded;

public class Runner {

  private static Logger log = LoggerFactory.getLogger(Runner.class);

  private static class ShutdownHook {

    public static void add(Runnable r) {
      Runtime.getRuntime().addShutdownHook(new Thread(r));
    }
  }

  private static class Lock {

    final static Object lock = new Object();

    public static void acquire() throws InterruptedException {
      synchronized (lock) {
        lock.wait();
      };
    }

    public static void release() {
      synchronized (lock) {
        lock.notify();
      }
    }
  }

  public static void main(String[] args) throws Exception {

    Runnable stopper = new Runnable() {
        public void run() {
          log.info("Stopping embedded Query Manager.");
          Embedded.stop();
        }
      };

    Runnable releaser = new Runnable() {
        @Override public void run() {
          Lock.release();
        }
      };

    log.info("Starting embedded Query Manager.");
    Embedded.start();

    ShutdownHook.add(stopper);
    ShutdownHook.add(releaser);

    log.info(" - Try ^C to exit.");
    Lock.acquire();
  }

}
