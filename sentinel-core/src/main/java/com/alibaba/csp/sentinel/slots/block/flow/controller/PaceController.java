/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.slots.block.flow.controller;

import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.csp.sentinel.slots.block.flow.Controller;

import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.csp.sentinel.node.Node;

/**
 * @author jialiang.linjl
 */
public class PaceController implements Controller {

    private final int maxQueueingTimeMs;
    private final double count;
    private final AtomicLong latestPassedTime = new AtomicLong(-1);

    public PaceController(int timeOut, double count) {
        this.maxQueueingTimeMs = timeOut;
        this.count = count;
    }

    @Override
    public boolean canPass(Node node, int acquireCount) {

        // 按照斜率来计算计划中应该什么时候通过
        long currentTime = TimeUtil.currentTimeMillis();

        long costTime = Math.round(1.0 * (acquireCount) / count * 1000);

        //期待时间
        long expectedTime = costTime + latestPassedTime.get();

        if (expectedTime <= currentTime) {
            //这里会有冲突,然而冲突就冲突吧.
            latestPassedTime.set(currentTime);
            return true;
        } else {
            // 计算自己需要的等待时间
            long waitTime = costTime + latestPassedTime.get() - TimeUtil.currentTimeMillis();
            // 如果是单线程，latestPassedTime 肯定是当前时间或者之前的时间。所以 waitTime < constTime ，
            // maxQueueingTimeMs要小于constTime才会走到if分支中，但是意义在哪里？
            // 这就要考虑多线程的情况了
            if (waitTime >= maxQueueingTimeMs) {
                return false;
            } else {
                long oldTime = latestPassedTime.addAndGet(costTime); // 想象下，多线程的情况下，这里并发执行。latestPassedTime是atomicLong，会变得很大。
                try {
                    waitTime = oldTime - TimeUtil.currentTimeMillis();
                    if (waitTime >= maxQueueingTimeMs) {     // 多线程的情况下，很可能触发这种。
                        latestPassedTime.addAndGet(-costTime);
                        return false;
                    }
                    Thread.sleep(waitTime);
                    return true;
                } catch (InterruptedException e) {
                }
            }
        }

        return false;
    }

}
