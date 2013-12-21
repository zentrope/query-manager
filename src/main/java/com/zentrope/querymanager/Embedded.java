package com.zentrope.querymanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import clojure.lang.RT;
import clojure.lang.Var;

public class Embedded {

  private static Logger log = LoggerFactory.getLogger(Embedded.class);
  private static boolean failIfNotFound = true;

  public static boolean start() {
    try {
      RT.loadResourceScript("query_manager/main.clj", failIfNotFound);
      RT.var("query-manager.main", "startEmbedded").invoke();
      return true;
    }

    catch (Throwable t) {
      log.error(t.getMessage());
      return false;
    }
  }

  public static boolean stop() {
    try {
      RT.loadResourceScript("query_manager/main.clj", failIfNotFound);
      RT.var("query-manager.main", "stopEmbedded").invoke();
      return true;
    }

    catch (Throwable t) {
      log.error(t.getMessage());
      return false;
    }
  }

}
