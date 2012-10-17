import java.io.*;
import java.net.*;

class ServerUDP
{
	private static final int MAX_BYTES = 1024;
	private static final int PORT = 9800;

	public static void main(String args[]) throws Exception
	{
		DatagramSocket serverSocket = new DatagramSocket(PORT);
		while(true)
		{
			byte[] receiveData = new byte[MAX_BYTES];
			DatagramPacket receivePacket = new DatagramPacket(
				receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			String msg = new String(receivePacket.getData());
			String reply = msg.toUpperCase();
			
			byte[] sendData = new byte[MAX_BYTES];
			sendData = reply.getBytes();
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			DatagramPacket sendPacket = new DatagramPacket(
				sendData, sendData.length, IPAddress, port);
			serverSocket.send(sendPacket);
		}
	}
}