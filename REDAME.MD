#### 缓存中间件

##### 特性

* 高性能批量缓存查询
* 非常快速地让某一缓存空间的所有缓存失效
* 以类名作为缓存空间隔离
* 使用spel计算key值
* 缓存异地双活，两地缓存有更好的一致性
* 配合db有更好的数据一致性

##### 使用方式

* 单个redis服务使用SingleRedisCacheManager

  ```java
  @Bean
  public SingleRedisCacheManager simpleRedisCacheManager(){
    RedisURI redisURI = RedisURI.builder()
      .withHost("localhost")
      .withPort(6379)
      .build();
  	return new SingleRedisCacheManager(redisURI);
  }
  ```

* 异地两个redis服务使用MdRedisCacheManager

  ```java
  @Bean
  public MdRedisCacheManager cacheManager(){
    RedisURI redisURI = RedisURI.builder()
      .withHost("localhost")
      .withPort(6379)
      .build();
    RedisURI redisURI2 = RedisURI.builder()
      .withHost("localhost")
      .withPort(6380)
      .build();
    return new MdRedisCacheManager(redisURI, redisURI2);
  }
  ```

* 序列化可选

  实现com.sohu.smc.md.cache.serializer.Serializer

* 配置类添加注解@EnableMdCaching

  ```java
  @EnableMdCaching
  @Configuration
  public class Config {
  }
  ```

##### 通用配置

* 参见com.sohu.smc.md.cache.spring.CacheConfig

