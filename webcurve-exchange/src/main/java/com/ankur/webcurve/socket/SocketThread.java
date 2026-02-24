package com.ankur.webcurve.socket;

import lombok.extern.slf4j.Slf4j;

import java.net.Socket;
@Slf4j
public class SocketThread extends Thread implements Runnable {
	protected SocketHandler handler;
	protected Socket socket;
	
	public SocketThread(Socket socket, SocketHandler handler)
	{
		this.handler = handler;
		this.socket = socket;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try
		{
			while ( true )
			{
				if (!socket.isConnected())
				{
					log.info("socket disconnected");
					break;
				}
				if ( !handler.handleSocket(socket))
				{
					log.info("server released client socket");
					socket.close();
					break;
				}

			}
		}
		catch (Exception ex)
		{
			log.warn(ex.toString());
		}

	}

}
