package com.scienjus.domain;

import com.scienjus.mrc.annotation.Counter;
import com.scienjus.mrc.annotation.Field;
import com.scienjus.mrc.annotation.Id;
import lombok.Data;

/**
 * @author ScienJus
 * @date 16/2/7.
 */
@Data
@Counter
public class User {

    @Id
    private Integer id;

    private String name;

    @Field
    private int followerCount;

    public int incrFollowerCount() {
        return 0;
    }

    public int decrFollowerCount() {
        return 0;
    }

}
