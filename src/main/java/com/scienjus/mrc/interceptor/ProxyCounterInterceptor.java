package com.scienjus.mrc.interceptor;

import com.scienjus.mrc.annotation.Counter;
import com.scienjus.mrc.annotation.Id;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
public class ProxyCounterInterceptor implements MethodInterceptor {

    private static final String UPDATE_AT = "update_at";

    private JedisPool jedisPool;

    public ProxyCounterInterceptor(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        String methodName = method.getName();
        Class clazz = method.getDeclaringClass();

        MethodType methodType = MethodType.valueOfMethodName(methodName);
        if (methodType == null) {
            return proxy.invokeSuper(obj, args);
        }

        String fieldName = getFieldName(methodName, methodType);
        Field field = clazz.getDeclaredField(fieldName);
        if (field == null || !field.isAnnotationPresent(com.scienjus.mrc.annotation.Field.class) || field.isAnnotationPresent(Id.class)) {
            return proxy.invokeSuper(obj, args);
        }
        field.setAccessible(true);
        String redisKey = getRedisKey(clazz, obj);
        String redisField = getRedisField(field);
        Long retVal = null;
        if (methodType == MethodType.GET) {
            boolean isRealtime = field.getAnnotation(com.scienjus.mrc.annotation.Field.class).realtime();
            if (isRealtime) {
                retVal = doGet(redisKey, redisField);
            }
        } else {
            int expire = ((Counter) clazz.getAnnotation(Counter.class)).expire();
            if (methodType == MethodType.SET) {
                retVal = doSet(redisKey, redisField, Long.parseLong(args[0].toString()), expire);
            } else {
                long defaultVal = Long.parseLong(String.valueOf(field.get(obj)));
                long increment = args.length == 1 ? Long.parseLong(String.valueOf(args[0])) : 1;
                if (methodType == MethodType.DECR) {
                    increment = -increment;
                }
                retVal = doIncr(redisKey, redisField, defaultVal, increment, expire);
            }
        }
        if (retVal != null) {
            field.set(obj, convert(field.getType(), retVal));
            return retVal;
        } else {
            return proxy.invokeSuper(obj, args);
        }
    }

    private Long doGet(String key, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            String val = jedis.hget(key, field);
            if (val != null) {
                return Long.parseLong(val);
            }
            return null;
        }
    }

    private Long doSet(String key, String field, long val, int expire) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (expire > 0) {
                Pipeline p = jedis.pipelined();
                p.hset(key, field, String.valueOf(val));
                p.hset(key, UPDATE_AT, now());
                p.expire(key, expire);
                p.sync();
            } else {
                jedis.hset(key, field, String.valueOf(val));
            }
        }
        return val;
    }

    private Long doIncr(String key, String field, long defaultVal, long increment, int expire) {
        Response<Long> retVal;
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline p = jedis.pipelined();
            if (!jedis.hexists(key, field)) {
                p.hset(key, field, String.valueOf(defaultVal));
            }
            retVal = p.hincrBy(key, field, increment);
            if (expire > 0) {
                p.hset(key, UPDATE_AT, now());
                p.expire(key, expire);
            }
            p.sync();
        }
        return retVal.get();
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
                String redisField = getRedisField(field);
                if (cache.get(redisField) != null) {
                    field.set(to, convert(field.getType(), Long.parseLong(cache.get(redisField))));
                } else {
                    field.set(to, field.get(from));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("clone failed " + e.getMessage());
        }
    }

    private static String getRedisKey(Class clazz, Object obj) {
        Counter annotation = ((Counter) clazz.getAnnotation(Counter.class));
        String name;
        if (annotation == null || annotation.name() == null || annotation.name().length() == 0) {
            name = clazz.getName();
        } else {
            name = annotation.name();
        }
        String id = getId(clazz, obj);
        if (id != null) {
            return name + "_Counter_" + id;
        } else {
            return name + "_Counter";
        }
    }

    private static String getRedisField(Field field) {
        com.scienjus.mrc.annotation.Field annotation = field.getAnnotation(com.scienjus.mrc.annotation.Field.class);
        if (annotation == null || annotation.name() == null || annotation.name().length() == 0) {
            return field.getName();
        } else {
            return annotation.name();
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

    private static String now() {
        return String.valueOf(System.currentTimeMillis());
    }

    private enum MethodType {
        GET,
        SET,
        INCR,
        DECR;

        public int len() {
            return this.name().length();
        }

        public static MethodType valueOfMethodName(String methodName) {
            for (MethodType methodType : MethodType.values()) {
                if (methodName.startsWith(methodType.name().toLowerCase())) {
                    return methodType;
                }
            }
            return null;
        }
    }
}
