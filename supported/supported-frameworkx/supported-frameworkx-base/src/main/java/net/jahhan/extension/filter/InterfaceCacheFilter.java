package net.jahhan.extension.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.frameworkx.annotation.Activate;
import lombok.extern.slf4j.Slf4j;
import net.jahhan.cache.CustomCacheKeyCreater;
import net.jahhan.cache.Redis;
import net.jahhan.cache.RedisFactory;
import net.jahhan.cache.annotation.Cache;
import net.jahhan.cache.util.SerializerUtil;
import net.jahhan.common.extension.annotation.Extension;
import net.jahhan.common.extension.constant.JahhanErrorCode;
import net.jahhan.common.extension.utils.Assert;
import net.jahhan.common.extension.utils.BeanTools;
import net.jahhan.context.BaseVariable;
import net.jahhan.exception.JahhanException;
import net.jahhan.lock.DistributedLock;
import net.jahhan.lock.util.ServiceReentrantLockUtil;
import net.jahhan.service.context.AuthenticationVariable;
import net.jahhan.service.service.bean.User;
import net.jahhan.spi.Filter;

import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Activate(group = Constants.PROVIDER, order = 1000)
@Extension("interfaceCacheFilter")
@Singleton
@Slf4j
public class InterfaceCacheFilter implements Filter {
    private Redis redis = RedisFactory.getRedis(RedisFactory.DEFAULT_DATABASE, null);
    private static String PRE = "fast_back:";
    private static String LOCK_PRE = "fast_back_lock:";

    private String createrCacheKey(Cache cache, String interfaceClassName, Method implMethod, Invocation invocation) {
        StringBuilder sb = new StringBuilder(PRE);
        sb.append(interfaceClassName).append("_").append(implMethod);
        if (cache.isCustomCacheKey()) {
            Class createrClass = cache.customCacheKeyCreaterClass();
            CustomCacheKeyCreater keyCreater = null;
            if (createrClass != null && !void.class.equals(createrClass)) {
                if (CustomCacheKeyCreater.class.isAssignableFrom(createrClass)) {
                    try {
                        keyCreater = (CustomCacheKeyCreater) createrClass.newInstance();
                    } catch (Exception e) {
                        JahhanException.throwException(JahhanErrorCode.INSTANTIATION_ERROR,
                                "customCacheKeyCreater:" + createrClass.getName() + " can't new instance error!", e);
                    }
                } else {// 配置有问题抛异常，测试阶段就能发现
                    JahhanException.throwException(JahhanErrorCode.CONFIGURATION_ERROR,
                            "service:" + interfaceClassName + " method:" + implMethod + " customCacheKeyCreaterClass:"
                                    + createrClass.getName()
                                    + " isn't a sub class of CustomCacheKeyCreater.class error!!");
                }
            }

            if (keyCreater != null) {
                String key = keyCreater.createCacheKey(invocation.getAttachments(), invocation.getArguments());
                sb.append("_createCacheKey:").append(key);

                return sb.toString();
            }

            int[] indexArr = cache.argumentIndexNumbers();
            String[] indexArgumentField = cache.indexArgumentField();
            if (indexArgumentField.length > 0 && indexArgumentField.length != indexArr.length) {//个数不对应，抛出异常
                JahhanException.throwException(JahhanErrorCode.CONFIGURATION_ERROR, "interfaceClassName:" + interfaceClassName + " method:" + implMethod.getName() + " configuration error,The parameter(argumentIndexNumbers,indexArgumentField) length of the cache annotation is not equal.");
            }
            Object[] args = invocation.getArguments();
            if (indexArr != null && args != null && args.length > 0) {
                int maxIndex = args.length - 1;
                sb.append("_customCacheKey(");
                int len = sb.length();
                List<Integer> errorIndexList = new ArrayList<>();
                for (int idx : indexArr) {
                    if (idx >= 0 && idx <= maxIndex) {
                        if (indexArgumentField.length > 0) {
                            String fieldNames = indexArgumentField[idx];
                            if (fieldNames.trim().length() > 0) {
                                sb.append("idx[").append(idx).append("]");

                                Object obj = args[idx];
                                String[] names = fieldNames.split(",");
                                try {
                                    Field field = null;
                                    for (String name : names) {
                                        field = obj.getClass().getDeclaredField(name);
                                        field.setAccessible(true);
                                        Object value = field.get(obj);
                                        sb.append("=").append(name).append(":").append(value);
                                    }
                                } catch (Exception e) {
                                    JahhanException.throwException(JahhanErrorCode.CONFIGURATION_ERROR, "interfaceClassName:" + interfaceClassName + " method:" + implMethod.getName() + " configuration error,indexArgumentField[" + idx + "]:" + fieldNames + " not found!");
                                }
                                sb.append("_");
                            } else {
                                sb.append("idx[").append(idx).append("]:").append(args[idx]).append("_");
                            }
                        } else {
                            sb.append("idx[").append(idx).append("]:").append(args[idx]).append("_");
                        }
                    } else {
                        errorIndexList.add(idx);
                    }
                }
                if (errorIndexList.size() > 0) {// 配置有问题抛异常，测试阶段就能发现
                    JahhanException.throwException(JahhanErrorCode.CONFIGURATION_ERROR,
                            "service:" + interfaceClassName + " method:" + implMethod + " argumentIndexNumbers:"
                                    + errorIndexList + " is invalid index!!");
                }
                if (sb.length() > len) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                sb.append(")");

                return sb.toString();
            }
        }

        BaseVariable baseVariable = BaseVariable.getBaseVariable();
        sb.append("_").append(baseVariable.getSign());

        return sb.toString();
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws JahhanException {
        String interfaceClassName = invoker.getUrl().getParameter("interface");
        String implClassName = invoker.getUrl().getParameter("class");
        String methodName = invocation.getMethodName();

        Method implMethod = null;
        try {
            implMethod = Class.forName(implClassName).getDeclaredMethod(methodName,
                    invocation.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new JahhanException(JahhanErrorCode.UNKNOW_ERROR, "未知错误", e);
        }
        Cache cache = implMethod.getAnnotation(Cache.class);
        Result invoke = null;
        if (null == cache || cache.blockTime() < 1) {
            return invoker.invoke(invocation);
        } else {
            String key = "";
            switch (cache.fastBackType()) {

                case USERID:
                    User user = AuthenticationVariable.getAuthenticationVariable().getUser();
                    Assert.notNull(user, "无用户信息", JahhanErrorCode.NO_AUTHORITY);
                    key = createrCacheKey(cache, interfaceClassName, implMethod, invocation) + "_"
                            + user.getUserId();
                    break;
                case ALL:
                    key = createrCacheKey(cache, interfaceClassName, implMethod, invocation);
                    break;
                default:
                    break;

            }
            log.trace("##cache key:{}", key);
            byte[] bytes = redis.getBinary(key.getBytes());
            if (bytes != null) {
                if (cache.fastBackFail()) {
                    throw new JahhanException(JahhanErrorCode.FAST_RESPONSE_ERROR, cache.fastBackFailMessage());
                }
                Result deserialize = SerializerUtil.deserialize(bytes, Result.class);
                log.debug("快速返回：" + interfaceClassName + "." + implMethod);
                return deserialize;
            }
            String ret = "";
            TimeUnit blockTimeUnit = cache.blockTimeUnit();
            try (DistributedLock lock = ServiceReentrantLockUtil.lock(LOCK_PRE + interfaceClassName + "." + implMethod,
                    cache.blockTime(), blockTimeUnit)) {
                bytes = redis.getBinary(key.getBytes());
                if (bytes != null) {
                    if (cache.fastBackFail()) { // fastBackFail=true 快速失败 ,
                        // fastBackFail=false 快速返回
                        throw new JahhanException(JahhanErrorCode.FAST_RESPONSE_ERROR, cache.fastBackFailMessage());
                    }
                    Result deserialize = SerializerUtil.deserialize(bytes, Result.class);
                    log.debug("快速返回：" + interfaceClassName + "." + implMethod);
                    return deserialize;
                }
                invoke = invoker.invoke(invocation);
                ret = redis.setNxTTL(key.getBytes(), SerializerUtil.serializeFrom(invoke), cache.blockTime(),
                        blockTimeUnit);
            } catch (Exception e) {
                log.error("错误", e);
                if (!(e instanceof JahhanException
                        && JahhanErrorCode.LOCK_OVERTIME == ((JahhanException) e).getCode())) {
                    throw new JahhanException(e);
                }
            }
            if (cache.fastBackFail() && (null == ret || !ret.equals("OK"))) {
                // 缓存设置不成功，但是数据库查询都能查询到数据，不要抛异常导致业务功能异常,只记录异常情况
                log.error("保存至redis错误，key:{}  value:{}", key, invoke);
                // throw new JahhanException(JahhanErrorCode.LOCK_ERROE,
                // "快速返回失败错误");
            }
        }
        return invoke;
    }
}