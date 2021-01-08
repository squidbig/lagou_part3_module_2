package com.lagou.zkclient;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.List;

public class CreateSession implements IConstant{


    private ZkClient zkClient=null;

    //创建链接
    public ZkClient getZkClient() {
        if(null==zkClient) {
            zkClient=new ZkClient(serverInfo);
        }

        //创建永久节点和服务器列表文件夹
        if(!zkClient.exists(IConstant.parentNode)) {
            zkClient.createPersistent(IConstant.parentNode, true);
        }

        return this.zkClient;
    }






    public static void main(String[] args) {
       //创建链接
        ZkClient zkClient =new ZkClient(CreateSession.serverInfo);

        //创建永久节点和服务器列表文件夹
        if(!zkClient.exists(IConstant.parentNode)) {
            zkClient.createPersistent(IConstant.parentNode, true);
        }

        //获得服务器列表
        List<String> children = zkClient.getChildren(IConstant.parentNode);
        for(String serverNode : children) {

            //获得节点值
            String serverInfo = zkClient.readData(serverNode).toString();

            //监听节点
            zkClient.subscribeDataChanges(serverNode, new IZkDataListener() {

                //值变化，暂时无用
                @Override
                public void handleDataChange(String s, Object o) throws Exception {

                }
                //节点被删除
                @Override
                public void handleDataDeleted(String s) throws Exception {

                }
            });
        }


        zkClient.subscribeChildChanges(IConstant.parentNode, new IZkChildListener() {
            @Override
            public void handleChildChange(String s, List<String> list) throws Exception {

            }
        });


    }
}
