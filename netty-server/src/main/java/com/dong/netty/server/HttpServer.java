package com.dong.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * @Author caishaodong
 * @Date 2020-10-28 12:02
 * @Description
 **/
public class HttpServer {
    private static int PORT = 8085;

    public void start(int port) throws Exception {
        EventLoopGroup worker = new NioEventLoopGroup();
        EventLoopGroup boss = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline()
                                    .addLast("codec", new HttpServerCodec())
                                    .addLast("compressor", new HttpContentCompressor())
                                    .addLast("aggregator", new HttpObjectAggregator(65536))
                                    .addLast("handler", new HttpServerHandler());
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            System.out.println("Http Server Started, Listening on " + port);
            channelFuture.channel().closeFuture().sync();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();

        }
    }

    public static void main(String[] args) throws Exception {
        new HttpServer().start(PORT);
    }
}

class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        String content = String.format("Receive http request, uri: %s, method: %s, content: %s%n",
                fullHttpRequest.getUri(), fullHttpRequest.getMethod(), fullHttpRequest.content().toString(CharsetUtil.UTF_8));

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
        channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
