import java.util.concurrent.*;

public class RequestStorage
{
	ConcurrentHashMap<String, RequestHandler> requestHash;
	
	public RequestStorage()
	{
		requestHash = new ConcurrentHashMap<String, RequestHandler>();
	}
	
	public RequestHandler getRequest(String ip)
	{
		return requestHash.get(ip);
	}
	
	public void putRequest(String ip, RequestHandler request)
	{
		requestHash.put(ip, request);
	}
}