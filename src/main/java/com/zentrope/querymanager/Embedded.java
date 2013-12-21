package com.zentrope.querymanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Embedded {

  private static Logger log = LoggerFactory.getLogger(Embedded.class);
  private static boolean failIfNotFound = true;

  public static boolean start() {
    try {
      clojure.lang.RT.loadResourceScript("query_manager/main.clj", failIfNotFound);
      clojure.lang.RT.var("query-manager.main", "start-embedded!").invoke();
      return true;
    }

    catch (Throwable t) {
      log.error(t.getMessage());
      return false;
    }
  }

  public static boolean stop() {
    try {
      clojure.lang.RT.loadResourceScript("query_manager/main.clj", failIfNotFound);
      clojure.lang.RT.var("query-manager.main", "stop-embedded!").invoke();
      return true;
    }

    catch (Throwable t) {
      log.error(t.getMessage());
      return false;
    }
  }

}
