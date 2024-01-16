package com.baidu.fsg.uid.boot;

import com.baidu.fsg.uid.core.worker.DefaultWorkerIdAssigner;
import com.baidu.fsg.uid.core.worker.WorkerIdAssigner;
import com.baidu.fsg.uid.core.worker.WorkerNode;
import com.baidu.fsg.uid.core.worker.WorkerNodeStorage;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.Objects;

public class SpringWorkerIdAssigner extends DefaultWorkerIdAssigner implements WorkerIdAssigner {
    private final ApplicationContext applicationContext;

    public SpringWorkerIdAssigner(ApplicationContext applicationContext, WorkerNodeStorage workerNodeStorage) {
        super(workerNodeStorage);
        this.applicationContext = applicationContext;
    }

    /**
     * Assign worker id base on database.<p>
     * If there is host name & port in the environment, we considered that the node runs in Docker container<br>
     * Otherwise, the node runs on an actual machine.
     *
     * @return assigned worker id
     */
    @Override
    public long assignWorkerId() {
        WorkerNode workerNode = buildWorkerNode();
        WorkerNode history = workerNodeStorage.getWorkerNodeByHostPort(workerNode.getHostName(), workerNode.getPort());
        if (history!=null){
            return history.getId();
        }
        return super.assignWorkerId();
    }

    /**
     * Build worker node entity by IP and PORT
     */
    @Override
    protected WorkerNode buildWorkerNode() {
        return super.buildWorkerNode();
    }


    @Override
    protected String getActualPort() {
        //先获取端口信息
        String serverPort = applicationContext.getEnvironment().getProperty("server.port");
        if (StringUtils.hasLength(serverPort)){
            return applicationContext.getId()+"-"+serverPort;
        }
        //没有则直接使用唯一标识
        URL resource = Thread.currentThread().getContextClassLoader().getResource(this.getClass().getName().replace(".", "/") + ".class");
        return applicationContext.getId()+"-"+Math.abs(Objects.requireNonNull(resource).toString().hashCode());
    }



}
