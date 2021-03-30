package com.arthas.core.command.basic1000;


import com.arthas.core.command.model.ChangeResultVO;
import com.arthas.core.command.model.VMOptionModel;
import com.arthas.core.util.StringUtils;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * vmoption command
 */
public class VMOptionCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(VMOptionCommand.class);


    public static VMOptionModel run(String name, String value) {
        try {
            HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                    .getPlatformMXBean(HotSpotDiagnosticMXBean.class);

            if (StringUtils.isBlank(name) && StringUtils.isBlank(value)) {
                // show all options
                return new VMOptionModel(hotSpotDiagnosticMXBean.getDiagnosticOptions());
            } else if (StringUtils.isBlank(value)) {
                // view the specified option
                VMOption option = hotSpotDiagnosticMXBean.getVMOption(name);
                if (option == null) {
                    return new VMOptionModel();
                } else {
                    return new VMOptionModel(Arrays.asList(option));
                }
            } else {
                VMOption vmOption = hotSpotDiagnosticMXBean.getVMOption(name);
                String originValue = vmOption.getValue();

                // change vm option
                hotSpotDiagnosticMXBean.setVMOption(name, value);
                return new VMOptionModel(new ChangeResultVO(name, originValue,
                        hotSpotDiagnosticMXBean.getVMOption(name).getValue()));
            }
        } catch (Throwable t) {
            LOGGER.error("Error during setting vm option", t);
        }
        return new VMOptionModel();
    }


}
