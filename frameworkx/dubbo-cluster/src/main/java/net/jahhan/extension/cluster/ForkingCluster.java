/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jahhan.extension.cluster;

import javax.inject.Singleton;

import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.support.ForkingClusterInvoker;

import net.jahhan.common.extension.annotation.Extension;
import net.jahhan.exception.JahhanException;
import net.jahhan.spi.Cluster;

/**
 * 并行调用，只要一个成功即返回，通常用于实时性要求较高的操作，但需要浪费更多服务资源。
 * 
 * <a href="http://en.wikipedia.org/wiki/Fork_(topology)">Fork</a>
 * 
 * @author william.liangf
 */
@Extension("forking")
@Singleton
public class ForkingCluster implements Cluster {
    
    public final static String NAME = "forking"; 
    
    public <T> Invoker<T> join(Directory<T> directory) throws JahhanException {
        return new ForkingClusterInvoker<T>(directory);
    }

}