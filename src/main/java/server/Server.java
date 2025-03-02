package server;

import common.Topic;
import java.util.HashMap;
import java.util.Map;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Server {
    private static final int PORT = 11111;
    private static final Map<String, Topic> topics = new HashMap<>();

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // поток для обработки подключений
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // поток для обработки данных

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup) // указываем принимающий родительский поток сервера и дочерний поток клиента
                    .channel(NioServerSocketChannel.class)// указываем тип канала для принятия новых TCP/IP подключений
                    .childHandler(new ChannelInitializer<SocketChannel>() { // создаем новое подключение для каждого клиент-соединения
                        @Override
                        protected void initChannel(SocketChannel ch){
                            ch.pipeline().addLast( new StringDecoder(), new StringEncoder(), new ServerHandler()); // добавляем в очередь по порядку: раскодирование входящих строк, кодирование исходящих, наш обработчик
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128) // макс. количество подключений
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // поддержка соединения

            ChannelFuture future = bootstrap.bind(PORT).sync();
            System.out.println("Порт " + PORT + " запущен");
            future.channel().closeFuture().sync(); // ожидание завершения работы канала
        } catch (InterruptedException e) {
            throw new RuntimeException("Соединение было разорвано");
        } finally { // закрываем потоки, дождавшись обработки всех сообщений
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static Map<String, Topic> getTopics(){
        return topics;
    }
}
