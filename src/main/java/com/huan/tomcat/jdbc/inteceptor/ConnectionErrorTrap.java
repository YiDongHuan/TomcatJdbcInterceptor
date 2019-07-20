package com.huan.tomcat.jdbc.inteceptor;

import com.huan.test.logback.vendor.ExceptionSorter;
import com.huan.test.logback.vendor.ExceptionSorterManager;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.interceptor.AbstractCreateStatementInterceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author edenhuan
 * Connection error trap interceptor. Release connection when sql execute error.
 * <p>
 * You need modify properties when you use this interceptor in spring project,such as:
 * spring.datasource.tomcat.jdbc-interceptors=com.webank.weboot.repo.tomcat.interceptor.ConnectionErrorTrap
 * </p>
 * Everytime an operation is called on the {@link java.sql.Connection} object the
 * {@link #invoke(Object, Method, Object[])}method on the interceptor will be
 * called. And the interceptor will release current connection When catch SQLException.
 * Everytime an operation is called on the {@link Statement} object the
 * {@link StatementProxy#invoke(Object, Method, Object[])} will be called. And
 * the StatementProxy will release current connection When catch SQLException.
 * The interceptor also record the num of released connection.
 */
public class ConnectionErrorTrap extends AbstractCreateStatementInterceptor {
    protected PooledConnection con;
    protected ConnectionPool pool;

    private static final Log log = LogFactory.getLog(ConnectionErrorTrap.class);

    protected static final Map<ConnectionPool, LongAdder> counts = new HashMap<>();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            if (checkIfNeedDiscardConnection(t)) {
                setConnectionDiscard();
            }
            throw t;
        }
    }

    /**
     * the constructors that are used to create statement proxies
     */
    @Override
    public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
        try {
            String name = method.getName();
            Constructor<?> constructor = null;
            String sql = null;
            if (compare(CREATE_STATEMENT, name)) {
                // createStatement
                constructor = getConstructor(CREATE_STATEMENT_IDX, Statement.class);
            } else if (compare(PREPARE_STATEMENT, name)) {
                // prepareStatement
                constructor = getConstructor(PREPARE_STATEMENT_IDX, PreparedStatement.class);
                sql = (String) args[0];
            } else if (compare(PREPARE_CALL, name)) {
                // prepareCall
                constructor = getConstructor(PREPARE_CALL_IDX, CallableStatement.class);
                sql = (String) args[0];
            } else {
                // do nothing
                return statement;
            }
            return constructor.newInstance(new Object[]{new ConnectionErrorTrap.StatementProxy(statement, sql)});
        } catch (Exception x) {
            log.warn("Unable to create statement proxy.", x);
        }
        return statement;
    }

    @Override
    public void closeInvoked() {
        //NOOP
    }

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {
        this.pool = parent;
        this.con = con;
    }

    @Override
    public void poolClosed(ConnectionPool pool) {
        ExceptionSorterManager.deleteSorter(pool);
        deleteCount(pool);
        super.poolClosed(pool);
    }

    @Override
    public void poolStarted(ConnectionPool pool) {
        super.poolStarted(pool);
        ExceptionSorterManager.setSorter(pool);
        initCount(pool);
    }

    /**
     * Class to release connect when catch connection exception
     */
    protected class StatementProxy implements InvocationHandler {
        protected Object delegate;
        protected final String query;

        public StatementProxy(Object parent, String query) {
            this.delegate = parent;
            this.query = query;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (compare(TOSTRING_VAL, method)) {
                return toString();
            }
            if (compare(EQUALS_VAL, method)) {
                return Boolean.valueOf(
                        this.equals(Proxy.getInvocationHandler(args[0])));
            }
            if (compare(HASHCODE_VAL, method)) {
                return Integer.valueOf(this.hashCode());
            }
            if (compare(CLOSE_VAL, method)) {
                if (delegate == null) {
                    return null;
                }
            }
            if (compare(ISCLOSED_VAL, method)) {
                if (delegate == null) {
                    return Boolean.TRUE;
                }
            }
            if (delegate == null) {
                throw new SQLException("Statement closed.");
            }
            Object result = null;
            try {
                //invoke next
                result = method.invoke(delegate, args);
            } catch (Throwable t) {
                if (checkIfNeedDiscardConnection(t)) {
                    setConnectionDiscard();
                }
                throw t;
            }
            //perform close cleanup
            if (compare(CLOSE_VAL, method)) {
                delegate = null;
            }
            return result;
        }
    }

    protected boolean checkIfNeedDiscardConnection(Throwable t) {
        ExceptionSorter exceptionSorter = ExceptionSorterManager.getSorter(pool);
        if (con != null && !con.isDiscarded() && exceptionSorter != null && exceptionSorter.isExceptionFatal(t)) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Current Connection info: lastConnected()=").append(con.getLastConnected())
                    .append(",lastValidate()=").append(con.getLastValidated()).append(".")
                    .append("Current connection pool has released ").append(getCount(pool))
                    .append("connection. And we will set current connection discard because of SQLException:");
            log.error(logMessage.toString(), t);
            return true;
        }
        return false;
    }

    protected void setConnectionDiscard() {
        con.setDiscarded(true);
        increment(pool);
    }

    public static void initCount(ConnectionPool pool) {
        if (pool == null) {
            return;
        }
        synchronized (counts) {
            counts.put(pool, new LongAdder());
        }
    }

    public static void deleteCount(ConnectionPool pool) {
        if (pool == null) {
            return;
        }
        synchronized (counts) {
            counts.remove(pool);
        }
    }

    public static void increment(ConnectionPool pool) {
        if (pool == null) {
            return;
        }
        LongAdder count = counts.getOrDefault(pool, null);
        if (count == null) {
            synchronized (counts) {
                count = counts.getOrDefault(pool, null);
                if (count == null) {
                    count = new LongAdder();
                    counts.put(pool, count);
                }
            }
        }
        count.increment();
    }

    public static int getCount(ConnectionPool pool) {
        if (pool == null) {
            return 0;
        }

        LongAdder count = counts.getOrDefault(pool, null);
        if (count != null) {
            return count.intValue();
        }
        return 0;
    }
}
