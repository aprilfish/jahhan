package net.jahhan.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jahhan.constant.SystemErrorCode;
import net.jahhan.exception.FrameworkException;
import net.jahhan.utils.PropertiesUtil;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/*
 * 使用前需要调用init方法
 */
public class RedisFactory {

	protected static Logger logger = LoggerFactory
			.getLogger(RedisFactory.class);

	private final static String MAXACTIVE = "1000"; // 一个pool的最大并发数

	private final static String MAXIDLE = "10"; // pool中最大的空闲数量

	private final static String MAXWAIT = "60000";// 获取一个jedis实例的超时时间，如果超过这个时间，就会抛出异常，单位ms

	private final static String TIMEOUT = "60000";// 用于设置socket的timeout，单位是ms

	private final static Map<String, Redis> poolMap = new HashMap<String, Redis>();

	public static Redis getRedis(String type, String key) {
		return getMainRedis(type, key);
	}

	public static Redis getMainRedis(String type, String id) {
		if (RedisConfigurationManger.isInUse()) {
			Redis redis = poolMap.get(type);
			if (redis == null) {
				logger.error("redis（type={}） 没有配置", type);
				return poolMap.get(RedisConstants.TABLE_COMMON);
			}
			return redis;
		} else {
			return new Redis(null);
		}
	}

	public static Redis getReadRedis(String type, String id) {
		return getMainRedis(type, id);
	}

	static {
		try {
			Properties is = PropertiesUtil.getProperties("redis");
			create(is, "cache.", RedisConstants.TABLE_SYSTEM);
			create(is, "common.", RedisConstants.TABLE_COMMON);
			create(is, "session.", RedisConstants.TABLE_SESSION);
			create(is, "seq.", RedisConstants.TABLE_SEQ);
			create(is, "mq.", RedisConstants.TABLE_MQ);
			logger.info("redis连接池初始化完毕!!!");
		} catch (Exception e) {
			logger.error("redis连接池初始化失败。" + e.getMessage(), e);
			FrameworkException.throwException(SystemErrorCode.SYSTEM_CONFIG_ERROR, "redis 失败");
		}
	}

	public static void init() {

	}

	/**
	 * 创建redisPool，并加入到poolMap中
	 * 
	 * @param is
	 *            配置
	 * @param pre
	 *            配置文件中的配置项前缀
	 * @param key
	 *            创建的redisPool在poolMap中对应的key
	 * @param database
	 *            当前所使用的库
	 */
	private static void create(Properties is, String pre, String key) {
		if (RedisConfigurationManger.isInUse()) {
			String host = is.getProperty(pre + "host");
			// 如果host没有配置,并且redis不是严格模式,就忽略掉该库
			if (StringUtils.isEmpty(host) && !RedisConfigurationManger.isStrict()) {
				logger.error("redis（pre={}） 没有配置", pre);
				return;
			}
			JedisPoolConfig config = new JedisPoolConfig();
			// 控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
			// 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
			config.setMaxTotal(Integer.parseInt(is.getProperty(pre
					+ "maxActive", MAXACTIVE)));
			config.setMaxIdle(Integer.parseInt(is.getProperty(pre + "maxIdle",
					MAXIDLE)));
			config.setMaxWaitMillis(Long.parseLong(is.getProperty(pre
					+ "maxWait", MAXWAIT)));
			// 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
			int timeout = Integer.parseInt(is.getProperty(pre + "timeout",
					TIMEOUT));
			String password = is.getProperty(pre + "password", null);
			int database = Integer.parseInt(is.getProperty(pre + "database",
					"0"));
			int port = Integer.parseInt(is.getProperty(pre + "port"));
			// config.setTestOnBorrow(configurationManager.getRedisTestOnBorrow());
			JedisPool pool = new JedisPool(config, host, port, timeout,
					password, database);
			Redis r = new Redis(pool);
			r.ping();
			poolMap.put(key, r);
			if (logger.isInfoEnabled()) {
				logger.info("redis(" + key + "):" + "{host:" + host + ",port:"
						+ port + ",maxActive:" + config.getMaxTotal()
						+ ",maxIdle:" + config.getMaxIdle() + ",maxWaitMS:"
						+ config.getMaxWaitMillis() + ",timeout:" + timeout
						+ ",database:" + database + "}");
			}
		}
	}

}
