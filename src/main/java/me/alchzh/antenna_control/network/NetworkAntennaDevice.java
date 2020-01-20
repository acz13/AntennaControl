package me.alchzh.antenna_control.network;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.EventEmitterImpl;
import me.alchzh.antenna_control.device.AntennaEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * A device that communicates over a TCP socket
 */
public class NetworkAntennaDevice extends EventEmitterImpl<AntennaEvent> implements AntennaDevice, Runnable {
    private SocketChannel client;
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
    private final ByteBuffer readBuffer = ByteBuffer.allocate(2048);

    /**
     * Connects to a device wrapped in a NetworkAntennaServer
     *
     * @param host Host to bind to
     * @param port Port to listen on
     */
    public NetworkAntennaDevice(String host, int port) throws InterruptedException {
        do {
            try {
                client = SocketChannel.open();

                client.connect(new InetSocketAddress(host, port));
            } catch (IOException e) {
                Thread.sleep(5000);
            }
        } while (client == null || !client.isConnected());

        new Thread(this, "Read loop").start();
    }

    private void read(int length) throws IOException {
        readBuffer.clear();

        try {
            readBuffer.limit(length);
        } catch (IllegalArgumentException e) {
            System.out.println(length);
        }

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
        } else {
            switch (type) {
                case COMMAND_ISSUED:
                    read(1);
                    byte code2 = readBuffer.get();
                    int code2length = AntennaCommand.Type.fromCode(code2).getLength();
                    data = new byte[1 + code2length];
                    data[0] = code2;

                    read(code2length + 1);
                    readBuffer.get(data, 1, code2length);
                    break;
                case MEASUREMENT:
                    read(4);
                    int count = readBuffer.getInt();
                    readBuffer.rewind();
                    data = new byte[Integer.BYTES + count * Float.BYTES];
                    readBuffer.get(data, 0, Integer.BYTES);

                    read(1 + count * Float.BYTES);
                    readBuffer.get(data, Integer.BYTES, count * Float.BYTES);
                    break;
                default:
                    data = new byte[length];
                    read(length + 1);
                    readBuffer.get(data);
            }
        }

        return new AntennaEvent(type, time, data);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && client.isConnected()) {
            try {
                AntennaEvent event = readEvent();

                sendEvent(event);
            } catch (IOException e) {
                System.out.println("Connection closed because of error or forced close");
                break;
            }
        }
    }

    @Override
    public void submitCommand(AntennaCommand command) {
        if (writeBuffer.position() > 0) {
            writeBuffer.compact();
        }

        writeBuffer.put(command.toByteBuffer());
        writeBuffer.put((byte) 0x0A);
        writeBuffer.flip();

        try {
            client.write(writeBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}