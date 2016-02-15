# MyBatis Redis Counter

use redis to counting in mybatis, non invasive and easy to use

[中文文档][1]

### Quick Start

Add the Maven dependency:

repository:

```
<repository>
    <id>scienjus-mvn-repo</id>
    <url>https://raw.github.com/ScienJus/maven/snapshot/</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
    </snapshots>
</repository>
```

dependency:

```
<dependency>
    <groupId>com.scienjus</groupId>
    <artifactId>mybatis-redis-counter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Add `QueryCounterInterceptor` to mybatis plugins:

```
JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);

QueryCounterInterceptor queryCounterInterceptor = new QueryCounterInterceptor();
        queryCounterInterceptor.setJedisPool(jedisPool);     
//... env config   
Configuration configuration = new Configuration(environment);
configuration.addInterceptor(queryCounterInterceptor);
//...  other config
SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
SqlSessionFactory factory = builder.build(configuration);
```

Or use `spring-mybatis`:

```
@Bean
public SqlSessionFactory sqlSessionFactory() throws Exception {
    final SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
    sqlSessionFactory.setDataSource(dataSource());
    sqlSessionFactory.setPlugins(new Interceptor[]{queryCounterInterceptor()});
    //... other config
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
    return new JedisPool("127.0.0.1", 6379);
}
```

Then just need to add some annotations and methods in your model:

```
@Counter
public class User {

    //primary key
    @Id	
    private Integer id;

    private String name;

    // means this is a counter field
    @Field	
    private int followerCount;
    
    public int setFollowerCount(int followerCount) {
    	this.followerCount = followerCount;
    	return this.followerCount;
    }
    
    public int getFollowerCount() {
    	return this.followerCount
    }

	//increment
    public int incrFollowerCount() {
        return 0;
    }

	//decrement
    public int decrFollowerCount() {
        return 0;
    }

}
```

It should be working like this

```
User user = userMapper.get(USER_ID);
user.setFollowerCount(10);
int retVal = user.incrFollowerCount();

user = userMapper.get(USER_ID);		//another user instance with same id

Assert.assertEquals(retVal, 11);
Assert.assertEquals(user.getFollowerCount(), 11);
```

[1]: ./zh.md
