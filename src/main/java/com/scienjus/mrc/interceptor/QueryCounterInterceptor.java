package com.scienjus.mrc.interceptor;

import com.scienjus.mrc.annotation.Counter;
import net.sf.cglib.proxy.Enhancer;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Properties;

/**
 * @author ScienJus
 * @date 16/2/7.
 */

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class })
})
public class QueryCounterInterceptor implements Interceptor {

    private ProxyCounterInterceptor proxy;

    public void setJedisPool(JedisPool jedisPool) {
        proxy = new ProxyCounterInterceptor(jedisPool);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object retVal = invocation.proceed();
        Class clazz = retVal.getClass();
        if (clazz.isAnnotationPresent(Counter.class)) {
            return proxy(retVal);
        } else if (List.class.isAssignableFrom(clazz)) {
            List list = ((List) retVal);
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item.getClass().isAnnotationPresent(Counter.class)) {
                    list.set(i, proxy(item));
                }
            }
        }
        return retVal;
    }

    private Object proxy(Object obj) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(obj.getClass());
        enhancer.setCallback(proxy);
        Object counter = enhancer.create();
        proxy.clone(obj, counter);
        return counter;
    }

    @Override
    public Object plugin(Object obj) {
        return Plugin.wrap(obj, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
