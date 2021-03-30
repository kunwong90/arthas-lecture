package com.arthas.core.command.basic1000;

import com.alibaba.fastjson.JSON;
import com.arthas.core.command.model.VMOptionModel;
import org.junit.Test;

public class VMOptionCommandTest {

    @Test
    public void runTest() {
        VMOptionModel vmOptionModel = VMOptionCommand.run(null, null);
        System.out.println(JSON.toJSONString(vmOptionModel));
    }
}