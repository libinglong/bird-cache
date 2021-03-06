#### 缓存中间件

##### 相比spring-cache增加了一些特性

* 高性能批量缓存查询

    md-cache支持缓存，但使用要非常小心，需要保证参数和返回值顺序是对应的。

* 非常快速地让某一缓存空间的所有缓存失效

  * spring cache的RedisCache实现，往往需要通过keys命令才可以完成清空缓存的操作。

  * md-cache则没有真正的清空缓存，而是增加了缓存空间的version概念。缓存的存取都会到最新的version中操作，因此redis要设置合适的驱逐策略。volatile-lru或volatile-ttl都是不错的选择。

* 以类名作为缓存空间隔离

    spring cache可以配置，但md-cache则每个类对应一个缓存空间，目前不支持配置。

* 缓存异地双活，两地缓存有更好的一致性，但仍保证高可用

    进行了缓存同步，因此正常情况下，无需关心一致性问题。但是如果另一机房宕机，则忽略io错误，保证当前机房的可用性。同时记录该事件，交由错误处理器处理，默认的错误处理器使用日志记录相关错误。并将key使用base64编码，以便后续恢复缓存不一致的问题。

* 缓存和实际数据的最终一致性

    在更新数据前，会对key执行redis的expire操作。可以配置expire的时间和方法的预期执行时间。保证过期时间远远大于方法执行时间，从而保证最终一致性。

  

##### 使用方式

* 如何启动

  * 添加依赖

    ```java
    <dependency>
      <groupId>com.sohu.smc</groupId>
      <artifactId>md-cache</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
    ```

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

  * 序列化可自定义实现，默认是pb序列化

    实现com.sohu.smc.md.cache.serializer.Serializer，并set到cacheManager中

  * 配置类添加注解@EnableMdCaching

    ```java
    @EnableMdCaching
    @Configuration
    public class Config {
    }
    ```

  * 自定义配置

    ```java
    @Bean
    public CacheConfig cacheConfig(){
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setExecTime(200L);
        return cacheConfig;
    }
    ```

##### 如何使用缓存

* 使用注解实现方法级别的缓存

  ```java
  @MdCacheable(key = "#p0")
  public int get(int a){
      //do some complete operations or io operations
      return ret;
  }
  
  @MdBatchCache(key = "#obj + #p0")
  public List<Integer> gets(List<Integer> as,int a){
      //do some complete operations or io operations
      return ret;
  }
  
  @MdCachePut(key = "#p0")
  @MdCachePut(key = "#p0 + #p1")
  public int put(int a, int b){
      //do some complete operations or io operations
      return ret;
  }
  
  @MdCacheEvict(key = "#p0")
  @MdCacheEvict(key = "#p0 + #p1")
  public int evict(int a){
      //do some complete operations or io operations
      return ret;
  }
  
  @MdCacheClear
  public void clear(){
  
  }
  ```

  

* 使用cacheManager做更高级的操作

  ```java
  @Autowired
  private RedisCacheManager cacheManager;
  
  @RequestMapping("cache/evcit")
  public String evict(@RequestParam("key") String base64Key) throws InterruptedException {
    Cache cache = cacheManager.getCache("cacheName");
    byte[] decode = Base64.getDecoder()
      .decode(base64Key);
    Object key = cacheManager.getSerializer()
      .deserialize(decode);
    cache.delete(key);
    return "ok";
  }
  ```