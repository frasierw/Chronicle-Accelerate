import cash.xcl.api.AllMessages;
import cash.xcl.api.ClientOut;
import cash.xcl.api.XCLClient;
import cash.xcl.api.XCLServer;
import cash.xcl.api.dto.CreateNewAddressCommand;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.salt.Ed25519;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class XCLClientServerTest {
    @Test
    public void test() throws IOException {
        Bytes privateKey = Bytes.elasticByteBuffer(32);
        privateKey.zeroOut(0, 32);
        privateKey.writeSkip(32);

        Bytes publicKey = Bytes.elasticByteBuffer(Ed25519.PUBLIC_KEY_LENGTH);
        Bytes secretKey = Bytes.elasticByteBuffer(Ed25519.SECRET_KEY_LENGTH);
        Ed25519.privateToPublicAndSecret(publicKey, secretKey, privateKey);

        StringWriter out = new StringWriter();
        AllMessages logging = Mocker.logging(AllMessages.class, "", out);
        XCLServer server = new XCLServer("test", 9900, 1, secretKey, logging);

        List<InetSocketAddress> addresses = Arrays.asList(new InetSocketAddress("localhost", 9900));
        AllMessages logging2 = Mocker.logging(AllMessages.class, "", out);
        ClientOut client = new XCLClient("test-client", addresses, 2, secretKey, logging2);

        client.createNewAddressCommand(new CreateNewAddressCommand().region("usny").publicKey(publicKey));
        for (int i = 0; i <= 20; i++) {
            assertTrue(i < 20);
            Jvm.pause(Jvm.isDebug() ? 2000 : 25);
            if (out.toString().length() > 0) break;
            System.out.println(out);
        }
        assertEquals("createNewAddressCommand[" +
                "!cash.xcl.api.dto.CreateNewAddressCommand {\n" +
                "  sourceAddress: 2,\n" +
                "  eventTime: 0,\n" +
                "  publicKey: !!binary O2onvM62pC1io6jQKm8Nc2UyFXcd4kOmOsBIoYtZ2ik=,\n" +
                "  sourceIP: \"\",\n" +
                "  region: usny\n" +
                "}\n" +
                "]\n", out.toString());

        client.close();
        server.close();
    }
}
