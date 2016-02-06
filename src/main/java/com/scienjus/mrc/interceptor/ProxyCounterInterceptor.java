package com.scienjus.mrc.interceptor;

import com.scienjus.mrc.annotation.Id;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
public class ProxyCounterInterceptor implements MethodInterceptor {

    private JedisPool jedisPool;

    public ProxyCounterInterceptor(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        String methodName = method.getName();
        Class clazz = method.getDeclaringClass();

        MethodType methodType;
        if (methodName.startsWith("set")) {
            methodType = MethodType.SET;
        } else if (methodName.startsWith("incr")) {
            methodType = MethodType.INCR;
        } else if (methodName.startsWith("decr")) {
            methodType = MethodType.DECR;
        } else {
            return proxy.invokeSuper(obj, args);
        }
        String fieldName = getFieldName(methodName, methodType);
        Field field = clazz.getDeclaredField(fieldName);
        if (field == null || !field.isAnnotationPresent(com.scienjus.mrc.annotation.Field.class) || field.isAnnotationPresent(Id.class)) {
            return proxy.invokeSuper(obj, args);
        }
        try (Jedis jedis = jedisPool.getResource()) {
            field.setAccessible(true);
            String redisKey = getRedisKey(clazz, obj);
            if (!jedis.hexists(redisKey, fieldName)) {
                jedis.hset(redisKey, fieldName, field.get(obj).toString());
            }
            Long val;
            switch (methodType) {
                case SET:
                    jedis.hset(redisKey, fieldName, args[0].toString());
                    val = Long.parseLong(args[0].toString());
                    break;
                case INCR:
                    val = jedis.hincrBy(redisKey, fieldName, 1);
                    break;
                case DECR:
                    val = jedis.hincrBy(redisKey, fieldName, -1);
                    break;
                default:
                    return proxy.invokeSuper(obj, args);
            }
            field.set(obj, convert(field.getType(), val));
            return val;
        }
    }

    private static Object convert(Class<?> clazz, Long val) {
        if (clazz.isAssignableFrom(String.class)) {
            return val.toString();
        } else if (clazz.isAssignableFrom(Byte.TYPE) || clazz.isAssignableFrom(Byte.class)) {
            return val.byteValue();
        } else if (clazz.isAssignableFrom(Short.TYPE) || clazz.isAssignableFrom(Short.class)) {
            return val.shortValue();
        } else if (clazz.isAssignableFrom(Integer.TYPE) || clazz.isAssignableFrom(Integer.class)) {
            return val.intValue();
        } else if (clazz.isAssignableFrom(Long.TYPE) || clazz.isAssignableFrom(Long.class)) {
            return val;
        } else if (clazz.isAssignableFrom(Float.TYPE) || clazz.isAssignableFrom(Float.class)) {
            return val.floatValue();
        } else if (clazz.isAssignableFrom(Double.TYPE) || clazz.isAssignableFrom(Double.class)) {
            return val.doubleValue();
        }
        throw new NumberFormatException("counter Long value can not convert to " + clazz.getName());
    }

    public void clone(Object from, Object to) {
        Class clazz = from.getClass();
        String redisKey = getRedisKey(clazz, from);
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> cache = jedis.hgetAll(redisKey);
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (cache.get(field.getName()) != null) {
                    field.set(to, convert(field.getType(), Long.parseLong(cache.get(field.getName()))));
                } else {
                    field.set(to, field.get(from));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("clone failed " + e.getMessage());
        }
    }

    private static String getRedisKey(Class clazz, Object obj) {
        String id = getId(clazz, obj);
        if (id != null) {
            return clazz.getName() + "_Counter_" + id;
        } else {
            return clazz.getName() + "_Counter";
        }

    }

    private static String getId(Class clazz, Object obj) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return field.get(obj).toString();
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String getFieldName(String methodName, MethodType methodType) {
        String fieldName = methodName.substring(methodType.len());
        return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
    }

    private enum MethodType {
        SET,
        INCR,
        DECR;

        public int len() {
            return this.name().length();
        }
    }
}
