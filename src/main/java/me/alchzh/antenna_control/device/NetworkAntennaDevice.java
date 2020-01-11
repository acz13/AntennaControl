package me.alchzh.antenna_control.device;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NetworkAntennaDevice extends AntennaDeviceBase implements Runnable {
    private SocketChannel client;
    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    public NetworkAntennaDevice(String host, int port) throws IOException {
        client = SocketChannel.open();

        client.connect(new InetSocketAddress(host, port));

        new Thread(this, "Read loop").start();
    }

    private void read(int length) throws IOException {
        readBuffer.rewind();
        readBuffer.limit(length);

        client.read(readBuffer);
        readBuffer.flip();
    }

    private AntennaEvent readEvent() throws IOException {
        read(1);
        byte code = readBuffer.get();
        read(4);
        int time = readBuffer.getInt();

        AntennaEvent.Type type = AntennaEvent.Type.fromCode(code);
        int length = type.getLength();
        byte[] data;

        if (length == 0) {
            data = new byte[0];
            read(1);
        } else if (length == -1) {
            read(1);
            byte code2 = readBuffer.get();
            int code2length = AntennaCommand.Type.fromCode(code2).getLength();
            data = new byte[1 + code2length];
            data[0] = code2;

            read(code2length + 1);
            readBuffer.get(data, 1, code2length);
        } else {
            data = new byte[length];
            read(length + 1);
            readBuffer.get(data);
        }

        return new AntennaEvent(type, time, data);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AntennaEvent event = readEvent();

                sendEvent(event);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    @Override
    public void submitCommand(AntennaCommand command) {
        writeBuffer.clear();
        writeBuffer.put(command.toArray());
        writeBuffer.put((byte) 0x0A);
        writeBuffer.flip();

        try {
            client.write(writeBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}