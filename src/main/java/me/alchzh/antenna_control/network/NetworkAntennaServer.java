package me.alchzh.antenna_control.network;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaEvent;
import me.alchzh.antenna_control.device.EventEmitter;
import me.alchzh.antenna_control.mock_device.MockAntennaDevice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.EventListener;

import static me.alchzh.antenna_control.util.Units.u;

/**
 * A server that wraps an AntennaDevice to communicate over a TCP socket
 */
public class NetworkAntennaServer implements EventEmitter.Listener<AntennaEvent> {
    private final AntennaDevice device;

    private ServerSocketChannel serverSocket;
    private SocketChannel client;

    private final ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
    private final ByteBuffer readBuffer = ByteBuffer.allocate(2048);

    /**
     * Wraps a device to serve over a network
     *
     * @param device Device to wrap
     * @throws IOException On any IOException
     */
    public NetworkAntennaServer(AntennaDevice device) throws IOException {
        this.device = device;
        device.addEventListener(this);

        serverSocket = ServerSocketChannel.open();
    }

    /**
     * Spins up a server using MockAntennaDevice to listen on port 52532
     *
     * @param args Command line arguments
     * @throws IOException On any IOException
     */
    public static void main(String[] args) throws IOException {
        AntennaDevice mockAntenna = new MockAntennaDevice(0, 0, 0, 0, u(135), u(70), u(100 / 1000.0));
        NetworkAntennaServer server = new NetworkAntennaServer(mockAntenna);

        server.listen("127.0.0.1", 52532);
    }


    private void read(int length) throws IOException {
        readBuffer.clear();
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

    /**
     * Listen on the specified host and port
     * At the moment, accepts only one client connection and reads command by command
     *
     * @param host Host to bind to
     * @param port Port to listen on
     */
    public void listen(String host, int port) {
        try {
            serverSocket.socket().bind(new InetSocketAddress(host, port));
            System.out.printf("Listening on... %s:%d\n", host, port);
            client = serverSocket.accept();
            System.out.printf("Client obtained %s\n", client.getRemoteAddress());

            while (client.isConnected()) {
                AntennaCommand command = readCommand();
                System.out.println(command);

                device.submitCommand(command);
            }
        } catch (IOException e) {
            System.out.println("Server closed because of error or forced close");
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        device.removeEventListener(this);
        device.submitCommand(AntennaCommand.Type.POWEROFF);

        if (client != null) {
            client.close();
        }

        serverSocket.close();
    }

    @Override
    public void eventOccurred(AntennaEvent event) {
        if (writeBuffer.position() > 0) {
            writeBuffer.compact();
        }

        try {
            writeBuffer.put(event.toArray());
            writeBuffer.put((byte) 0x0A);
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }
        writeBuffer.flip();

        try {
            client.write(writeBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
