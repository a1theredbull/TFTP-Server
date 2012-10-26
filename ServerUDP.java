import java.io.*;
import java.net.*;

class ServerUDP
{
	private static final int MAX_BYTES = 512;
	private static final int PORT = 9800;

	private File readDirectory, writeDirectory;

	public static void main(String args[]) throws Exception
	{
		DatagramSocket serverSocket = new DatagramSocket(PORT);
		while(true)
		{
			byte[] receiveData = new byte[MAX_BYTES];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try
			{
				serverSocket.receive(receivePacket);
				Runnable r = new RequestHandler(receivePacket, receiveData);
				Thread t = new Thread(r);
				t.start();
			}
			catch(IOException ex)
			{
				System.err.println("Failed to receive.\n");
				System.exit(1);
			}
			
			/*
			byte[] sendData = new byte[MAX_BYTES];
			sendData = reply.getBytes();
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			DatagramPacket sendPacket = new DatagramPacket(
				sendData, sendData.length, IPAddress, port);
			serverSocket.send(sendPacket);
			*/
		}
	}
}

class RequestHandler implements Runnable
{
	private static final int MAX_BYTES = 512;

	byte[] receiveData;
	DatagramPacket receivePacket;
	byte[] opcode = new byte[2];

	RequestHandler(DatagramPacket receivePacket, byte[] receiveData)
	{
		this.receivePacket = receivePacket;
		this.receiveData = receiveData;
	}

	public void run()
	{
		setOpcode(receiveData);
		System.out.println(new String(opcode) + '\n');
	}

	public void setOpcode(byte[] packetBytes)
	{
		opcode[0] = packetBytes[0];
		opcode[1] = packetBytes[1];
	}
}
