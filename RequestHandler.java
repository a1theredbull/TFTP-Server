import java.io.*;
import java.lang.*;
import java.net.*;

public class RequestHandler implements Runnable
{
	private DatagramSocket requestedSocket;
	private Request rInfo;
	private byte[] receivedBytes, requestBytes;
	private DatagramPacket receivedRequest, receivedPacket;
	
	public RequestHandler(DatagramPacket receivedRequest, byte[] requestBytes)
	{
		this.receivedRequest = receivedRequest;
		this.requestBytes = requestBytes;
	}
	
	public void run()
	{
		int randomPort = 30000 + (int)(Math.random() * 30000); //ports 30000 to 600000
		try
		{
			requestedSocket = new DatagramSocket(randomPort);
		}
		catch(SocketException ex)
		{
			System.err.println("Request Connection failed to start: " + ex);
		}
		
		int sendToPort = receivedRequest.getPort();
		rInfo = new Request(receivedRequest.getAddress(), sendToPort, randomPort);
		
		Runnable r = new PacketListener(requestedSocket, receivedBytes, rInfo);
		Thread t = new Thread(r);
		t.start();
		
		byte opcode = requestBytes[1];
		doOperation(opcode);
	}
	
	private void doOperation(byte opcode)
	{
		String[] parsedRequest = parseRequest(opcode);
	
		if(opcode == 1) //read request received
		{
			rInfo.filename = parsedRequest[1];
			int r_blksize = Integer.parseInt(parsedRequest[4]);
			int r_timeout = Integer.parseInt(parsedRequest[6]);
			int r_tsize = Integer.parseInt(parsedRequest[8]);
			if(r_blksize != rInfo.BLOCK_SIZE || r_timeout != rInfo.TIME_OUT || r_tsize != rInfo.T_SIZE)
				sendOACK(r_blksize, r_timeout, r_tsize);
			rInfo.BLOCK_SIZE = r_blksize;
			rInfo.TIME_OUT = r_timeout;
			rInfo.T_SIZE = r_tsize;
			doReadRequest();
		}
		else if(opcode == 2) //write request received
		{
			String[] splitDir = parsedRequest[1].split("\\\\");
			if(splitDir.length > 1)
			{
				StringBuilder dir = new StringBuilder("");
				for(int i = 0; i < splitDir.length-1; i++)
					dir.append(splitDir[i] + "\\");
				rInfo.requestedWriteDir = dir.toString();
				rInfo.filename = splitDir[splitDir.length - 1];
			}
			else
				rInfo.filename = parsedRequest[1];
			int r_blksize = Integer.parseInt(parsedRequest[4]);
			int r_timeout = Integer.parseInt(parsedRequest[6]);
			int r_tsize = Integer.parseInt(parsedRequest[8]);
			if(r_blksize != rInfo.BLOCK_SIZE || r_timeout != rInfo.TIME_OUT || r_tsize != rInfo.T_SIZE)
				sendOACK(r_blksize, r_timeout, r_tsize);
			else
				doWriteRequest();
			rInfo.BLOCK_SIZE = r_blksize;
			rInfo.TIME_OUT = r_timeout;
			rInfo.T_SIZE = r_tsize;
		}
	}

	private void doReadRequest()
	{
		String fileDir = rInfo.readDirectory + rInfo.filename;
		try
		{
			File fileToSend = new File(fileDir);
			if(!fileToSend.exists())
			{
				sendErr("RRQ: Could not find specified file!",
					"Could not locate requested file on server!");
				return;
			}
			
			RandomAccessFile f = new RandomAccessFile(fileToSend, "rw");
			DatagramPacket sendPacket;
			
			byte[] fileData = new byte[(int)f.length()];
			f.read(fileData);
			f.close();
			
			int remainingLength = fileData.length;
			int packetSize = rInfo.BLOCK_SIZE;
			int blockNum = 1;
			int fileDataOffset = 0;
			int lastAck = 1;
			
			do
			{
				if(rInfo.currAckNum != lastAck) continue;
				lastAck = blockNum;
				
				if(remainingLength < rInfo.BLOCK_SIZE) //last packet sent
					packetSize = remainingLength;
				
				byte[] sendBytes = new byte[packetSize+4];
				sendBytes[0] = 0;
				sendBytes[1] = 3;
				sendBytes[2] = 0;
				sendBytes[3] = (byte)blockNum;
				
				for(int i = 0; i < packetSize; i++)
					sendBytes[i+4] = fileData[i + fileDataOffset];
				
				sendPacket = new DatagramPacket(sendBytes, sendBytes.length, rInfo.ip, rInfo.sendToPort);
				requestedSocket.send(sendPacket);
				
				remainingLength -= rInfo.BLOCK_SIZE;
				fileDataOffset += rInfo.BLOCK_SIZE;
				blockNum++;
			} while(remainingLength > 0);
			
			System.out.println("Successfully sent file " + rInfo.filename);
			rInfo.lastAckNum = rInfo.currAckNum;
		}
		catch(IOException ex)
		{
			System.err.println("Sending failed:" + ex);
			System.exit(1);
		}
	}	
	
	private void doWriteRequest()
	{
		//sends acknowledgement/error packet depending if write request is satisfactory
		File toWrite = new File(rInfo.requestedWriteDir);
		if(toWrite.exists())
		{
			File dir = new File(rInfo.requestedWriteDir + rInfo.filename);
				
			try
			{
				dir.createNewFile();
				byte[] ackBytes = new byte[4];
				ackBytes[0] = 0;
				ackBytes[1] = 4;
				ackBytes[2] = 0;
				ackBytes[3] = 0;
				DatagramPacket sendPacket = new DatagramPacket(ackBytes, 4, rInfo.ip, rInfo.sendToPort);
				requestedSocket.send(sendPacket);
			}
			catch(IOException e)
			{
				System.err.println("WRQ Failed: " + e);
				System.exit(1);
			}
		}
		else
			sendErr("WRQ received but could not find requested directory to write to!",
				"Failed to find directory to write to!");
	}
	
	private String[] parseRequest(byte opcode)
	{
		//reformat opcode
		requestBytes[0] = opcode;
		requestBytes[1] = '\0';
		
		ByteArrayInputStream bais = new ByteArrayInputStream(requestBytes);
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
	
	private void sendErr(String localMsg, String packMsg)
	{
		System.err.println(localMsg);
		byte[] errBytes = new byte[100];
		errBytes[0] = 0;
		errBytes[1] = 5;
		errBytes[2] = 0;
		errBytes[3] = 1;
		String msg = packMsg;
		byte[] msgBytes = msg.getBytes();
		int errByteOffset = 4;
		for(int i = 0; i < msgBytes.length; i++)
		{
			errBytes[errByteOffset] = msgBytes[i];
			errByteOffset++;
		}
		errBytes[errByteOffset] = '\0';
		
		try
		{
			DatagramPacket sendPacket = new DatagramPacket(errBytes, errBytes.length, rInfo.ip, rInfo.sendToPort);
			requestedSocket.send(sendPacket);
		}
		catch(IOException e)
		{
			System.err.println("WRQ Failed: " + e);
			System.exit(1);
		}
	}
	
	private void sendOACK(int blksize, int timeout, int tsize)
	{
		byte[] OACKBytes = new byte[100];
		OACKBytes[0] = 0;
		OACKBytes[1] = 6;
		
		String msg = "blksize";
		byte[] msgBytes = msg.getBytes();
		int byteOffset = 2;
		for(int i = 0; i < msgBytes.length; i++)
		{
			OACKBytes[byteOffset] = msgBytes[i];
			byteOffset++;
		}
		OACKBytes[byteOffset] = '\0';
		byteOffset++;
		Integer tmp = new Integer(blksize);
		msgBytes = tmp.toString().getBytes();
		for(int i = 0; i < msgBytes.length; i++)
		{
			OACKBytes[byteOffset] = msgBytes[i];
			byteOffset++;
		}
		byteOffset++;
		
		msg = "timeout";
		msgBytes = msg.getBytes();
		for(int i = 0; i < msgBytes.length; i++)
		{
			OACKBytes[byteOffset] = msgBytes[i];
			byteOffset++;
		}
		OACKBytes[byteOffset] = '\0';
		byteOffset++;
		tmp = new Integer(timeout);
		msgBytes = tmp.toString().getBytes();
		for(int i = 0; i < msgBytes.length; i++)
		{
			OACKBytes[byteOffset] = msgBytes[i];
			byteOffset++;
		}
		byteOffset++;
		
		msg = "tsize";
		msgBytes = msg.getBytes();
		for(int i = 0; i < msgBytes.length; i++)
		{
			OACKBytes[byteOffset] = msgBytes[i];
			byteOffset++;
		}
		byteOffset++;
		tmp = new Integer(blksize);
		msgBytes = tmp.toString().getBytes();
		for(int i = 0; i < msgBytes.length; i++)
		{
			OACKBytes[byteOffset] = msgBytes[i];
			byteOffset++;
		}
		byteOffset++;
		OACKBytes[byteOffset] = '\0';
		
		byte[] trimmed = new byte[byteOffset+1];
		for(int i = 0; i < byteOffset+1; i++)
			trimmed[i] = OACKBytes[i];	
		
		try
		{
			DatagramPacket sendPacket = new DatagramPacket(OACKBytes, OACKBytes.length, rInfo.ip, rInfo.sendToPort);
			requestedSocket.send(sendPacket);
		}
		catch(IOException e)
		{
			System.err.println("OACK Failed: " + e);
			System.exit(1);
		}
	}
}

class PacketListener implements Runnable
{
	private static final int MAX_BYTES = 2048;

	private Request rInfo;
	private byte[] receivedBytes;
	private DatagramSocket requestedSocket;

	public PacketListener(DatagramSocket requestedSocket, byte[] receivedBytes, Request rInfo)
	{
		this.requestedSocket = requestedSocket;
		this.receivedBytes = receivedBytes;
		this.rInfo = rInfo;
	}
	
	public void run()
	{
		while(!requestedSocket.isClosed())
		{
			byte[] rawData = new byte[MAX_BYTES];
			DatagramPacket receivedPacket = new DatagramPacket(rawData, rawData.length);
			try
			{
				requestedSocket.setSoTimeout(rInfo.TIME_OUT * 1000);
				requestedSocket.receive(receivedPacket);
				
				//trims bytes not necessary
				receivedBytes = new byte[receivedPacket.getLength()];
				for(int i = 0; i < receivedBytes.length; i++)
					receivedBytes[i] = rawData[i];
				
				Runnable r = new PacketHandler(receivedBytes, receivedPacket, rInfo, requestedSocket);
				Thread t = new Thread(r);
				t.start();
			}
			catch(SocketTimeoutException ex)
			{
				System.err.println("Request connection timed out.");
				requestedSocket.close();
			}
			catch(IOException ex)
			{
				if(requestedSocket.isClosed())
				{
					System.err.println("Request connection terminated.");
					return;
				}
				System.err.println("Something bad happened: " + ex);
				System.exit(1);
			}
		}
	}
}