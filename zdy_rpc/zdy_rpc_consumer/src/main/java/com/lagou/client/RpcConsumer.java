package com.lagou.client;

import com.lagou.service.JSONSerializer;
import com.lagou.service.RpcEncoder;
import com.lagou.service.RpcRequest;
import com.lagou.zkclient.CreateSession;
import com.lagou.zkclient.IConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.I0Itec.zkclient.IZkChildListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RpcConsumer {
    //创建线程池对象
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    //缓存线程对象
    Map<String,UserClientHandler> map=new HashMap<String,UserClientHandler>();

    //1.创建一个代理对象 providerName：UserService#sayHello are you ok?
    public Object createProxy(final Class<?> serviceClass){

        //链接zookeeper服务器
        CreateSession cs=new CreateSession();
        //注册节点监听
        cs.getZkClient().subscribeChildChanges(IConstant.parentNode, new IZkChildListener() {
            @Override
            public void handleChildChange(String s, List<String> list) throws Exception {
                Set<String> set = new HashSet<>(list);
                for(String keyName:map.keySet()) {
                    //对象不在新的列表中
                    if(!set.contains(keyName)) {
                        //唤醒并结束该线程
                        map.get(keyName).notify();
                    }
                }
            }
        });

        //借助JDK动态代理生成代理对象
        return  Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{serviceClass}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //（1）调用初始化netty客户端的方法
                //获得服务器列表
                List<String> children = cs.getZkClient().getChildren(IConstant.parentNode);
                for(String serverNode : children) {
                    //获得节点值
                    String serverInfo = cs.getZkClient().readData(serverNode).toString();
                    if(serverInfo.indexOf(":")>=0) {
                        //获得ip和port
                        String[] str = serverInfo.split(":");
                        String ip=str[0];
                        int port=Integer.parseInt(str[1]);

                        //根据每一个ip地址，创建一个线程对象
                        UserClientHandler userClientHandler =new UserClientHandler();
                        EventLoopGroup group = new NioEventLoopGroup();

                        //初始化netty客户端
                        Bootstrap bootstrap = new Bootstrap();
                        bootstrap.group(group)
                                .channel(NioSocketChannel.class)
                                .option(ChannelOption.TCP_NODELAY,true)
                                .handler(new ChannelInitializer<SocketChannel>() {
                                    protected void initChannel(SocketChannel ch) throws Exception {
                                        ChannelPipeline pipeline = ch.pipeline();
                                        pipeline.addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()));
                                        pipeline.addLast(new StringDecoder());
                                        pipeline.addLast(userClientHandler);
                                    }
                                });

                        bootstrap.connect(ip,port).sync();

                        //封装
                        RpcRequest request = new RpcRequest();
                        String requestId = UUID.randomUUID().toString();
                        System.out.println(requestId);

                        String className = method.getDeclaringClass().getName();
                        String methodName = method.getName();

                        Class<?>[] parameterTypes = method.getParameterTypes();

                        request.setRequestId(requestId);
                        request.setClassName(className);
                        request.setMethodName(methodName);
                        request.setParameterTypes(parameterTypes);
                        request.setParameters(args);

                        // 设置参数
                        userClientHandler.setPara(request);
                        System.out.println(request);
                        System.out.println("设置参数完成");

                        // 去服务端请求数据
                        executor.submit(userClientHandler).get();
                        map.put(serverNode,userClientHandler);
                    }
                }
                return null;
            }
        });
    }
}
