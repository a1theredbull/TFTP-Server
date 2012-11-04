import java.util.*;
import java.net.*;

class Request
{
	//defaults
	public int BLOCK_SIZE = 512;
	public int TIME_OUT = 10;
	public int T_SIZE = 0;
	
	public String readDirectory = "C:/Users/Alex/Desktop/Test/";
	public String requestedWriteDir = "C:/Users/Alex/Desktop/Test/";
	public String filename;
	public InetAddress ip;
	public int port;
	
	public int currAckNum = 1;
	public ArrayList<byte[]> blocks = new ArrayList<byte[]>();
	
	public Request(InetAddress ip)
	{
		this.ip = ip;
	}
}