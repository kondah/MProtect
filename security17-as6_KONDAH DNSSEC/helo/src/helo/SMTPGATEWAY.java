package helo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
public class SMTPGATEWAY {

	

		    private final int listenOnPort = 3000;
		    private String badWords = "C:\\Users\\SAID\\Desktop\\words.txt";
		    private String SMTPServer;
		    private int SMTPport;


		

		    public static void main(String[] args) {
		    	SMTPGATEWAY p = new SMTPGATEWAY();
		        p.intercept();
		    }
		   
		  
		    
		    private List loadWords() throws IOException{
		    	FileReader fr = new FileReader(badWords);
				BufferedReader texReader = new BufferedReader(fr);
				
				int n =nbrligne();
				List<String> textData =new LinkedList();
				int i;
				for( i=0;i<n;i++){
					textData.add(texReader.readLine());
				}
				texReader.close();
				return textData;
			}
		    
		    public  int nbrligne() throws IOException{
				FileReader fr = new FileReader(badWords);
				BufferedReader texReader = new BufferedReader(fr);
				String line;
				int n=0;
			      while ((line = texReader.readLine()) != null) {
			    	  n++;
			      }
				return n;
			}
			
		    
		    
		        
		      
		  
		   
		    private String filters(final List badwords, final String text) {
		        String replaced = text;
		        Iterator<String> i = badwords.iterator();
				while (i.hasNext()) {
					replaced= replaced.replaceAll(i.next(), "[redacted]");
				}
		 
		        
		        
		        if (!replaced.equals(text)) {
		            System.out.println("filter in use");
		            System.out.println("original: " + text);
		            System.out.println("replaced: " + replaced);
		        }
		        
		        return replaced;
		    }
		    
		  
		    private void disconnectClient(Socket client, Socket toSMTPServer) {
		        try {
		            client.close();
		        } catch (IOException ex) {}
		        
		        try {
		            toSMTPServer.close();
		        } catch (IOException ex) {}
		    }
		    
		    private void intercept() {
		        List badwords;
		        
		        try {
		        	System.out.println("---------SMTP GATEWAY--------");

		        	System.out.println("enter the smtp server to relay emails to");
		        	Scanner scan = new Scanner(System.in);
		        	this.SMTPServer=scan.next();
		        	System.out.println("enter the port ");
		        	this.SMTPport=Integer.parseInt(scan.next());
		        	System.out.println("SMTP SERVER:"+SMTPServer+",port:"+SMTPport);
		        	System.out.println("do you want to use the file of forbidden words by defaults(yes or no)");
		        	String response = scan.next();
		        	System.out.println(""+response);
		        	  switch (response) {
		              case "no":   System.out.println("enter the path of the file");
			        	String path = scan.next();
			        	this.badWords=path;
		              case "yes": System.out.println("OK");
		                       break;
		        	  }


		            badwords = loadWords();

		        } catch (IOException e) {
		            e.printStackTrace();
		            return;
		        }
		        
		        ServerSocket srv;
		                
		        try {
		            srv = new ServerSocket(listenOnPort);
		        } catch (IOException ex) {
		            ex.printStackTrace();
		            return;
		        }
		        
		        while (true) {
		            // Waiting for a new SMTP client to connect
		            
		            Socket client;
		            
		            try {
		                client = srv.accept();
		            } catch (IOException ex) {
		                ex.printStackTrace();
		                continue;
		            }
		            
		            
		            System.out.println("client connected");
		            
		            PrintWriter clientW;
		            BufferedReader clientR;
		            
		            try {
		                clientW = new PrintWriter(client.getOutputStream(), true);
		                clientR = new BufferedReader(new InputStreamReader(client.getInputStream()));
		            } catch (IOException ex) {
		                System.err.println("eroor occured");
		                continue;
		            }

		            Thread readFromClient = new Thread() {
		                Socket socketsmtp;
		                File file;
		                FileWriter writer;
		                PrintWriter serverWrite;
		                BufferedReader serverRead;
		                boolean BodyArea = false;
		                
		                boolean isVirus=false;
		                
		                @Override
		                public void run() {
		                    
		                    try {
		                        socketsmtp = new Socket(SMTPServer, SMTPport);
		                    } catch (IOException ex) {
		                        System.err.println("Error: could not connect to local SMTP server");
		                    }

		                    
		                    try {
		                        serverWrite = new PrintWriter(socketsmtp.getOutputStream(), true);
		                        serverRead = new BufferedReader(new InputStreamReader(socketsmtp.getInputStream()));
		                        file = new File("C:\\Users\\SAID\\Desktop\\scan.txt");
		                        writer = new FileWriter(file);
		                    } catch (IOException ex) {
		                    	ex.printStackTrace();
		                        return;
		                    }

		                    Thread readFromServer = new Thread() {
		                        @Override
		                        public void run() {
		                            String currentLineFromServer;

		                            while (true) {
		                                try {
		                                    currentLineFromServer = serverRead.readLine();
		                                } catch (IOException ex) {
		                                    disconnectClient(client, socketsmtp);
		                                    return;
		                                }

		                                if (currentLineFromServer.startsWith("221 ")) { 
		                                    clientW.println(currentLineFromServer);

		                                    disconnectClient(client, socketsmtp);
		                                    return;
		                                }

		                                clientW.println(currentLineFromServer);
		                            }
		                        }
		                    };

		                    readFromServer.start();
		                    
		                    String currentLine;
		                    String currentLineLowercase;
		                    String from = "";
		                    String to = "";
		                            
		                    while (true) {
		                        try {
		                            currentLine = clientR.readLine();
		                        } catch (IOException ex) {
		                            disconnectClient(client, socketsmtp);
		                            return;
		                        }
		                        
		                        if (BodyArea) {
		                            if (currentLine.equals(".")) {
		                            	try {
											writer.close();
											String res = Scan(file.getPath());
											String[] data = res.split("\\s+");
											
											System.out.println(""+res);

											if(res.indexOf("FOUND")!= -1) {
												isVirus=true;
												System.out.println("sending email");
												System.out.println(""+from);
												System.out.println(""+to);

												System.out.println(""+SendMail(from,to,data[1]));
												clientW.println("250 ok");
												System.out.println(""+clientR.readLine());
												clientW.println("221 see you later");
					                            disconnectClient(client, socketsmtp);


												
											}
										} catch (IOException | MessagingException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
		                                BodyArea = false;
		                            } else {
		                            	try {
											writer.write(currentLine);
											writer.flush();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
		                                currentLine = filters(badwords, currentLine);
		                            }
		                        } else {
		                            currentLineLowercase = currentLine.toLowerCase();
		                            if(currentLine.indexOf("from")!=-1) {
		                            	String[] s =currentLine.split("<");
		                            	String[] f= s[1].split(">");
		                            	from= f[0];
		                            	
		                            }
		                            	if(currentLine.indexOf("rcpt")!=-1) {
		                            		String[] s =currentLine.split("<");
			                            	String[] f= s[1].split(">");
			                            	to= f[0];
			                            	
		                            	
		                            }
		                            if (currentLineLowercase.equals("data")) { 
		                                BodyArea = true;
		                            } else if (currentLineLowercase.startsWith("subject: ")) { 
		                                String subject = currentLine.substring(9);

		                                currentLine = "Subject: " + filters(badwords, subject);
		                            }
		                        }
		                        
		                       if(isVirus==false) serverWrite.println(currentLine);
		                    }
		                }
		            };

		            readFromClient.start();
		        }
		    }
			public static String Scan(String FilePath) throws IOException {
				String[] commands = new String[4];
				commands[0]= "C:\\Program Files (x86)\\ClamWin\\bin\\clamscan.exe";
				commands[1]= "-d";
				commands[2]="C:\\ProgramData\\.clamwin\\db";
				commands[3]=FilePath;
				Process p;
				p = Runtime.getRuntime().exec(commands);
				BufferedReader reader =
		                new BufferedReader(new InputStreamReader(p.getInputStream()));
		        String res = "nothing";

		            String line = "";
		while ((line = reader.readLine())!= null) {
			if(line.indexOf("FOUND")!= -1) res=line;
		}	
		return res;

				
			}
			public String SendMail(String from, String to,String m) throws AddressException, MessagingException {
				 String username = "mouadyas13";
				 String password = "yasmine123++M";

				Properties props = new Properties();
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "smtp.sendgrid.net");
				props.put("mail.smtp.port", "587");

				Session session = Session.getInstance(props,
				  new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				  });
				System.out.println("Done");		// TODO Auto-generated method stub


				Message message = new MimeMessage(session);

					message.setFrom(new InternetAddress(from));
					message.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse(to));
					message.setSubject("[virus found]");
					message.setText("helo our smtp gateway has found a virus getting from the sender above"
							+"\n virus found:"+m);

					Transport.send(message);

					return "done";	
				
				
			}
		}