package com.huan.tomcat.jdbc.vendor;

public interface ExceptionSorter {

    /**
     * Return true if throwable is fatal
     * @param t  the throwable
     * @return
     */
    boolean isExceptionFatal(Throwable t);
}
