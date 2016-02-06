package com.scienjus.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.scienjus.mapper.UserMapper;
import com.scienjus.mrc.interceptor.QueryCounterInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
@Configuration
public class DataSourceConfig {

    @Bean(initMethod="init",destroyMethod="close")
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUsername("root");
        dataSource.setPassword("pwd");
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/mrc");
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        final SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactory.setConfigLocation(resolver.getResource("classpath:mybatis.xml"));
        sqlSessionFactory.setPlugins(new Interceptor[]{queryCounterInterceptor()});
        return sqlSessionFactory.getObject();
    }

    @Bean
    public QueryCounterInterceptor queryCounterInterceptor() {
        QueryCounterInterceptor queryCounterInterceptor = new QueryCounterInterceptor();
        queryCounterInterceptor.setJedisPool(jedisPool());
        return queryCounterInterceptor;
    }

    @Bean
    public JedisPool jedisPool() {
        return new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379, 0, "pwd");
    }

    @Bean
    public MapperFactoryBean<UserMapper> userMapper() throws Exception {
        MapperFactoryBean<UserMapper> factory = new MapperFactoryBean<>();
        factory.setMapperInterface(UserMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory());
        return factory;
    }
}
