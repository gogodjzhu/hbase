import protobf.SimpleProto;

import com.google.protobuf.InvalidProtocolBufferException;

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

    }

}
