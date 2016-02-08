# MyBatis Redis Counter

在MyBatis项目中使用Redis辅助计数，使用简单并且无侵入性

### 为什么使用Redis

在应用程序中经常会出现一些计数需求，例如访问量、粉丝数等。

对于这些写入非常频繁的数据，直接通过SQL update数据库是一种非常耗费性能的行为，无意义的连接占用和锁占用会造成数据库压力。

所以引入拥有单线程，原子操作等优势的Redis解决这个问题再合适不过了。所有计数操作都在Redis中完成，只需要定时将Redis中的数据同步到数据库中就可以了（也可以不同步，只存放在Redis中）。

这个库没有提供同步操作的原因是：在接口层做定时任务是非常不可靠且不优雅的。同步其实很简单，你只需要写个脚本定时从Redis中使用`scan`操作查询出当前的数据然后更新到数据库中即可（注意不要用`keys`操作）。

注意这个库所做的并不是缓存，缓存解决的是读频繁问题，而这个库解决的是写频繁问题，它并不能改善读性能。

### 快速开始

暂时还没有放在公共的Maven仓库中，所以你只能先Clone下源码然后通过Maven打成Jar包：

```
git clone https://github.com/ScienJus/mybatis-redis-counter.git
cd mybatis-redis-counter
mvn package

mvn install:install-file  
-DgroupId=com.scienjus
-DartifactId=mybatis-redis-counter
-Dversion=1.0-SNAPSHOT
-Dpackaging=jar  
-Dfile=/path/to/jar/mybatis-redis-counter.jar
```

添加Maven依赖：

```
<dependency>
    <groupId>com.scienjus</groupId>
    <artifactId>mybatis-redis-counter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

将 `QueryCounterInterceptor`注册到MyBatis的插件中（也称为拦截器）：

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

或者使用`spring-mybatis`依赖注入:

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

接下来只需要在你的原有Model类中添加一些方法和注解：

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

`@Counter`表示这个类需要计数服务，其中`@Id`为主键（也可以没有，相当于一个单例对象，但是不支持多个主键），`@Field`为需要计数的字段。

你可以为这个计数字段添加对应的`setter`、`incrXXX`、`decrXXX`方法，只添加所需的即可，方法的返回值可以是`void`或是字段的类型。

使用时只需要这样：

```
User user = userMapper.get(USER_ID);
user.setFollowerCount(10);
int retVal = user.incrFollowerCount();

user = userMapper.get(USER_ID);		//another user instance with same id

Assert.assertEquals(retVal, 11);
Assert.assertEquals(user.getFollowerCount(), 11);
```

你只需要调用一下刚才定义的方法，就会发现不光这个对象的属性改变了，其他环境中与它拥有相同主键对象的对应属性也会发生改变，并且根本不需要进行更新操作，这完全是无侵入性的！


### 待添加功能

- [ ] 给对象添加生存时间，你可以不添加，对象就会一直存储在Redis中。也可以添加并定时同步到数据库，这样Redis中就只需要保存热数据，以节约内存空间
- [ ] 给对象添加实时性选项，实时性高的字段每次调用`getter`时都会去查最新的值，而实时性低的对象只有从MyBatis中读取时会赋值。这只和读取有关，写入依旧是每次都更新，并且只要有写入操作了，对应字段的值也会变成最新的值。
- [x] 给类添加别名
- [x] 给字段添加别名
- [ ] 提供一个不经过MyBatis的方法，只在Redis中维护对象，这样就可以方便的进行一些与数据库数据无关的计数，例如注册数、每日注册数等。也就有了无`@Id`的应用场景
- [ ] 基于上面的功能，提供`Date`日期类型的主键，并指定日期格式化选项