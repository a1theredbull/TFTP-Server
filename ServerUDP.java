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
				Runnable r = new RequestHandler(receivePacket, receiveData, serverSocket);
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
	private int BLOCK_SIZE = 512; //default
	private int TIME_OUT = 10; //default
	private int T_SIZE = 0; //default

	private byte[] packetBytes;
	private DatagramPacket receivePacket;
	private byte opcode;
	private String readDirectory, writeDirectory;
	private DatagramSocket serverSocket;
	
	RequestHandler(DatagramPacket receivePacket, byte[] packetBytes, DatagramSocket serverSocket)
	{
		this.receivePacket = receivePacket;
		this.packetBytes = packetBytes;
		this.serverSocket = serverSocket;
		
		readDirectory = "C:/Users/Alex/Desktop/"; //change directory here
		writeDirectory = "C:/Users/Alex/Desktop/"; //change directory here
	}

	public void run()
	{
		opcode = packetBytes[1];
		doOperation(opcode);
	}

	private void doOperation(int opcode)
	{
		String[] parsedPacket = parsePacket();
	
		if(opcode == 1) //read request received
			doReadRequest(parsedPacket);
		else if(opcode == 2); //write request received
			//doWriteRequest();
		else if(opcode == 3); //data packet received
			//writeToFile();
		else if(opcode == 4); //acknowledgment packet received
			//getAcknowledgment();
		else if(opcode == 5); //error packet received
			//displayError();
	}

	private void doReadRequest(String[] parsedPacket)
	{
		String filename = parsedPacket[1];
		BLOCK_SIZE = Integer.parseInt(parsedPacket[4]);
		TIME_OUT = Integer.parseInt(parsedPacket[6]);
		T_SIZE = Integer.parseInt(parsedPacket[8]);
		
		System.out.println("Filename: " + filename);
		System.out.println("Blocksize: " + BLOCK_SIZE);
		System.out.println("Timeout: " + TIME_OUT);
		System.out.println("T Size: " + T_SIZE);
		
		String fileToSendPath = readDirectory + filename;
		System.out.println(fileToSendPath);
		
		try
		{
			File fileToSend = new File(fileToSendPath);
			RandomAccessFile f = new RandomAccessFile(fileToSend, "rw");
			DatagramPacket sendPacket;
			
			byte[] all_data = new byte[(int)f.length()];
			f.read(all_data);
			int remaining_data_length = all_data.length;
			System.out.println("LENGTH: " + all_data.length);
			
			byte[] sendData = new byte[all_data.length+4]; //BLOCK_SIZE
			sendData[0] = '0';
			sendData[1] = '1';
			sendData[2] = '0';
			sendData[3] = '0';
			
			for(int i = 4; i < all_data.length+4; i++)
			{
				sendData[i] = all_data[i-4];
			}
			
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			sendPacket = new DatagramPacket(
					sendData, all_data.length, IPAddress, port);
			serverSocket.send(sendPacket);
			
			//for(int i = 0, block_count = 0; i < all_data.length; i++)
			//{
			//	int start_of_block = 0;
			//	if(block_count == 0)
			//		start_of_block = i;
			//		
			//	sendData[block_count] = all_data[i];
			//	if(block_count == BLOCK_SIZE-1 || remaining_data_length < BLOCK_SIZE)
			//	{
			//		//send packet
			//		InetAddress IPAddress = receivePacket.getAddress();
			//		int port = receivePacket.getPort();
			//		if(sendData.length == BLOCK_SIZE)
			//			sendPacket = new DatagramPacket(
			//				sendData, start_of_block, BLOCK_SIZE, IPAddress, port);
			//		else
			//			sendPacket = new DatagramPacket(
			//				sendData, start_of_block, remaining_data_length, IPAddress, port);
			//		serverSocket.send(sendPacket);					
			//		remaining_data_length -= BLOCK_SIZE;
			//		
			//		if(remaining_data_length <= 0)
			//			break;
			//		
			//		//start next packet
			//		if(remaining_data_length < BLOCK_SIZE)
			//			sendData = new byte[remaining_data_length];
			//		else
			//			sendData = new byte[BLOCK_SIZE];
			//		block_count = 0;
			//	}
			//}
			System.out.println("Completed file request.");
		}
		catch(IOException ex)
		{
			System.err.println("ERROR SEND: + ex");
			System.exit(1);
		}
	}
	
	private String[] parsePacket()
	{
		//reformat opcode
		packetBytes[0] = opcode;
		packetBytes[1] = '\0';
		
		ByteArrayInputStream bais = new ByteArrayInputStream(packetBytes);
		BufferedReader d = new BufferedReader(new InputStreamReader(bais));
		String[] splitter = null;
		
		try
		{
			String packetStr = d.readLine();
			System.out.println(packetStr);
			splitter = packetStr.split("\0");
		}
		catch(IOException ex)
		{
			System.err.println("FAIL PARSING: " + ex);
			System.exit(1);
		}
		
		return splitter;
	}
}
