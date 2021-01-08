package com.lagou.client;

import com.lagou.service.UserService;


public class ClientBootStrap {

   // public static  final String providerName="UserService#sayHello#";

    public static void main(String[] args) throws InterruptedException {

        RpcConsumer rpcConsumer = new RpcConsumer();

        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class);

        while (true){
            Thread.sleep(2000);
            proxy.sayHello("are you ok?");
            System.out.println("已响应");
        }


    }




}
