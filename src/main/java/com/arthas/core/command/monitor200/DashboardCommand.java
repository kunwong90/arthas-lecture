package com.arthas.core.command.monitor200;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arthas.core.command.model.*;
import com.arthas.core.util.NetUtils;
import com.arthas.core.util.ThreadUtil;
import com.arthas.core.util.metrics.SumRateCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.util.*;

import static com.arthas.core.command.model.MemoryEntryVO.*;

public class DashboardCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardCommand.class);

    private SumRateCounter tomcatRequestCounter = new SumRateCounter();
    private SumRateCounter tomcatErrorCounter = new SumRateCounter();
    private SumRateCounter tomcatReceivedBytesCounter = new SumRateCounter();
    private SumRateCounter tomcatSentBytesCounter = new SumRateCounter();

    private volatile Timer timer;

    private long interval = 5000;

    public void process() {
        timer = new Timer("Timer-for-arthas-dashboard", true);
        timer.scheduleAtFixedRate(new DashboardTimerTask(), 0, getInterval());
    }

    public long getInterval() {
        return interval;
    }


    private class DashboardTimerTask extends TimerTask {
        private ThreadSampler threadSampler;

        public DashboardTimerTask() {
            this.threadSampler = new ThreadSampler();
        }

        @Override
        public void run() {
            try {

                DashboardModel dashboardModel = new DashboardModel();

                //thread sample
                List<ThreadVO> threads = ThreadUtil.getThreads();
                dashboardModel.setThreads(threadSampler.sample(threads));

                //memory
                addMemoryInfo(dashboardModel);

                //gc
                addGcInfo(dashboardModel);

                //runtime
                addRuntimeInfo(dashboardModel);

                //tomcat
                try {
                    addTomcatInfo(dashboardModel);
                } catch (Throwable e) {
                    LOGGER.error("try to read tomcat info error", e);
                }
                System.out.println(JSON.toJSONString(dashboardModel));
            } catch (Throwable e) {
                String msg = "process dashboard failed: " + e.getMessage();
                LOGGER.error(msg, e);
            }
        }
    }

    private static MemoryEntryVO createMemoryEntryVO(String type, String name, MemoryUsage memoryUsage) {
        return new MemoryEntryVO(type, name, memoryUsage.getUsed(), memoryUsage.getCommitted(), memoryUsage.getMax());
    }

    private static void addGcInfo(DashboardModel dashboardModel) {
        List<GcInfoVO> gcInfos = new ArrayList<GcInfoVO>();
        dashboardModel.setGcInfos(gcInfos);

        List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcMXBean : garbageCollectorMxBeans) {
            String name = gcMXBean.getName();
            gcInfos.add(new GcInfoVO(beautifyName(name), gcMXBean.getCollectionCount(), gcMXBean.getCollectionTime()));
        }
    }

    private void addTomcatInfo(DashboardModel dashboardModel) {
        // 如果请求tomcat信息失败，则不显示tomcat信息
        if (!NetUtils.request("http://localhost:8006").isSuccess()) {
            return;
        }

        TomcatInfoVO tomcatInfoVO = new TomcatInfoVO();
        dashboardModel.setTomcatInfo(tomcatInfoVO);
        String threadPoolPath = "http://localhost:8006/connector/threadpool";
        String connectorStatPath = "http://localhost:8006/connector/stats";
        NetUtils.Response connectorStatResponse = NetUtils.request(connectorStatPath);
        if (connectorStatResponse.isSuccess()) {
            List<TomcatInfoVO.ConnectorStats> connectorStats = new ArrayList<TomcatInfoVO.ConnectorStats>();
            List<JSONObject> tomcatConnectorStats = JSON.parseArray(connectorStatResponse.getContent(), JSONObject.class);
            for (JSONObject stat : tomcatConnectorStats) {
                String connectorName = stat.getString("name").replace("\"", "");
                long bytesReceived = stat.getLongValue("bytesReceived");
                long bytesSent = stat.getLongValue("bytesSent");
                long processingTime = stat.getLongValue("processingTime");
                long requestCount = stat.getLongValue("requestCount");
                long errorCount = stat.getLongValue("errorCount");

                tomcatRequestCounter.update(requestCount);
                tomcatErrorCounter.update(errorCount);
                tomcatReceivedBytesCounter.update(bytesReceived);
                tomcatSentBytesCounter.update(bytesSent);

                double qps = tomcatRequestCounter.rate();
                double rt = processingTime / (double) requestCount;
                double errorRate = tomcatErrorCounter.rate();
                long receivedBytesRate = new Double(tomcatReceivedBytesCounter.rate()).longValue();
                long sentBytesRate = new Double(tomcatSentBytesCounter.rate()).longValue();

                TomcatInfoVO.ConnectorStats connectorStat = new TomcatInfoVO.ConnectorStats();
                connectorStat.setName(connectorName);
                connectorStat.setQps(qps);
                connectorStat.setRt(rt);
                connectorStat.setError(errorRate);
                connectorStat.setReceived(receivedBytesRate);
                connectorStat.setSent(sentBytesRate);
                connectorStats.add(connectorStat);
            }
            tomcatInfoVO.setConnectorStats(connectorStats);
        }

        NetUtils.Response threadPoolResponse = NetUtils.request(threadPoolPath);
        if (threadPoolResponse.isSuccess()) {
            List<TomcatInfoVO.ThreadPool> threadPools = new ArrayList<TomcatInfoVO.ThreadPool>();
            List<JSONObject> threadPoolInfos = JSON.parseArray(threadPoolResponse.getContent(), JSONObject.class);
            for (JSONObject info : threadPoolInfos) {
                String name = info.getString("name").replace("\"", "");
                long busy = info.getLongValue("threadBusy");
                long total = info.getLongValue("threadCount");
                threadPools.add(new TomcatInfoVO.ThreadPool(name, busy, total));
            }
            tomcatInfoVO.setThreadPools(threadPools);
        }
    }

    private static String beautifyName(String name) {
        return name.replace(' ', '_').toLowerCase();
    }

    private static void addMemoryInfo(DashboardModel dashboardModel) {
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        Map<String, List<MemoryEntryVO>> memoryInfoMap = new LinkedHashMap<String, List<MemoryEntryVO>>();
        dashboardModel.setMemoryInfo(memoryInfoMap);

        //heap
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        List<MemoryEntryVO> heapMemEntries = new ArrayList<MemoryEntryVO>();
        heapMemEntries.add(createMemoryEntryVO(TYPE_HEAP, TYPE_HEAP, heapMemoryUsage));
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = poolMXBean.getUsage();
                String poolName = beautifyName(poolMXBean.getName());
                heapMemEntries.add(createMemoryEntryVO(TYPE_HEAP, poolName, usage));
            }
        }
        memoryInfoMap.put(TYPE_HEAP, heapMemEntries);

        //non-heap
        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        List<MemoryEntryVO> nonheapMemEntries = new ArrayList<MemoryEntryVO>();
        nonheapMemEntries.add(createMemoryEntryVO(TYPE_NON_HEAP, TYPE_NON_HEAP, nonHeapMemoryUsage));
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.NON_HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = poolMXBean.getUsage();
                String poolName = beautifyName(poolMXBean.getName());
                nonheapMemEntries.add(createMemoryEntryVO(TYPE_NON_HEAP, poolName, usage));
            }
        }
        memoryInfoMap.put(TYPE_NON_HEAP, nonheapMemEntries);

        addBufferPoolMemoryInfo(memoryInfoMap);
    }

    private static void addBufferPoolMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfoMap) {
        try {
            List<MemoryEntryVO> bufferPoolMemEntries = new ArrayList<MemoryEntryVO>();
            @SuppressWarnings("rawtypes")
            Class bufferPoolMXBeanClass = Class.forName("java.lang.management.BufferPoolMXBean");
            @SuppressWarnings("unchecked")
            List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(bufferPoolMXBeanClass);
            for (BufferPoolMXBean mbean : bufferPoolMXBeans) {
                long used = mbean.getMemoryUsed();
                long total = mbean.getTotalCapacity();
                bufferPoolMemEntries.add(new MemoryEntryVO(TYPE_BUFFER_POOL, mbean.getName(), used, total, Long.MIN_VALUE));
            }
            memoryInfoMap.put(TYPE_BUFFER_POOL, bufferPoolMemEntries);
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    private static void addRuntimeInfo(DashboardModel dashboardModel) {
        RuntimeInfoVO runtimeInfo = new RuntimeInfoVO();
        runtimeInfo.setOsName(System.getProperty("os.name"));
        runtimeInfo.setOsVersion(System.getProperty("os.version"));
        runtimeInfo.setJavaVersion(System.getProperty("java.version"));
        runtimeInfo.setJavaHome(System.getProperty("java.home"));
        runtimeInfo.setSystemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        runtimeInfo.setProcessors(Runtime.getRuntime().availableProcessors());
        runtimeInfo.setUptime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        runtimeInfo.setTimestamp(System.currentTimeMillis());
        dashboardModel.setRuntimeInfo(runtimeInfo);
    }
}
