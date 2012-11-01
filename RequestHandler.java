import java.io.*;
import java.net.*;
import java.util.*;

/*handles each packet received*/

class RequestHandler implements Runnable
{
	private int BLOCK_SIZE = 512; //default
	private int TIME_OUT = 10; //default
	private int T_SIZE = 0; //default

	private byte[] packetBytes;
	private DatagramPacket receivePacket;
	private byte opcode;
	private DatagramSocket serverSocket;
	
	private InetAddress IPAddress;
	private int port;
	private RequestStorage storage;
	
	public RequestHandler masterRequest;
	public String readDirectory, writeDirectory;
	public String filename;
	
	ArrayList<byte[]> blocks = new ArrayList<byte[]>();
	
	RequestHandler(DatagramPacket receivePacket, byte[] packetBytes, DatagramSocket serverSocket, RequestStorage storage)
	{
		this.receivePacket = receivePacket;
		this.packetBytes = packetBytes;
		this.serverSocket = serverSocket;
		this.storage = storage;
		
		readDirectory = "C:/Users/Alex/Desktop/Test/"; //change directory here
		writeDirectory = "C:/Users/Alex/Desktop/Test/"; //change directory here
		
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
		masterRequest = storage.getRequest(IPAddress.toString());
		String[] parsedPacket = parsePacket();
		if(opcode == 1 || opcode == 2)
		{
			filename = parsedPacket[1];
			BLOCK_SIZE = Integer.parseInt(parsedPacket[4]);
			TIME_OUT = Integer.parseInt(parsedPacket[6]);
			T_SIZE = Integer.parseInt(parsedPacket[8]);
		}
	
		if(opcode == 1) //read request received
			doReadRequest();
		else if(opcode == 2) //write request received
			doWriteRequest();
		else if(opcode == 3) //data packet received
			writeToFile();
		else if(opcode == 4) //acknowledgment packet received
			; // DO NOTHING
		else if(opcode == 5); //error packet received
			//doError();
	}

	private void doReadRequest()
	{
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
			System.err.println("ERROR SEND:" + ex);
			System.exit(1);
		}
	}
	
	private void doWriteRequest()
	{
		try
		{
			File dir = new File(writeDirectory);
			
			if(dir.exists())
			{
				byte[] ackBytes = new byte[4];
				ackBytes[0] = 0;
				ackBytes[1] = 4;
				ackBytes[2] = 0;
				ackBytes[3] = 0;
				
				DatagramPacket sendPacket = new DatagramPacket(
						ackBytes, 4, IPAddress, port);
				serverSocket.send(sendPacket);
			}
			else
			{
				System.err.println("WRQ: Directory doesn't exist");
				byte[] errBytes = new byte[100];
				errBytes[0] = 0;
				errBytes[1] = 5;
				errBytes[2] = 0;
				errBytes[3] = 1;
				String msg = "Failed to find directory to write to!";
				byte[] msgBytes = msg.getBytes();
				int errByteOffset = 4;
				for(int i = 0; i < msgBytes.length; i++)
				{
					errBytes[errByteOffset] = msgBytes[i];
					errByteOffset++;
				}
				errBytes[errByteOffset] = '\0';
				
				DatagramPacket sendPacket = new DatagramPacket(
						errBytes, errBytes.length, IPAddress, port);
				serverSocket.send(sendPacket);
			}
		}
		catch(IOException ex)
		{
			System.err.println("WRQ FAIL: " + ex);
		}
	}
	
	private void writeToFile()
	{
		try
		{
			byte[] block = new byte[packetBytes.length];
			for(int i = 4; i < packetBytes.length; i++)
				block[i-4] = packetBytes[i];
				
			byte[] ackBytes = new byte[4];
			ackBytes[0] = 0;
			ackBytes[1] = 4;
			ackBytes[2] = 0;
			ackBytes[3] = packetBytes[3];
				
			DatagramPacket sendPacket = new DatagramPacket(
					ackBytes, 4, IPAddress, port);
			serverSocket.send(sendPacket);
			
			System.out.println(packetBytes.length);
			System.out.println(block.length);
			System.out.println(BLOCK_SIZE);
			if(block.length == BLOCK_SIZE)
				masterRequest.blocks.add(block);
			else
			{
				ArrayList<Byte> toWrite = new ArrayList<Byte>();
				for(int i = 0, size = masterRequest.blocks.size(); i < size; i++) //each full block
				{
					byte[] toAppend = masterRequest.blocks.get(i);
					for(int j = 0; j < toAppend.length; j++)
						toWrite.add(toAppend[j]);
				}
				for(int i = 0; i < block.length; i++) //last partial block
				{
					for(int j = 0; j < block.length; j++)
						toWrite.add(block[j]);
				}
				
				byte[] toWriteBytes = new byte[toWrite.size()];
				for(int i = 0, size = toWrite.size(); i < size; i++)
					toWriteBytes[i] = toWrite.get(i);
				
				FileOutputStream fos = new FileOutputStream(masterRequest.writeDirectory + masterRequest.filename);
				fos.write(toWriteBytes);
				fos.close();
				System.out.println("Wrote " + masterRequest.writeDirectory + masterRequest.filename);
			}
		}
		catch(Exception ex)
		{
			System.out.println("FAIL: " + ex);
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