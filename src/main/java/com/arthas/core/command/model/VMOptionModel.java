package com.arthas.core.command.model;

import com.sun.management.VMOption;

import java.util.List;


public class VMOptionModel extends ResultModel {

    private List<VMOption> vmOptions;

    private ChangeResultVO changeResult;

    public VMOptionModel() {
    }

    public VMOptionModel(List<VMOption> vmOptions) {
        this.vmOptions = vmOptions;
    }

    public VMOptionModel(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }

    @Override
    public String getType() {
        return "vmoption";
    }

    public List<VMOption> getVmOptions() {
        return vmOptions;
    }

    public void setVmOptions(List<VMOption> vmOptions) {
        this.vmOptions = vmOptions;
    }

    public ChangeResultVO getChangeResult() {
        return changeResult;
    }

    public void setChangeResult(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }
}
