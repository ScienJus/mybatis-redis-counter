package com.scienjus;

import com.scienjus.config.DataSourceConfig;
import com.scienjus.domain.User;
import com.scienjus.mapper.UserMapper;
import org.junit.Assert;
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

    private static final int USER_ID = 1;

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
}
