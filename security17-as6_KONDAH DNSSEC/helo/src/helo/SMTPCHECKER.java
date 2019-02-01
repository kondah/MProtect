package helo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.server.CommandHandler;
import org.subethamail.smtp.server.SMTPServer;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import helo.MyMessageHandlerFactory.Handler;

public class SMTPCHECKER  {

	public static void main(String[] args) throws IOException, MessagingException {
		// TODO Auto-generated method stub
	MyMessageHandlerFactory myFactory = new MyMessageHandlerFactory() ;
		
		System.out.println("Gateway Listenning");
		SMTPServer smtpServer = new SMTPServer(myFactory);
		smtpServer.setPort(25);
		smtpServer.start();
	}
		

		
	
}
