import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class Server
{
	private static final int MAX_BYTES = 2048;
	private static final int PORT = 69;
	private DatagramSocket serverSocket;
	private RequestStorage storage;

	public Server()
	{
		try
		{
			serverSocket = new DatagramSocket(PORT);
			storage = new RequestStorage();
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
				
				InetAddress ip = receivedPacket.getAddress();
				String ipKey = ip.toString();
				
				storage.requests.putIfAbsent(ipKey, new Request(ip));
				
				Runnable r = new PacketHandler(receivedBytes, receivedPacket, storage, serverSocket);
				Thread t = new Thread(r);
				t.start();
			}
			catch(IOException ex)
			{
				System.err.println("Failed receiving packets: " + ex);
				System.exit(1);
			}
		}	
	}
}