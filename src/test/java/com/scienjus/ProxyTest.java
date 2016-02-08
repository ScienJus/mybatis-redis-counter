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

    private static final int DEFAULT_VIEW_COUNT = 5;

    private static final double DEFAULT_SCORE = 10.1;

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
        post.setViewCount(DEFAULT_VIEW_COUNT);
        post.setScore(DEFAULT_SCORE);
        postMapper.insert(post);
    }

    @Test
    public void setterTest() {
        User user = getUser(USER_ID);
        user.setFollowerCount(10);

        user = getUser(USER_ID);

        Assert.assertEquals(user.getFollowerCount(), 10);
    }

    @Test
    public void incrTest() {
        User user = getUser(USER_ID);
        user.setFollowerCount(10);
        int retVal = user.incrFollowerCount();

        user = getUser(USER_ID);

        Assert.assertEquals(retVal, 11);
        Assert.assertEquals(user.getFollowerCount(), 11);
    }

    @Test
    public void decrTest() {
        User user = getUser(USER_ID);
        user.setFollowerCount(10);
        int retVal = user.decrFollowerCount();

        user = getUser(USER_ID);

        Assert.assertEquals(retVal, 9);
        Assert.assertEquals(user.getFollowerCount(), 9);
    }

    @Test
    public void listTest() {
        User user = getUser(USER_ID);
        user.setFollowerCount(10);
        int retVal = user.decrFollowerCount();

        user = userMapper.all().get(0);

        Assert.assertEquals(retVal, 9);
        Assert.assertEquals(user.getFollowerCount(), 9);
    }

    @Test
    public void aliasTest() {
        Post post = getPost(POST_ID);
        post.setViewCount(100);
        int retVal = post.incrViewCount();

        post = getPost(POST_ID);

        Assert.assertEquals(retVal, 101);
        Assert.assertEquals(post.getViewCount(), 101);
    }

    @Test
    public void expireTest() throws InterruptedException {
        Post post = getPost(POST_ID);
        post.setViewCount(100);

        post = getPost(POST_ID);
        Assert.assertEquals(post.getViewCount(), 100);

        Thread.sleep(5000);
        session.clearCache();

        post = getPost(POST_ID);
        Assert.assertEquals(post.getViewCount(), DEFAULT_VIEW_COUNT);

    }

    @Test
    public void realtimeTest() {
        Post post1 = getPost(POST_ID);
        Post post2 = getPost(POST_ID);

        post1.setViewCount(100);
        Assert.assertEquals(post2.getViewCount(), 100);

    }

    @Test
    public void incrArgTest() {
        Post post = getPost(POST_ID);
        post.setViewCount(100);
        int retVal = post.incrViewCount(5);

        post = getPost(POST_ID);

        Assert.assertEquals(retVal, 105);
        Assert.assertEquals(post.getViewCount(), 105);
    }

    @Test
    public void decrArgTest() {
        Post post = getPost(POST_ID);
        post.setViewCount(100);
        int retVal = post.decrViewCount(5);

        post = getPost(POST_ID);

        Assert.assertEquals(retVal, 95);
        Assert.assertEquals(post.getViewCount(), 95);
    }


    @Test
    public void doubleIncrTest() {
        Post post = getPost(POST_ID);
        double retVal = post.incrScore(0.05);

        Assert.assertEquals(retVal, DEFAULT_SCORE + 0.05, 0.0001);
        Assert.assertEquals(retVal, DEFAULT_SCORE + 0.05, 0.0001);
    }
    
    public User getUser(int id) {
        session.clearCache();
        return userMapper.get(id);
    }

    public Post getPost(String id) {
        session.clearCache();
        return postMapper.get(id);
    }

    @AfterClass
    public static void after() {
        session.close();
    }
}
