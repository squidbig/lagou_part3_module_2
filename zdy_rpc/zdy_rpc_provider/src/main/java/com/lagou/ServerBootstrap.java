package com.lagou;

import com.lagou.service.UserServiceImpl;
import com.lagou.zkclient.CreateSession;
import com.lagou.zkclient.IConstant;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerBootstrap {

    public static void main(String[] args) throws InterruptedException {

        SpringApplication.run(ServerBootstrap.class, args);

        //本服务器的IP和端口号
        String ip="127.0.0.1";
        String port="8990";

        //获得zookeeper信息
        CreateSession cs=new CreateSession();

        //创建临时顺序节点,向zookeeper注册自身服务器信息
        cs.getZkClient().createEphemeralSequential(IConstant.parentNode+IConstant.serverInfoNode, ip+":"+port);

        //开启服务
        UserServiceImpl.startServer(ip,Integer.parseInt(port));

        //如果运行结束，则关闭session
        cs.getZkClient().close();
    }



}
