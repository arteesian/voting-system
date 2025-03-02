package client;
import java.util.Scanner;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Client {
    public static void main(String[] args) {
        EventLoopGroup group = new NioEventLoopGroup(); // поток для передачи данных

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new StringDecoder(), new StringEncoder(), new ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect("localhost", 11111).sync(); // настраиваем подключение к серверу
            Channel channel = future.channel();
            System.out.println("Клиент подключен к серверу");

            Scanner scanner = new Scanner(System.in);
            while(true){
                String prompt = scanner.nextLine();
                channel.writeAndFlush(prompt);
            }

        }catch (InterruptedException e) {
                throw new RuntimeException("Соединение было разорвано");
        }finally {
            group.shutdownGracefully();
        }
    }
}
