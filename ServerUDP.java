import java.io.*;
import java.net.*;

public class ServerUDP
{
	private static final int MAX_BYTES = 512;
	private static final int PORT = 69;
	DatagramSocket serverSocket;

	public ServerUDP()
	{
		try
		{
			serverSocket = new DatagramSocket(PORT);
		}
		catch(SocketException ex)
		{
			System.err.println("SERVER START FAIL: " + ex);
		}
	}
	
	public void runServer()
	{
		RequestStorage storage = new RequestStorage();
		while(true)
		{
			byte[] receiveData = new byte[MAX_BYTES];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try
			{
				serverSocket.receive(receivePacket);
				Runnable r = new RequestHandler(receivePacket, receiveData, serverSocket, storage);
				storage.putRequest(receivePacket.getAddress().toString(), (RequestHandler)r);
				Thread t = new Thread(r);
				t.start();
			}
			catch(IOException ex)
			{
				System.err.println("Failed to receive.\n");
				System.exit(1);
			}
		}	
	}
}
