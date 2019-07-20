# TomcatJdbcInterceptor-ConnectionErrorTrap
Tomcat jdbc连接池可以通过test-on-borrow、test-on-return、test-on=idle来验证连接池中数据库连接的有效性。但是这些检测方法都是有时间间隔的，如果在时间间隔内，数据库连接出现异常，并不会被释放，从而导致使用此连接的一系列sql操作都会执行失败。

此项目通过自定义一个tomcat jdbc连接池的interceptor，catch连接执行时sql时抛出的sql异常，从而释放掉异常连接，保证连接池中连接的可用性。

由于不同类型数据库抛出的异常种类不同，此工程中仅处理连接mysql数据库时的异常处理。如有使用其余数据库，可对应修改。

同时本项目提供拦截器 jmx Mbean的实现。可用于监控连接池释放连接情况。
