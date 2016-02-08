package com.scienjus;

import com.scienjus.config.DataSourceConfig;
import com.scienjus.domain.Post;
import com.scienjus.domain.User;
import com.scienjus.mapper.PostMapper;
import com.scienjus.mapper.UserMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DataSourceConfig.class)
public class SpringTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    private static final int USER_ID = 1;

    private static final String POST_ID = "A";

    private static final int DEFAULT_VIEW_COUNT = 5;

    private static final double DEFAULT_SCORE = 10.1;

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
    public void expireTest() throws InterruptedException {
        Post post = postMapper.get(POST_ID);
        post.setViewCount(100);

        post = postMapper.get(POST_ID);
        Assert.assertEquals(post.getViewCount(), 100);

        Thread.sleep(5000);

        post = postMapper.get(POST_ID);
        Assert.assertEquals(post.getViewCount(), DEFAULT_VIEW_COUNT);

    }
}
