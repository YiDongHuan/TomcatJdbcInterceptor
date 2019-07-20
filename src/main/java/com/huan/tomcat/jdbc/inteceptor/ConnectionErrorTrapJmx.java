package com.huan.tomcat.jdbc.inteceptor;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.jmx.JmxUtil;

import javax.management.*;
import javax.management.openmbean.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author edenhuan
 * Connection error trap interceptor which can register as Mbean. Release connection and send
 * notification when set connection discard.
 */
public class ConnectionErrorTrapJmx extends ConnectionErrorTrap
        implements ConnectionErrorTrapJmxMBean, NotificationEmitter {
    protected String poolName = null;

    protected volatile NotificationBroadcasterSupport notifier = new NotificationBroadcasterSupport();
    private boolean isJmxPoolEnable;

    protected static volatile CompositeType CONNECTION_ERROR_COUNT_TYPE;

    public static final String CONNECTION_ERROR_NOTIFICATION = "CONNECTION ERROR";
    protected static final AtomicLong notifySequence = new AtomicLong(0);
    protected static final ConcurrentHashMap<String, ConnectionErrorTrapJmxMBean> mbeans = new ConcurrentHashMap<>();

    private static final Log log = LogFactory.getLog(ConnectionErrorTrapJmx.class);


    @Override
    protected void setConnectionDiscard() {
        super.setConnectionDiscard();
        notifyJmx();
    }

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {
        super.reset(parent, con);
        isJmxPoolEnable = false;
        if (parent != null) {
            this.poolName = parent.getName();
            registerJmx();
            if (parent.getJmxPool() != null) {
                isJmxPoolEnable = true;
            }
        } else {
            this.poolName = null;
        }
    }

    @Override
    public void poolClosed(ConnectionPool pool) {
        this.poolName = pool.getName();
        deregisterJmx();
        super.poolClosed(pool);
    }

    @Override
    public void poolStarted(ConnectionPool pool) {
        this.pool = pool;
        super.poolStarted(pool);
        this.poolName = pool.getName();
    }

    protected void registerJmx() {
        try {
            ObjectName oname = getObjectName(getClass(), poolName);
            if (mbeans.putIfAbsent(poolName, this) == null) {
                JmxUtil.registerJmx(oname, null, this);
            }
        } catch (MalformedObjectNameException e) {
            log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", e);
        } catch (RuntimeOperationsException e) {
            log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", e);
        }
    }


    protected void deregisterJmx() {
        try {
            if (mbeans.remove(poolName) != null) {
                ObjectName oname = getObjectName(getClass(), poolName);
                JmxUtil.unregisterJmx(oname);
            }
        } catch (MalformedObjectNameException e) {
            log.warn("Jmx deregistration failed.", e);
        } catch (RuntimeOperationsException e) {
            log.warn("Jmx deregistration failed.", e);
        }

    }

    protected void notifyJmx() {
        try {
            long sequence = notifySequence.incrementAndGet();
            if (isJmxPoolEnable && this.pool.getJmxPool().getObjectName() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("CONNECTION POOL:").append(poolName).append(" CATCH A CONNECTION ERROR AND WAIT TO DISCARD THE CONNECTION");
                this.pool.getJmxPool().notify(CONNECTION_ERROR_NOTIFICATION, sb.toString());
            } else {
                if (notifier != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("CONNECTION POOL:").append(poolName).append(" CATCH A CONNECTION ERROR AND WAIT TO DISCARD THE CONNECTION");
                    Notification notification =
                            new Notification(CONNECTION_ERROR_NOTIFICATION,
                                    this,
                                    sequence,
                                    System.currentTimeMillis(),
                                    sb.toString());

                    notifier.sendNotification(notification);
                }
            }
        } catch (RuntimeOperationsException e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to send failed query notification.", e);
            }
        }
    }


    public ObjectName getObjectName(Class<?> clazz, String poolName) throws MalformedObjectNameException {
        return new ObjectName(ConnectionPool.POOL_JMX_TYPE_PREFIX + clazz.getName() + ",name=" + poolName);
    }


    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
        notifier.addNotificationListener(listener, filter, handback);
    }


    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return notifier.getNotificationInfo();
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener);

    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener, filter, handback);
    }

    @Override
    public CompositeData[] getPoolConnectionErrorCount() throws OpenDataException {
        CompositeDataSupport[] result = null;
        if (counts != null) {
            result = new CompositeDataSupport[counts.size()];
            int pos = 0;
            for (Iterator<Map.Entry<ConnectionPool, LongAdder>> it = counts.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<ConnectionPool, LongAdder> countEntry = it.next();
                result[pos++] = getCompositeData(getCompositeType(), countEntry);
            }
        }
        return result;
    }

    public CompositeDataSupport getCompositeData(final CompositeType type, Map.Entry<ConnectionPool, LongAdder> entry) throws OpenDataException {
        Object[] values = new Object[]{
                entry.getKey().getName(), entry.getValue().intValue()
        };
        return new CompositeDataSupport(type, FIELD_NAMES, values);
    }

    protected static CompositeType getCompositeType() {
        if (CONNECTION_ERROR_COUNT_TYPE == null) {
            try {
                CONNECTION_ERROR_COUNT_TYPE = new CompositeType(
                        ConnectionErrorTrapJmx.class.getName(),
                        "Composite data type for connection error count statistics",
                        FIELD_NAMES,
                        FIELD_DESCRIPTIONS,
                        FIELD_TYPES);
            } catch (OpenDataException x) {
                log.warn("Unable to initialize composite data type for JMX stats and notifications.", x);
            }
        }
        return CONNECTION_ERROR_COUNT_TYPE;
    }


    static final String[] FIELD_NAMES = new String[]{
            "poolName",
            "errorCount",
    };

    static final String[] FIELD_DESCRIPTIONS = new String[]{
            "The ConnectionPool name",
            "The number of released error connection",
    };

    static final OpenType<?>[] FIELD_TYPES = new OpenType[]{
            SimpleType.STRING,
            SimpleType.INTEGER,
    };
}
