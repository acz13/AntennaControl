package me.alchzh.antenna_control.mock_device;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class MockNetworkAntennaServer implements MockAntenna.Forwarder {
    private MockAntenna antenna;

    private ServerSocketChannel serverSocket;
    private SocketChannel client;

    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    public MockNetworkAntennaServer(int baseAz, int baseEl, int minAz, int minEl, int maxAz, int maxEl, int speed) throws IOException {
        antenna = new MockAntenna(this, baseAz, baseEl, minAz, minEl, maxAz, maxEl, speed);

        serverSocket = ServerSocketChannel.open();
    }

    public static int u(double degrees) {
        return (int) (degrees * (Integer.MAX_VALUE / 180.0));
    }

    public static double d(int units) {
        return units * 180.0 / Integer.MAX_VALUE;
    }

    public static void main(String[] args) throws IOException {
        (new MockNetworkAntennaServer(0, 0, 0, 0, u(135), u(70), u(5 / 1000.0)))
                .listen("127.0.0.1", 52532);
    }

    private void read(int length) throws IOException {
        readBuffer.rewind();
        readBuffer.limit(length);

        client.read(readBuffer);
        readBuffer.flip();
    }

    private AntennaCommand readCommand() throws IOException {
        read(1);
        byte code = readBuffer.get();

        AntennaCommand.Type type = AntennaCommand.Type.fromCode(code);
        int length = type.getLength();
        byte[] data;

        if (length == 0) {
            data = new byte[0];
            read(1);
        } else if (length == -1) {
            read(1);
            byte code2 = readBuffer.get();
            int code2length = AntennaEvent.Type.fromCode(code2).getLength();
            data = new byte[1 + code2length];
            data[0] = code2;

            read(code2length + 1);
            readBuffer.get(data, 1, code2length);
        } else {
            data = new byte[length];
            read(length + 1);
            readBuffer.get(data);
        }

        return new AntennaCommand(type, data);
    }

    public void listen(String host, int port) throws IOException {
        serverSocket.socket().bind(new InetSocketAddress(host, port));
        System.out.printf("Listening on... %s:%d\n", host, port);
        client = serverSocket.accept();

        while (true) {
            try {
                AntennaCommand command = readCommand();
                System.out.println(command);

                antenna.commandReceived(command);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    @Override
    public void sendEvent(AntennaEvent event) {
        writeBuffer.clear();
        writeBuffer.put(event.toArray());
        writeBuffer.put((byte) 0x0A);
        writeBuffer.flip();

        try {
            client.write(writeBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
