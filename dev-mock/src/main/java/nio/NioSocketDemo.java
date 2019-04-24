package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import org.junit.Test;
import com.sun.tools.javac.util.ArrayUtils;

public class NioSocketDemo {

    private static final int NIO_BUFFER_LIMIT = 5;

    @Test
    public void testStartServer() throws Exception {
        new SimpleServer(8080).listen();
    }

    @Test
    public void testStartClient() throws InterruptedException, IOException {
        while (true){
            try {
                new SimpleClient("localhost", 8080).listen();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private class SimpleClient {
        //多路复用器、选择器（具体看使用的操作系统以及jdk版本，1.5有可能就是select而1.7就是epoll）
        private Selector selector;

        public SimpleClient(String serverIp, int port) throws IOException {
            SocketChannel channel = SocketChannel.open();
            // 将该通道设置为非阻塞
            channel.configureBlocking(false);
            // 获取多路复用器实例
            selector = Selector.open();
            // 客户端连接服务器，需要调用channel.finishConnect();才能实际完成连接。
            channel.connect(new InetSocketAddress(serverIp, port));
            // 为该通道注册SelectionKey.OP_CONNECT事件，也就是将channel的fd和感兴趣的事件添加到多路复用器中
            channel.register(selector, SelectionKey.OP_CONNECT);
        }

        public void listen() throws IOException, InterruptedException {
            //System.out.println("Client starting...");
            // 轮询访问selector
            while (true) {
                if (!selector.isOpen()){
                    //System.out.println("selector is close. Bye");
                    break;
                }
                // 选择注册过的io操作的事件(第一次为SelectionKey.OP_CONNECT)
                selector.select();
                //获取注册在该复用器上的channel和channelEvent
                Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
                while (ite.hasNext()) {
                    SelectionKey key = ite.next();
                    // 删除已选的key，防止重复处理
                    ite.remove();
                    if (key.isConnectable()) {
                        SocketChannel channel = (SocketChannel) key.channel();

                        // 如果正在连接，则等待其完成连接
                        if (channel.isConnectionPending()) {
                            channel.finishConnect();
                        }
                        //System.out.println("Client connected");
                        String message = "I am client";
                        // 向服务器发送消息
                        channel.write(ByteBuffer.wrap(message.getBytes()));
                        // 连接成功后，注册接收服务器消息的事件
                        channel.register(selector, SelectionKey.OP_READ);
                        //System.out.println("Send message to server:" + message);
                    } else if (key.isReadable()) { // 判断该channel的channelEvent事件类型，也就是reactor模式中的分发器，如果把里面处理过程进行封装就是处理器了
                        SocketChannel channel = (SocketChannel) key.channel();

                        ByteBuffer buffer = ByteBuffer.allocate(NIO_BUFFER_LIMIT);
                        int bytesCnt = channelIO(channel, null, buffer);
                        if (bytesCnt == -1){
                            channel.close();
                            continue;
                        }
                        if (bytesCnt == 0){
                            continue;
                        }
                        StringBuffer stringBuffer = new StringBuffer();
                        while (bytesCnt > 0){
                            //切换为读取模式
                            buffer.flip();
                            //使用limit参数获取想要的数据
                            byte[] data = Arrays.copyOf(buffer.array(), buffer.limit());
                            stringBuffer.append(new String(data));
                            bytesCnt = channelIO(channel, null, buffer);
                        }
                        String message = stringBuffer.toString();
                        //System.out.println("Receive message from server:" + message);
                        channel.close();
                        selector.close();
                    }
                }
            }
        }
    }

    public class SimpleServer {
        //多路复用器
        private Selector selector;

        public SimpleServer(int port) throws IOException {
            // 获取一个ServerSocket通道
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            // 获取多路复用器对象
            selector = Selector.open();
            // 将通道管理器与通道绑定，并为该通道注册SelectionKey.OP_ACCEPT事件，
            // 只有当该事件到达时，Selector.select()会返回，否则一直阻塞。
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        }

        public void listen() throws IOException {
            //System.out.println("Server starting...");
            // 使用轮询访问selector
            while (true) {
                selector.select();
                Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
                while (ite.hasNext()) {
                    SelectionKey key = ite.next();
                    ite.remove();
                    // 客户端请求连接事件
                    if (key.isAcceptable()) {
                        //System.out.println("Server accept client request.");
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        // 获得客户端连接通道
                        SocketChannel channel = server.accept();
                        channel.configureBlocking(false);
                        String message = "Hello there.";
                        // 向客户端发消息
                        channel.write(ByteBuffer.wrap(message.getBytes()));
                        // 在与客户端连接成功后，为客户端通道注册SelectionKey.OP_READ事件
                        channel.register(selector, SelectionKey.OP_READ);
                        //System.out.println("Say hello to client:" + message);
                    } else if (key.isReadable()) {// 有可读数据事件
                        SocketChannel channel = (SocketChannel) key.channel();

                        ByteBuffer buffer = ByteBuffer.allocate(NIO_BUFFER_LIMIT);
                        int bytesCnt = channelIO(channel, null, buffer);
                        if (bytesCnt == -1){
                            channel.close();
                            continue;
                        }
                        if (bytesCnt == 0){
                            continue;
                        }
                        StringBuffer stringBuffer = new StringBuffer();
                        while (bytesCnt > 0){
                            //切换为读取模式
                            buffer.flip();
                            //使用limit参数获取想要的数据
                            byte[] data = Arrays.copyOf(buffer.array(), buffer.limit());
                            stringBuffer.append(new String(data));
                            bytesCnt = channelIO(channel, null, buffer);
                        }
                        String message = stringBuffer.toString();
                        //System.out.println("Receive message from client:" + message);
                    }
                }
            }
        }
    }

    /**
     * Only one of readCh or writeCh should be non-null.
     *
     * @param readCh read channel
     * @param writeCh write channel
     * @param buf buffer to read or write into/out of
     * @return bytes written
     * @throws IOException e
     */
    private static int channelIO(ReadableByteChannel readCh,
                                 WritableByteChannel writeCh,
                                 ByteBuffer buf) throws IOException {

        int originalLimit = buf.limit();
        int initialRemaining = buf.remaining();
        int ret = 0;

        while (buf.remaining() > 0) {
            try {
                //获取单批次处理的长度
                //NIO_BUFFER_LIMIT 是Buffer的长度，一般设置一个比普遍请求都大的值，以便可以一次完成读取，hbase为64k
                int ioSize = Math.min(buf.remaining(), NIO_BUFFER_LIMIT);
                buf.limit(buf.position() + ioSize);

                ret = (readCh == null) ? writeCh.write(buf) : readCh.read(buf);

                if (ret < ioSize) {
                    break;
                }

            } finally {

                buf.limit(originalLimit);
            }
        }

        int nBytes = initialRemaining - buf.remaining();
        return (nBytes > 0) ? nBytes : ret;
    }

}