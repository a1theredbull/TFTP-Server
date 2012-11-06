import java.util.*;
import java.net.*;
import java.io.*;

public class Server
{
	private static final int MAX_BYTES = 2048;
	private static final int PORT = 69;
	private DatagramSocket serverSocket;

	public Server()
	{
		try
		{
			serverSocket = new DatagramSocket(PORT);
		}
		catch(SocketException ex)
		{
			System.err.println("Server failed to start: " + ex);
		}
	}

	public void run()
	{
		while(true)
		{
			byte[] rawData = new byte[MAX_BYTES];
			DatagramPacket receivedPacket = new DatagramPacket(rawData, rawData.length);
			
			try
			{
				serverSocket.receive(receivedPacket);
				
				//trims bytes not necessary
				byte[] receivedBytes = new byte[receivedPacket.getLength()];
				for(int i = 0; i < receivedBytes.length; i++)
					receivedBytes[i] = rawData[i];
				
				Runnable r = new RequestHandler(receivedPacket, receivedBytes);
				Thread t = new Thread(r);
				t.start();
			}
			catch(IOException ex)
			{
				System.err.println("Server failed receiving packets: " + ex);
				System.exit(1);
			}
		}	
	}
}