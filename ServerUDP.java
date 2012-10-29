import java.io.*;
import java.net.*;

class ServerUDP
{
	private static final int MAX_BYTES = 512;
	private static final int PORT = 69;

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

	byte[] packetBytes;
	DatagramPacket receivePacket;
	int opcode;
	private File readDirectory, writeDirectory;

	RequestHandler(DatagramPacket receivePacket, byte[] packetBytes)
	{
		this.receivePacket = receivePacket;
		this.packetBytes = packetBytes;
		readDirectory = new File("C:/Users/user/Desktop");
		writeDirectory = new File("C:/Users/user/Desktop");
	}

	public void run()
	{
		opcode = packetBytes[1];
		doOperation(opcode);
	}

	private void doOperation(int opcode)
	{
		if(opcode == 1) //read request received
			doReadRequest();
		else if(opcode == 2); //write request received
			//doWriteRequest();
		else if(opcode == 3); //data packet received
			//writeToFile();
		else if(opcode == 4); //acknowledgment packet received
			//getAcknowledgment();
		else if(opcode == 5); //error packet received
			//displayError();
	}

	private void doReadRequest()
	{
		//receiveData[0], receiveData[1] = 0;
		String parsedPacket = new String(packetBytes);
		System.out.println(parsedPacket);
	}
}
