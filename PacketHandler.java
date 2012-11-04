import java.util.*;
import java.net.*;
import java.io.*;

public class PacketHandler implements Runnable
{
	private byte[] receivedBytes;
	private RequestStorage storage;
	private Request rInfo;
	private DatagramSocket serverSocket;
	private DatagramPacket receivedPacket;
	
	public PacketHandler(byte[] receivedBytes, DatagramPacket receivedPacket, RequestStorage storage, DatagramSocket serverSocket)
	{
		this.receivedBytes = receivedBytes;
		this.receivedPacket = receivedPacket;
		this.storage = storage;
		this.rInfo = storage.requests.get(receivedPacket.getAddress().toString());
		this.serverSocket = serverSocket;
	}
	
	public void run()
	{
		byte opcode = receivedBytes[1];
		doOperation(opcode);
	}
	
	private void doOperation(byte opcode)
	{
		parseRequest(opcode);
		if(opcode == 1) //read request received
		{
			String[] parsedRequest = parseRequest(opcode);
			rInfo.filename = parsedRequest[1];
			rInfo.BLOCK_SIZE = Integer.parseInt(parsedRequest[4]);
			rInfo.TIME_OUT = Integer.parseInt(parsedRequest[6]);
			rInfo.T_SIZE = Integer.parseInt(parsedRequest[8]);
			rInfo.port = receivedPacket.getPort();
			doReadRequest();
		}
		else if(opcode == 2) //write request received
		{
			String[] parsedRequest = parseRequest(opcode);
			String[] splitDir = parsedRequest[1].split("/");
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
			rInfo.BLOCK_SIZE = Integer.parseInt(parsedRequest[4]);
			rInfo.TIME_OUT = Integer.parseInt(parsedRequest[6]);
			rInfo.T_SIZE = Integer.parseInt(parsedRequest[8]);
			rInfo.port = receivedPacket.getPort();
			doWriteRequest();
		}
		else if(opcode == 3) //data packet received
			writeToFile();
		else if(opcode == 4) //acknowledgment packet received
			getAcknowledgement();
		else if(opcode == 5); //error packet received
			//doError();
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
				
				sendPacket = new DatagramPacket(sendBytes, sendBytes.length, rInfo.ip, rInfo.port);
				serverSocket.send(sendPacket);
				
				remainingLength -= rInfo.BLOCK_SIZE;	
				fileDataOffset += rInfo.BLOCK_SIZE;
				blockNum++;
			} while(remainingLength > 0);
			
			System.out.println("Successfully sent file " + rInfo.filename);
			storage.requests.remove(receivedPacket.getAddress().toString());
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
				DatagramPacket sendPacket = new DatagramPacket(ackBytes, 4, rInfo.ip, rInfo.port);
				serverSocket.send(sendPacket);
				System.out.println("Testing");
			}
			catch(IOException e)
			{
				System.err.println("WRQ Failed.");
				System.exit(1);
			}
		}
		else
			sendErr("WRQ received but could not find requested directory to write to!",
				"Failed to find directory to write to!");
	}
	
	private void writeToFile()
	{
		if(rInfo.currAckNum != receivedBytes[3]) return;

		try
		{
			byte[] block = new byte[receivedBytes.length-4];
			for(int i = 4; i < receivedBytes.length; i++)
				block[i-4] = receivedBytes[i];
				
			byte[] ackBytes = new byte[4];
			ackBytes[0] = 0;
			ackBytes[1] = 4;
			ackBytes[2] = 0;
			ackBytes[3] = receivedBytes[3];
			rInfo.currAckNum++;
			System.out.println(rInfo.currAckNum);
			DatagramPacket sendPacket = new DatagramPacket(ackBytes, 4, rInfo.ip, rInfo.port);
			serverSocket.send(sendPacket);
			
			if(block.length == rInfo.BLOCK_SIZE)
				rInfo.blocks.add(block);
			else
			{
				rInfo.blocks.add(block);
				ArrayList<Byte> toWrite = new ArrayList<Byte>();
				for(int i = 0, size = rInfo.blocks.size(); i < size; i++) //each full block
				{
					byte[] toAppend = rInfo.blocks.get(i);
					for(int j = 0; j < toAppend.length; j++)
						toWrite.add(toAppend[j]);
				}
				
				byte[] toWriteBytes = new byte[toWrite.size()];
				for(int i = 0, size = toWrite.size(); i < size; i++)
					toWriteBytes[i] = toWrite.get(i);
				
				FileOutputStream fos = new FileOutputStream(rInfo.requestedWriteDir + rInfo.filename);
				fos.write(toWriteBytes);
				fos.close();
				System.out.println("Wrote " + rInfo.requestedWriteDir + rInfo.filename);
				storage.requests.remove(receivedPacket.getAddress().toString());
			}
		}
		catch(Exception ex)
		{
			System.out.println("FAIL: " + ex);
			System.exit(1);
		}
	}
	
	private void getAcknowledgement()
	{
		rInfo.currAckNum = receivedBytes[3];
	}
	
	private String[] parseRequest(byte opcode)
	{
		//reformat opcode
		receivedBytes[0] = opcode;
		receivedBytes[1] = '\0';
		
		ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
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
			DatagramPacket sendPacket = new DatagramPacket(errBytes, errBytes.length, rInfo.ip, rInfo.port);
			serverSocket.send(sendPacket);
		}
		catch(IOException e)
		{
			System.err.println("WRQ Failed.");
			System.exit(1);
		}
	}
}
