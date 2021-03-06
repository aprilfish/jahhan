/*
 * Copyright 1999-2012 Alibaba Group.
 *    
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *        http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.jahhan.extension.merger;

import javax.inject.Singleton;

import net.jahhan.common.extension.annotation.Extension;
import net.jahhan.spi.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Extension("float")
@Singleton
public class FloatArrayMerger implements Merger<float[]> {

    public float[] merge(float[]... items) {
        int total = 0;
        for (float[] array : items) {
            total += array.length;
        }
        float[] result = new float[total];
        int index = 0;
        for (float[] array : items) {
            for (float item : array) {
                result[index++] = item;
            }
        }
        return result;
    }
}
