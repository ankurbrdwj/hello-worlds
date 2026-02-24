package com.ankur.webcurve.socket;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
@Slf4j
public class SocketServer implements Runnable {
	private int port = 3828;
	private int maxConnections = 100;
	private ServerSocket serverSocket;
	private Vector<SocketThread> threads = new Vector<SocketThread>();
	private Thread serverThread;
	private SocketHandler handler;
	
	public SocketServer(int port, SocketHandler handler)
	{
		this.port = port;
		this.handler = handler;
		serverThread = new Thread(this);
	}
	
	private void cleanThreadPool()
	{
		int i=0;
		while(i<threads.size())
		{
			Socket socket = threads.get(i).socket;
			if (socket.isClosed() || (!socket.isConnected()))
			{
				threads.remove(i);
				log.info("Removing a stale client socket, total " + threads.size() + " client sockets");
				continue;
			}
			i++;	
		}
	}
	
	public void run()
	{
		try
		{
			serverSocket = new ServerSocket(port);
		}
		catch (IOException ex) 
		{
		    log.error(ex.toString());
		    ex.printStackTrace();
		    return;
		}
		
		while((!serverSocket.isClosed()) && (threads.size() < maxConnections) || (maxConnections == 0))
		{
			//before accepting new connection, clear some stale sockets from the pool
			cleanThreadPool();
			
			Socket socket = null;
			try
			{
				socket = serverSocket.accept();
				socket.setKeepAlive(true);
			}
			catch (IOException ex) 
			{
			    log.warn(ex.toString());
			    ex.printStackTrace();
			}
			
			if (socket != null)
			{
				SocketThread thread = new SocketThread(socket, handler);
				threads.add(thread);
				log.info("Adding a client socket, total " + threads.size() + " client sockets now");
				thread.start();
			}
		}		
	}
	
	public void start()
	{
		serverThread.start();
	}
	
	public void stop()
	{
		try
		{
			serverSocket.close();
		}
		catch (IOException ex) 
		{
		    log.warn(ex.toString());
		    ex.printStackTrace();
		}
		//conThread.stop();
	}

}
