package com.huan.tomcat.jdbc.vendor;

import com.huan.tomcat.jdbc.vendor.ExceptionSorter;
import org.apache.tomcat.jdbc.pool.ConnectionPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author edenhuan
 */
public class ExceptionSorterManager {
    protected static final String DATA_SOURCE_DRIVE_MYSQL = "com.mysql.jdbc.Driver";


    private static final Map<ConnectionPool, ExceptionSorter> exceptionSorters = new ConcurrentHashMap<>();


    public static void setSorter(ConnectionPool pool) {
        if (pool == null || pool.getPoolProperties() == null) {
            return;
        }

        ExceptionSorter sorter = exceptionSorters.getOrDefault(pool, null);
        if (sorter != null) {
            return;
        }

        switch (pool.getPoolProperties().getDriverClassName()) {
            case DATA_SOURCE_DRIVE_MYSQL: {
                exceptionSorters.put(pool, new MysqlExceptionSorter());
                break;
            }
            default: {
                //NOOP
            }
        }
    }

    public static ExceptionSorter getSorter(ConnectionPool pool) {
        if (pool == null) {
            return null;
        }
        return exceptionSorters.getOrDefault(pool, null);
    }

    public static void deleteSorter(ConnectionPool pool) {
        if (pool == null) {
            return;
        }
        exceptionSorters.remove(pool);
    }
}
