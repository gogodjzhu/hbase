package protobuf;

import com.google.protobuf.BlockingService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * Descriptions...
 *
 * @author DJ.Zhu
 */
public class ProtoBufferTest {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        SimpleProto.PingEntity.Builder builder = SimpleProto.PingEntity.newBuilder();
        builder.setId(1);
        builder.setOpt(1);
        builder.setStr("name");
        builder.setType(SimpleProto.PingEntity.PingType.TYPE2);
        builder.addPhone("123");
        builder.addPhone("123");
        builder.addPhone("123");
        SimpleProto.PingEntity sourceEntity = builder.build();

        //字节码
        byte[] bytes = sourceEntity.toByteArray();

        SimpleProto.PingEntity targetEntity = SimpleProto.PingEntity.parseFrom(bytes);
        System.out.println(targetEntity);

        MyServer myServer = new MyServer();
        BlockingService blockingService = SimpleProto.Ping2PongService
                .newReflectiveBlockingService(new MyBlockingService(myServer));

    }

    private static class MyServer {

        private SimpleProto.PongEntity.Builder builder = SimpleProto.PongEntity.newBuilder();

        private SimpleProto.PongEntity doGet(SimpleProto.PingEntity pingEntity){
            builder.clear();
            builder.setId(pingEntity.getId());
            builder.setStr("hello " + pingEntity.getStr());
            return builder.build();
        }


    }

    private static class MyBlockingService implements SimpleProto.Ping2PongService.BlockingInterface{

        private MyServer server;

        public MyBlockingService(MyServer server){
            this.server = server;
        }

        @Override
        public SimpleProto.PongEntity get(RpcController controller, SimpleProto.PingEntity request) throws ServiceException {
            return this.server.doGet(request);
        }
    }

}
