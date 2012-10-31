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
	
	private InetAddress IPAddress;
	private int port;
	
	RequestHandler(DatagramPacket receivePacket, byte[] packetBytes, DatagramSocket serverSocket)
	{
		this.receivePacket = receivePacket;
		this.packetBytes = packetBytes;
		this.serverSocket = serverSocket;
		
		readDirectory = "C:/Users/Alex/Desktop/Test/"; //change directory here
		writeDirectory = "C:/Users/Alex/Desktop/"; //change directory here
		
		IPAddress = receivePacket.getAddress();
		port = receivePacket.getPort();
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
		else if(opcode == 4) //acknowledgment packet received
			System.out.println("Acknowledgment received");
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
		
		try
		{
			File fileToSend = new File(fileToSendPath);
			RandomAccessFile f = new RandomAccessFile(fileToSend, "rw");
			DatagramPacket sendPacket;
			
			byte[] fileData = new byte[(int)f.length()];
			f.read(fileData);
			f.close();
			
			int remainingLength = fileData.length;
			System.out.println("LENGTH: " + remainingLength);
			int packetSize = BLOCK_SIZE;
			int blockNum = 1;
			int fileDataOffset = 0;
			
			do
			{
				if(remainingLength < BLOCK_SIZE) //last packet sent
					packetSize = remainingLength;
				
				byte[] sendBytes = new byte[packetSize+4];
				sendBytes[0] = 0;
				sendBytes[1] = 3;
				sendBytes[2] = 0;
				sendBytes[3] = (byte)blockNum;
				
				for(int i = 0; i < packetSize; i++)
					sendBytes[i+4] = fileData[i + fileDataOffset];
				
				sendPacket = new DatagramPacket(
						sendBytes, sendBytes.length, IPAddress, port);
				serverSocket.send(sendPacket);
				
				System.out.println(remainingLength);
				remainingLength -= BLOCK_SIZE;	
				fileDataOffset += BLOCK_SIZE;
				blockNum++;
			} while(remainingLength > 0);
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
			splitter = packetStr.split("\0");
		}
		catch(IOException ex)
		{
			System.err.println("FAIL PARSING: " + ex);
			System.exit(1);
		}
		
		return splitter;
	}
	
	private void sendOACK()
	{
		
	}
}
