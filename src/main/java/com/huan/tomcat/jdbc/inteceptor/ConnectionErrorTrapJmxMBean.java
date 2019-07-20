package com.huan.tomcat.jdbc.inteceptor;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

/**
 * @author edenhuan
 */
public interface ConnectionErrorTrapJmxMBean {
    public CompositeData[] getPoolConnectionErrorCount() throws OpenDataException;
}
