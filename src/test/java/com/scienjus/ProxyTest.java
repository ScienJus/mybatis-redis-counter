package com.scienjus;

import com.alibaba.druid.pool.DruidDataSource;
import com.scienjus.domain.Post;
import com.scienjus.domain.User;
import com.scienjus.mapper.PostMapper;
import com.scienjus.mapper.UserMapper;
import com.scienjus.mrc.interceptor.QueryCounterInterceptor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.*;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
public class ProxyTest {

    private static SqlSession session;

    private static UserMapper userMapper;

    private static PostMapper postMapper;

    private static final int USER_ID = 1;

    private static final String POST_ID = "A";

    @BeforeClass
    public static void before() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUsername("root");
        dataSource.setPassword("pwd");
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/mrc");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();

        JedisPool jedisPool =  new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379, 0, "pwd");

        QueryCounterInterceptor queryCounterInterceptor = new QueryCounterInterceptor();
        queryCounterInterceptor.setJedisPool(jedisPool);

        Environment environment = new Environment("test", transactionFactory, dataSource);

        Configuration configuration = new Configuration(environment);
        configuration.setLazyLoadingEnabled(true);
        configuration.addInterceptor(queryCounterInterceptor);
        configuration.setMapUnderscoreToCamelCase(true);

        configuration.addMapper(UserMapper.class);
        configuration.addMapper(PostMapper.class);

        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory factory = builder.build(configuration);

        session = factory.openSession();
        userMapper = session.getMapper(UserMapper.class);
        postMapper = session.getMapper(PostMapper.class);
    }

    @Before
    public void beforeTest() {
        userMapper.delAll();

        User user = new User();
        user.setId(USER_ID);
        user.setName("ScienJus");
        user.setFollowerCount(0);
        userMapper.insert(user);

        postMapper.delAll();
        Post post = new Post();
        post.setId(POST_ID);
        post.setTitle("Counter");
        post.setViewCount(0);
        postMapper.insert(post);
    }

    @Test
    public void setterTest() {
        User user = userMapper.get(USER_ID);
        user.setFollowerCount(10);

        user = userMapper.get(USER_ID);

        Assert.assertEquals(user.getFollowerCount(), 10);
    }

    @Test
    public void incrTest() {
        User user = userMapper.get(USER_ID);
        user.setFollowerCount(10);
        int retVal = user.incrFollowerCount();

        user = userMapper.get(USER_ID);

        Assert.assertEquals(retVal, 11);
        Assert.assertEquals(user.getFollowerCount(), 11);
    }

    @Test
    public void decrTest() {
        User user = userMapper.get(USER_ID);
        user.setFollowerCount(10);
        int retVal = user.decrFollowerCount();

        user = userMapper.get(USER_ID);

        Assert.assertEquals(retVal, 9);
        Assert.assertEquals(user.getFollowerCount(), 9);
    }

    @Test
    public void listTest() {
        User user = userMapper.get(USER_ID);
        user.setFollowerCount(10);
        int retVal = user.decrFollowerCount();

        user = userMapper.all().get(0);

        Assert.assertEquals(retVal, 9);
        Assert.assertEquals(user.getFollowerCount(), 9);
    }

    @Test
    public void aliasTest() {
        Post post = postMapper.get(POST_ID);
        post.setViewCount(100);
        int retVal = post.incrViewCount();

        post = postMapper.get(POST_ID);

        Assert.assertEquals(retVal, 101);
        Assert.assertEquals(post.getViewCount(), 101);
    }

    @AfterClass
    public static void after() {
        session.close();
    }
}
