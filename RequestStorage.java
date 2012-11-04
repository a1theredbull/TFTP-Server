import java.util.concurrent.*;

public class RequestStorage
{
	public ConcurrentHashMap<String, Request> requests;
	
	public RequestStorage()
	{
		requests = new ConcurrentHashMap<String, Request>();
	}
}