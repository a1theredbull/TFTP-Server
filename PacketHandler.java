import java.util.*;
import java.net.*;
import java.io.*;

public class PacketHandler implements Runnable
{
	private byte[] receivedBytes;
	private Request rInfo;
	private DatagramSocket requestedSocket;
	private DatagramPacket receivedPacket;
	
	public PacketHandler(byte[] receivedBytes, DatagramPacket receivedPacket, Request rInfo, DatagramSocket requestedSocket)
	{
		this.receivedBytes = receivedBytes;
		this.receivedPacket = receivedPacket;
		this.rInfo = rInfo;
		this.requestedSocket = requestedSocket;
	}
	
	public void run()
	{
		byte opcode = receivedBytes[1];
		doOperation(opcode);
	}
	
	private void doOperation(byte opcode)
	{
		if(opcode == 3) //data packet received
			writeToFile();
		else if(opcode == 4) //acknowledgment packet received
			getAcknowledgement();
		else if(opcode == 5); //error packet received
			//doError();
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
			DatagramPacket sendPacket = new DatagramPacket(ackBytes, 4, rInfo.ip, rInfo.sendToPort);
			requestedSocket.send(sendPacket);
			
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
				requestedSocket.close();
			}
		}
		catch(Exception ex)
		{
			System.err.println("FAIL: " + ex);
			System.exit(1);
		}
	}
	
	private void getAcknowledgement()
	{
		rInfo.currAckNum = receivedBytes[3];
		if(rInfo.lastAckNum == rInfo.currAckNum)
		{
			System.out.println("Finished and closed.");
			requestedSocket.close();
		}
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
			System.err.println("WRQ Failed.");
			System.exit(1);
		}
	}
}
