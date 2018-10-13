package letian.demo;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.Collections;

public class Demo01 {

    private static final String RESOURCE_KEY = "groupName:topicName";

    public static void init() {
        FlowRule rule = new FlowRule();
        rule.setResource(RESOURCE_KEY); // 对应的 key 为 `groupName:topicName`
        rule.setCount(20);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setLimitApp("default");
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);// 匀速器模式下，若设置了 QPS 为 5，则请求每 200 ms 允许通过 1 个
        // 如果更多的请求到达，这些请求会被置于虚拟的等待队列中。等待队列有一个 max timeout，如果请求预计的等待时间超过这个时间会直接被 block
        // 在这里，timeout 为 5s
        rule.setMaxQueueingTimeMs(5 * 1000);
        FlowRuleManager.loadRules(Collections.singletonList(rule));
    }


    public static void main(String[] args) throws BlockException {
        init();
        int count = 0;
        while (true) {
            Entry e = SphU.entry(RESOURCE_KEY);;
            System.out.println(++count);
        }
    }
}
