import java.io.*;
import java.net.*;

class ServerUDP
{
	private static final int MAX_BYTES = 512;
	private static final int PORT = 69;

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
				Runnable r = new ClientHandler(receivePacket, receiveData);
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

class ClientHandler implements Runnable
{
	private static final int MAX_BYTES = 512;

	byte[] receiveData;
	DatagramPacket receivePacket;
	byte[] opcode = new byte[2];

	ClientHandler(DatagramPacket receivePacket, byte[] receiveData)
	{
		this.receivePacket = receivePacket;
		this.receiveData = receiveData;
	}

	public void run()
	{
		decodeBytes(receiveData);
	}

	public boolean decodeBytes(byte[] toDecode)
	{
		String str = new String(toDecode);
		System.out.println(str);
		opcode[0] = toDecode[0];
		opcode[1] = toDecode[1];
		return true;
	}
}
