import java.io.*;
import java.net.*;
import java.util.*;

class FileRequest implements Runnable {
	//Constructor
	FileRequest(Socket s, Serve app) {
		System.out.println("FileRequest(Socket s, Serve app)");
		this.app=app;
		client = s;
	}
	//multi threaded system
	public void run() {
		System.out.println("file request run()");

		if(requestRead()) {
			if(fileOpened()) {
				constructHeader();
				if(fileSent()) {
					//app.display("*File: " + fileName + "File Transfer Complete * Bytes Sent: " + bytesSent + "\n");
				}
			}
		}
		try {
			dis.close();
			client.close();
		} catch (Exception e) { /*app.display ("Error closing Socket \n" + e);*/ }
	}

	//send the file 
	private boolean fileSent()
	{
		System.out.println("fileSent()");

		try {
			DataOutputStream clientStream = new DataOutputStream (new BufferedOutputStream (client.getOutputStream()));
			clientStream.writeBytes(header);
			//app.display("******** File Request ******** \n" + "******** " + fileName + "******** \n" +header);
			int i;
			bytesSent = 0;
			while ((i=requestedFile.read()) != -1) {
				clientStream.writeByte(i);
				bytesSent++;
			}
			clientStream.flush();
			clientStream.close();

		} catch (IOException e) { return false; }
		return true;
	}

	//check if the requested file is opened, if there is no file, throw exception
	private boolean fileOpened() {
		System.out.println("fileOpened()");

		try {
			requestedFile = new DataInputStream(new BufferedInputStream(new FileInputStream(Serve.Data.FolderName+"/" + fileName)));
			fileLength = requestedFile.available();
			//app.display(fileName + "is: " + fileLength + "bytes long... \n");
		} catch (FileNotFoundException e) {

			if(fileName.equals("filenfound.html")) { return false; }
			fileName="filenfound.html";
			if(!fileOpened()) { return false; }
		} catch (Exception e) { return false; }

		return true;
	}

	//handle the request
	private boolean requestRead() {
		System.out.println("requestRead()");

		try {
			//Open InputStream and read(parse) the request
			dis = new DataInputStream(client.getInputStream());
			String line;
			while ((line=dis.readLine()) != null) {
				//Request For Client Connection. this line comes from header
				if(line.startsWith("ACTION=Connect"))
				{
					//ip and port are added to the local database
					Serve.Data.AddConnection(client.getInetAddress().toString(), Integer.parseInt(line.split("&")[1].split("=")[1]));
					app.display("ClientConnected\n");
					return false;
				}
				//Request For Search Query. this line comes from header
				else if(line.startsWith("QUERY="))
				{
					String ttl=line.split("&")[1].split("=")[1];//parse ttl counter
					String Query=line.split("&")[0].split("=")[1];//parse the query
					int Port=Integer.parseInt(line.split("&")[2].split("=")[1]);//parse the port

					ArrayList<String>Files=new ArrayList<String>();
					if(!ttl.equals("0"))//if ttl is not 0, handle the request, if not, don't do anything
					{
						Integer Ttl=Integer.parseInt(ttl);
						Ttl=Ttl-1;
						Files=Serve.Data.Request(Query, Ttl.toString(),client.getInetAddress().toString().substring(1),Port);
					}
					Files.addAll(Serve.Data.QueryFileNames(Query));
					Serve.Data.SendQueryData(client, Files);
					return false;
				}
				//Request For File Download. this line comes from header
				else if(line.startsWith("FILE="))
				{

					String ttl=line.split("&")[1].split("=")[1];//parse ttl counter
					String Query=line.split("&")[0].split("=")[1];//parse the query
					int Port=Integer.parseInt(line.split("&")[2].split("=")[1]);//parse the port
					if(Serve.Data.CheckFile(Query))
					{
						//File Found On Server, prepare the new header and send
						PrintWriter out = new PrintWriter(client.getOutputStream(), true);
						out.println("POST / HTTP/1.0");
						out.println("Connection: Keep-Alive");
						out.println("User-Agent: CS328-Servant");
						out.println("Accept-Language: en");
						out.println("Content-type: application/x-www-form-urlencoded");
						out.println("Content-length:"+3);
						out.println("");
						out.println("YES");//"file is in the server" indicator
						out.flush();
						fileName= Query;
						return true;
					}

					if(!ttl.equals("0"))
					{
						//Check Other Connections for file until TTL = 0
						Integer Ttl=Integer.parseInt(ttl);
						Ttl=Ttl-1;
						if(Serve.Data.RequestFile(Query, Ttl.toString(),client.getInetAddress().toString().substring(1),Port))
						{
							//Notify Requester about file is coming, this prepares the header
							PrintWriter out = new PrintWriter(client.getOutputStream(), true);
							out.println("POST / HTTP/1.0");
							out.println("Connection: Keep-Alive");
							out.println("User-Agent: CS328-Servant");
							out.println("Accept-Language: en");
							out.println("Content-type: application/x-www-form-urlencoded");
							out.println("Content-length:"+3);
							out.println("");
							out.println("YES");//"file is in the server" indicator
							out.flush();
							//Send read file
							out.println("HTTP/1.0 200 OK");
							out.println("Allow: GET");
							out.println("MIME-Version: 1.0");
							out.println("Server: CS328 Basic HTTP Server");
							out.println("Content-Type:text");
							out.println("Content-length: "+Serve.Data.fileByteContext.length);//write how many bytes the requested side needs
							out.println("");

							client.getOutputStream().write(Serve.Data.fileByteContext);//send file by bytes
							client.getOutputStream().flush();
							out.flush();
							return false;
						}
					}
					//File Not Found, still prepare and send the header
					PrintWriter out = new PrintWriter(client.getOutputStream(), true);
					out.println("POST / HTTP/1.0");
					out.println("Connection: Keep-Alive");
					out.println("User-Agent: CS328-Servant");
					out.println("Accept-Language: en");
					out.println("Content-type: application/x-www-form-urlencoded");
					out.println("Content-length:"+2);
					out.println("");
					out.println("NO");//"file is not in the server" indicator
					out.flush();
					return false;
				}
				else
				{
					//SendFile
					StringTokenizer tokenizer = new StringTokenizer(line, " ");
					if (!tokenizer.hasMoreTokens()) { continue; }
					if (tokenizer.nextToken().equals("GET")) {
						fileName = tokenizer.nextToken();
						if(fileName.equals("/")) {
							fileName = "index.html";
						} else {
							fileName = fileName.substring(1);
						}
						break;
					}
				}
			}

		}catch (Exception e) {
			return false;
		}
		app.display("Finished file request...\n");
		return true;
	}

	//check file type
	private void constructHeader() {
		System.out.println("constructHeader()");

		String contentType;

		if((fileName.toLowerCase().endsWith(".jpg"))||(fileName.toLowerCase().endsWith(".jpeg"))||(fileName.toLowerCase().endsWith(".jpe")))
		{ contentType = "image/jpg"; }
		else if((fileName.toLowerCase().endsWith(".gif")))
		{ contentType = "image/gif"; }
		else if((fileName.toLowerCase().endsWith(".htm"))||(fileName.toLowerCase().endsWith(".html")))
		{ contentType = "text/html"; }
		else if((fileName.toLowerCase().endsWith(".qt"))||(fileName.toLowerCase().endsWith(".mov")))
		{ contentType = "video/quicktime"; }
		else if((fileName.toLowerCase().endsWith(".class")))
		{ contentType = "application/octet-stream"; }
		else if((fileName.toLowerCase().endsWith(".mpg"))||(fileName.toLowerCase().endsWith(".mpeg"))||(fileName.toLowerCase().endsWith(".mpe")))
		{ contentType = "video/mpeg"; }
		else if((fileName.toLowerCase().endsWith(".au"))||(fileName.toLowerCase().endsWith(".snd")))
		{ contentType = "audio/basic"; }
		else if ((fileName.toLowerCase().endsWith(".wav")))
		{ contentType = "audio/x-wave"; }
		else
		{ contentType = "text/plain"; } //default

		header = "HTTP/1.0 200 OK\n" + "Allow: GET\n" +
				"MIME-Version: 1.0\n" + "Server: HMJ Basic HTTP Server\n" +
				"Content-Type: " + contentType + "\n" + "Content-Length: " +
				fileLength + "\n\n";
	}

	private Serve app;
	private Socket client;
	private String fileName, header;
	private DataInputStream requestedFile, dis;
	private int fileLength, bytesSent;

}










