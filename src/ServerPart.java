import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


public class ServerPart 
{
	//Data structure to store connection data
	class ClientData
	{

		String Ip;
		int Port;
	}
	ArrayList<ClientData> ConnectedClients;
	//Holds Files in server
	ArrayList<String> ServerFiles;
	//Foldername to look at
	String FolderName;
	//Port its working on
	int MyPort;
	public byte[] fileByteContext;

	public ServerPart()
	{
		System.out.println("ServerPart()");

		//Set Default Parameters 
		this.FolderName="wwwroot";
		this.ServerFiles=this.GetFileNames();
		this.ConnectedClients=new ArrayList<ServerPart.ClientData>();
		this.MyPort=8080;
	}
	public void SetMyPort(int Port)
	{
		System.out.println("SetMyPort(int Port)");
		this.MyPort=Port;
	}
	public void SetFolderName(String NewFolderName)
	{
		System.out.println("SetFolderName(String NewFolderName)");

		this.FolderName=NewFolderName;
	}
	ArrayList<String> GetFileNames()
	{
		System.out.println("GetFileNames()");

		//Load Filenames from its data folder to a list
		ArrayList<String> Files=new ArrayList<String>();
		File folder = new File(this.FolderName);
		for (final File fileEntry : folder.listFiles()) 
		{   
			Files.add(fileEntry.getName());
		}
		return Files;
	}
	ArrayList<String> QueryFileNames(String Query)
	{
		System.out.println("QueryFileNames(String Query)");

		//SErach Fro files
		//.* means all files
		if(Query.equals(".*"))
		{
			return this.GetFileNames();
		}
		ArrayList<String> Files=new ArrayList<String>();
		File folder = new File(this.FolderName);
		//Get rid of . before extension name
		Query = Query.substring(1);
		//Check extension
		for (final File fileEntry : folder.listFiles()) 
		{   
			//Check Extension
			String extension = "";
			int i = fileEntry.getName().lastIndexOf('.');
			if (i > 0) {
				extension = fileEntry.getName().substring(i+1);
			}
			if(extension.equals(Query))
			{
				Files.add(fileEntry.getName());
			}
		}
		return Files;
	}
	void AddConnection(String Ip,int Port)
	{
		System.out.println("AddConnection(String Ip,int Port)");

		//this works like a public static variable and is a database
		ClientData ServerData=new ClientData();
		//client.getinetadress starts with /
		if(Ip.startsWith("/"))
			ServerData.Ip=Ip.substring(1);
		else
			ServerData.Ip=Ip;


		ServerData.Port=Port;
		//does not add same connection
		//avoids loops
		for(ClientData cd : this.ConnectedClients){
			if(cd.Ip.equals(ServerData.Ip)){
				if(cd.Port==ServerData.Port){
					return;
				}
			}
		}
		//connected clients database
		this.ConnectedClients.add(ServerData);
	}
	
	void Connect(String Ip,int Port)
	{
		System.out.println("Connect(String Ip,int Port)");

		try
		{
			//prepare and send connect packet to let server know
			Socket s=new Socket(Ip,Port);
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			out.println("POST / HTTP/1.0");
			out.println("Connection: Keep-Alive");
			out.println("User-Agent: CS328-Servant");
			out.println("Accept-Language: en");
			out.println("Content-type: application/x-www-form-urlencoded");
			out.println("Content-length:24");
			out.println("");
			out.println("ACTION=Connect&PORT="+this.MyPort);
			out.flush();
			out.close();
			s.close();
			this.AddConnection(Ip, Port);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	ArrayList<String> Request(String Query,String TTL)
	{
		System.out.println("Request(String Query,String TTL)");

		//Ask For files or extensions
		ArrayList<String> ReplyData=new ArrayList<String>();

		//if it starts with extension only otherwise whole file
		if(Query.startsWith("."))
		{
			try
			{
				for(ClientData Connection:this.ConnectedClients)
				{
					//prepare the header for the query
					Socket s=new Socket(Connection.Ip,Connection.Port);//open socket
					PrintWriter out = new PrintWriter(s.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
					out.println("POST / HTTP/1.0");
					out.println("Connection: Keep-Alive");
					out.println("User-Agent: CS328-Servant");
					out.println("Accept-Language: en");
					out.println("Content-type: application/x-www-form-urlencoded");
					//text size of the line 164 + the byte size of the data itself
					out.println("Content-length:"+(22+Query.length()));
					out.println("");
					//Port is used to avoid loops
					out.println("QUERY="+Query+"&TTL="+TTL+"&PORT="+this.MyPort);//what does the requesting side want
					out.flush();
					ReplyData.addAll(this.ParseQueryReply(in));
					in.close();
					out.close();
					s.close();
				}
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			RequestFile(Query,TTL);
		}
		return ReplyData;
	}

	ArrayList<String> Request(String Query,String TTL,String Ip,int Port)
	{
		System.out.println("Request(String Query,String TTL,String Ip,int Port)");

		//Same function as above except added extra guards against loops so that it won't check itself again
		//the comments are the same as the function above.
		ArrayList<String> ReplyData=new ArrayList<String>();

		if(Query.startsWith("."))
		{
			try
			{
				for(ClientData Connection:this.ConnectedClients)
				{
					if(Connection.Ip.equals(Ip))
					{
						if(Connection.Port==Port)
						{
							continue;
						}
					}
					Socket s=new Socket(Connection.Ip,Connection.Port);
					PrintWriter out = new PrintWriter(s.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
					out.println("POST / HTTP/1.0");
					out.println("Connection: Keep-Alive");
					out.println("User-Agent: CS328-Servant");
					out.println("Accept-Language: en");
					out.println("Content-type: application/x-www-form-urlencoded");
					out.println("Content-length:"+(22+Query.length()));
					out.println("");
					out.println("QUERY="+Query+"&TTL="+TTL+"&PORT="+this.MyPort);

					out.flush();
					ReplyData.addAll(this.ParseQueryReply(in));
					in.close();
					out.close();
					s.close();
				}
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			RequestFile(Query,TTL);
		}
		return ReplyData;
	}



	boolean CheckFile(String FileName)
	{
		System.out.println("CheckFile(String FileName)");

		//Check if the file exists in file list
		for(String File:this.ServerFiles)
		{
			if(FileName.equals(File))
			{
				return true;
			}
		}
		return false;
	}
	
	boolean RequestFile(String Query,String TTL)
	{
		System.out.println("RequestFile(String Query,String TTL)");

		//Request File from connections 
		ArrayList<String> ReplyData=new ArrayList<String>();
		try
		{
			for(ClientData Connection:this.ConnectedClients)
			{
				Socket s=new Socket(Connection.Ip,Connection.Port);
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				//BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				//Data input stream is required to avoid problems regarding binary and text data
				DataInputStream in = new DataInputStream(s.getInputStream());
				out.println("POST / HTTP/1.0");
				out.println("Connection: Keep-Alive");
				out.println("User-Agent: CS328-Servant");
				out.println("Accept-Language: en");
				out.println("Content-type: application/x-www-form-urlencoded");
				out.println("Content-length:"+(21+Query.length()));
				out.println("");
				out.println("FILE="+Query+"&TTL="+TTL+"&PORT="+this.MyPort);
				out.flush();
				//If parsefile reply true file is found otherwise continue
				if(this.ParseFileReply(in,Query,s)){
					break;
				}
				in.close();
				out.close();
				s.close();
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	boolean RequestFile(String Query,String TTL,String Ip,int Port)
	{
		System.out.println("RequestFile(String Query,String TTL,String Ip,int Port)");

		//Same function as above except added extra guards against loops so that it won't check itself again
		//the comments are the same as the function above.
		try
		{
			for(ClientData Connection:this.ConnectedClients)
			{
				if(Connection.Ip.equals(Ip))
				{
					if(Connection.Port==Port)
					{
						continue;
					}
				}
				Socket s=new Socket(Connection.Ip,Connection.Port);
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				DataInputStream in = new DataInputStream(s.getInputStream());
				out.println("POST / HTTP/1.0");
				out.println("Connection: Keep-Alive");
				out.println("User-Agent: CS328-Servant");
				out.println("Accept-Language: en");
				out.println("Content-type: application/x-www-form-urlencoded");
				out.println("Content-length:"+(21+Query.length()));
				out.println("");
				out.println("FILE="+Query+"&TTL="+TTL+"&PORT="+this.MyPort);
				out.flush();
				if(this.ParseFileReply(in,Query,3))
				{
					in.close();
					out.close();
					s.close();
					return true;
				}
				in.close();
				out.close();
				s.close();
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	ArrayList<String> ParseQueryReply(BufferedReader s) throws IOException
	{
		System.out.println("ParseQueryReply(BufferedReader s)");

		//Parse Reply and adds files to a list
		ArrayList<String> ReplyData=new ArrayList<String>();
		s.readLine();//first 7 lines are skipped, hence readline() but don't save them
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		int Size=Integer.parseInt(s.readLine());
		for(int i=0;i<Size;++i)
		{
			ReplyData.add(s.readLine());
		}
		return ReplyData;
	}
	
	boolean ParseFileReply(DataInputStream s,String query,Socket socket) throws IOException
	{
		System.out.println("ParseFileReply(DataInputStream s,String query,Socket socket)");

		//Parse file request reply if yes(file found) save file 
		//otherwise returns false
		//these are deprecated functions because of binary data writing problems, I changed
		//to DataInputStream instead of something else.
		//the reason there are 2 of this is because of propagation
		s.readLine();//same logic as above but check for "YES"
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		String reply= s.readLine();
		if(reply.equals("YES")){
			String res=s.readLine();
			res=s.readLine();
			res=s.readLine();
			res=s.readLine();
			res=s.readLine();
			int cl=Integer.parseInt(s.readLine().split(" ")[1]);//content-length
			res=s.readLine();
			//read file as binary and save 
			this.fileByteContext=new byte[cl];
			s.readFully(fileByteContext);
			Path path = Paths.get(this.FolderName+"/"+query);
			Files.write(path, fileByteContext); //creates, overwrites
			return true;
		}
		return false;
	}
	boolean ParseFileReply(DataInputStream s,String query,int notf) throws IOException
	{
		System.out.println("ParseFileReply(DataInputStream s,String query,int notf)");

		//Same as above except instead of saving pass it into previous requester in filerequest.java
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		s.readLine();
		String reply= s.readLine();
		if(reply.equals("YES")){
			String res=s.readLine();
			res=s.readLine();
			res=s.readLine();
			res=s.readLine();
			res=s.readLine();

			int cl=Integer.parseInt(s.readLine().split(" ")[1]);//content-length
			res=s.readLine();


			this.fileByteContext=new byte[cl];
			s.readFully(fileByteContext);

			return true;
		}
		return false;
	}


	void SendQueryData(Socket s,ArrayList<String> Data)
	{
		System.out.println("SendQueryData(Socket s,ArrayList<String> Data)");

		//Sends data for querying
		try
		{
			int Size=0;
			for(String file:Data)
			{
				Size+=file.length();
			}
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			out.println("HTTP/1.0 200 OK");
			out.println("Allow: GET");
			out.println("MIME-Version: 1.0");
			out.println("Server: CS328 Basic HTTP Server");
			out.println("Content-Type:Text");
			out.println("Content-Length:"+Size);
			out.println();
			out.println(Data.size());	
			for(String file:Data)
			{
				out.println(file);
			}
			out.close();
			s.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
