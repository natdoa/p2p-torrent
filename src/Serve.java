import java.net.*;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;

public class Serve extends Frame implements Runnable {
	public static ServerPart Data=new ServerPart();
	Serve() {
		System.out.println("Serve()");

		setTitle("Server Program");
		Panel p = new Panel();
		portInput = new TextField(String.valueOf(port),4);
		startButton = new Button("Start Server");
		stopButton = new Button("Stop Server");

		//FolderName
		p.add(new Label("FilesFolder:"));
		final TextField FileFilder=new TextField(15);
		FileFilder.setText("wwwroot");//default text for testing purposes, this is changeable
		p.add(FileFilder);

		p.add(new Label("Port: "));
		p.add(portInput);
		p.add(startButton);
		//p.add(stopButton);
		//For Connection to Server
		p.add(new Label("ServerIP:"));
		final TextField ServerIP=new TextField(15);
		ServerIP.setText("127.0.0.1");//default text for testing purposes, this is changeable
		p.add(ServerIP);
		p.add(new Label("ServerPort:"));
		final TextField ServerPort=new TextField(4);
		ServerPort.setText("8081");//default text for testing purposes, this is changeable
		p.add(ServerPort);
		final Button ConnectButton=new Button("Connect");
		ConnectButton.setEnabled(false);
		p.add(ConnectButton);
		//QueryButton
		final TextField QueryField=new TextField(15);
		p.add(QueryField);
		final Button QueryButton=new Button("Query");
		p.add(QueryButton);


		stopButton.addActionListener (new ActionListener()
		{
			public void actionPerformed(ActionEvent e){
				stopServer();
			}
		});

		startButton.addActionListener (new ActionListener()
		{
			public void actionPerformed(ActionEvent e){
				System.out.println("startButton.addActionListener");

				//Set Up Variables for ServerPart
				Serve.Data.SetFolderName(FileFilder.getText());
				Serve.Data.ServerFiles=Serve.Data.GetFileNames();
				FileFilder.setEditable(false);
				startButton.setEnabled(false);
				Serve.Data.SetMyPort(Integer.parseInt(portInput.getText()));
				ConnectButton.setEnabled(true);
				startServer();
			}
		});

		ConnectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				System.out.println("ConnectButton.addActionListener");

				//Add To Connected List
				Serve.Data.Connect(ServerIP.getText(), Integer.parseInt(ServerPort.getText()));
				ServerIP.setEditable(false);
				ServerPort.setEditable(false);
				ConnectButton.setEnabled(false);
			}
		});

		QueryButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				System.out.println("QueryButton.addActionListener");

				display("Query Started\n");//just for displaying the incoming data
				ArrayList<String> ReplyData=Serve.Data.Request(QueryField.getText(), "3");
				for(int i=0;i<ReplyData.size();++i)
				{
					display(ReplyData.get(i)+"\n");
				}
				display("Query Finished\n");
			}
		});

		add("North",p);

		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				System.out.println("addWindowListener");

				dispose();
				System.exit(0);
			}
		});

		a = new TextArea(50,30);
		add("Center",a);
		resize(1200,300);
		show();
	}

	public void run() {
		System.out.println("serve run()");

		port=Integer.parseInt(portInput.getText());
		display("Server Started on Port: " + port + "\n");

		try {
			ss = new ServerSocket(port);//start listening
			while(true) {
				//display("Listening again ** port: " + port + "\n");
				Socket s = ss.accept();//accept incoming connections
				new Thread(new FileRequest(s,this)).start();
				//  display("Got a file request... \n");
			}

		}  catch (Exception e) { display ("$$$$ Exception " + e + "\n");}
	}

	public void startServer(){
		System.out.println("startServer()");


		if(runner==null) {
			runner = new Thread(this);//multi thread
			runner.start();
			portInput.setEditable(false);
		} else {
			runner.resume();
			display("Server resuming..\n");
		}
	}

	public void stopServer(){
		System.out.println("stopServer()");

		if(runner != null) {
			runner.suspend();
			display("Server Stopped by User... \n");
			portInput.setEditable(true);
		}
	}
	public synchronized void display(String text) {

		a.appendText(text);
	}

	public static void main(String[] args) {
		System.out.println("main(String[] args)");

		new Serve();
	}

	private ServerSocket ss;
	private TextArea a;
	private int port = 8080;
	private TextField portInput;
	private Button startButton, stopButton;
	private Thread runner=null;
}