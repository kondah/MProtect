package helo;

import org.jitsi.dnssec.validator.ValidatingResolver;
import org.subethamail.smtp.*;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TLSARecord;
import org.xbill.DNS.Type;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.xml.bind.DatatypeConverter;

public class MyMessageHandlerFactory implements MessageHandlerFactory {
	private MimeMessage me;

	public MessageHandler create(MessageContext ctx) {
		return new Handler(ctx);
	}

	class Handler implements MessageHandler {
		MessageContext ctx;
		private List badwords;
		private javax.mail.Message message;
		private String Domain;
		private String from;
		private String recipient;
		private DataOutputStream dos;
		private BufferedReader out = null;
		private String badWords = "C:\\Users\\SAID\\Desktop\\words.txt";
		private String ROOT = ". IN DS 19036 8 2 49AAC11D7B6F6446702E54A1607371607A1A41855200FD2CE1CDDE32F24E8FB5";

		public Handler(MessageContext ctx) {
			this.ctx = ctx;
		}

		public void from(String from) throws RejectException {
			System.out.println("FROM:" + from);
			this.from = from;

		}

		public void recipient(String recipient) throws RejectException {

			System.out.println("RECIPIENT:" + recipient);
			this.recipient = recipient;

		}

		public void data(InputStream data) throws IOException {
			System.out.println("MAIL DATA");
			boolean isOk = false;
			badwords = loadWords();
			System.out.println("list:" + badwords.toString());

			System.out.println("RECIPIENT:" + this.recipient);
			String[] d = this.recipient.split("@");
			this.Domain = d[1];
			System.out.println("domain:" + Domain);
			String MXRecord = null;
			String cert = null;

			String tlsacert = null;
			int port = 10587;
			SimpleResolver sr = null;
			try {
				sr = new SimpleResolver("4.2.2.1");
			} catch (UnknownHostException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}
			ValidatingResolver vr = new ValidatingResolver(sr);
			try {
				vr.loadTrustAnchors(new ByteArrayInputStream(ROOT.getBytes("ASCII")));
			} catch (UnsupportedEncodingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			System.out.println("\n\nValidating resolver:");
			String mx = null;
			try {
				mx = sendAndPrint(vr, this.Domain + ".");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (mx.indexOf("true") != -1) {
				System.out.println("DNSSEc VALIDATE true");
				System.out.println("parsing the mx record ");
				String[] s = mx.split("\\s+");
				MXRecord = s[0];
				System.out.println("" + MXRecord);
				System.out.println("starting connection with MXRecord");
				System.out.println("getting certificate");
				try {
					String a = validateArecord(vr, MXRecord);
					if (a.indexOf("false") != -1) {
						System.out.println("could not validarte a record exit..");
						System.exit(-1);
					}
					String[] se = a.split("\\s+");

					String ip = se[0];
					System.out.println("ip is " + ip);

					cert = GetCert(ip, port);
				} catch (KeyManagementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CertificateEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("certificate obtained:" + cert);
				String query = "_" + port + "._tcp." + MXRecord;
				System.out.println("getting tlsa record...");
				if (mx.indexOf("true") != -1) {
					String[] t = null;
					try {
						t = getTLSACert(vr, query).split("\\s+");
						tlsacert = t[0];

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					System.out.println("tlsa cert associated:" + tlsacert);
					if (tlsacert.equals(cert)) {
						System.out.println("TLSA record match the  certificate presented by the server. ");
						isOk = true;

					} else {
						isOk = false;

						System.out.println("TLSA record does not  match the  certificate presented by the server. ");
						System.out.println("not sending email");

					}
				}

			}

			else {

				System.out.println("cannot do a DNSSEC VALIDATION");

			}

			if (isOk == true) {

				Session s = Session.getInstance(new Properties());
				try {
					System.out.println(" sending email...");

					MimeMessage message = new MimeMessage(s, data);
					me = message;

					System.out.println("content" + filters(badwords, message.getContent().toString()));
					System.out.println(""
							+ SendEmail(this.from, this.recipient, filters(badwords, message.getContent().toString()),
									MXRecord, 10587, filters(badwords, message.getSubject())));
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private String validateArecord(Resolver vr, String name) throws IOException {
			System.out.println("\n---" + name);

			Record[] records = new Lookup("security2017.vaucher.org", Type.MX).run();

			Record qr = Record.newRecord(Name.fromConstantString(name), Type.A, DClass.IN);
			Message response = vr.send(Message.newQuery(qr));
			Record[] record = response.getSectionArray(1);
			ARecord arecord = (ARecord) record[0];
			String[] s = arecord.toString().split("\\s+");
			System.out.println("A record is " + s[4]);
			return "" + s[4] + " " + response.getHeader().getFlag(Flags.AD);

		}

		private String sendAndPrint(Resolver vr, String name) throws IOException {

			Record[] records = new Lookup("security2017.vaucher.org", Type.MX).run();

			Record qr = Record.newRecord(Name.fromConstantString(name), Type.MX, DClass.IN);
			Message response = vr.send(Message.newQuery(qr));
			Record[] record = response.getSectionArray(1);
			if (record.length == 0) {
				System.out.println("mxrecord do not exist quitting..");
				System.exit(-1);
			}
			MXRecord mx = (MXRecord) record[0];
			return mx.getAdditionalName().toString() + " " + response.getHeader().getFlag(Flags.AD);

		}

		public String SendEmail(String from, String to, String data, String mxrecord, int port, String Subject)
				throws UnsupportedEncodingException, IOException {
			Properties props = new Properties();
			SimpleResolver sr = new SimpleResolver("4.2.2.1");
			ValidatingResolver vr = new ValidatingResolver(sr);

			vr.loadTrustAnchors(new ByteArrayInputStream(ROOT.getBytes("ASCII")));
			System.out.println("validate record a" + validateArecord(vr, mxrecord));
			String a = validateArecord(vr, mxrecord);
			String[] s = a.split("\\s+");
			String ip = s[0];

			props.put("mail.smtp.auth", "false");
			props.put("mail.smtp.ssl.trust", ip);

			props.put("mail.smtp.starttls.required", "true");
			props.put("mail.smtp.host", ip);
			props.put("mail.smtp.port", port);

			Session session = Session.getDefaultInstance(props);

			try {

				javax.mail.Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(from));
				message.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(to));
				if (Subject != null) {
					message.setSubject(Subject);
				}
				if (data.length() != 0) {
					message.setText(data);
				}

				Transport.send(message);

				return "done";
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
		}

		private List loadWords() throws IOException {
			FileReader fr = new FileReader(badWords);
			BufferedReader texReader = new BufferedReader(fr);

			int n = nbrligne();
			List<String> textData = new LinkedList();
			int i;
			for (i = 0; i < n; i++) {
				textData.add(texReader.readLine());
			}
			texReader.close();
			return textData;
		}

		public int nbrligne() throws IOException {
			FileReader fr = new FileReader(badWords);
			BufferedReader texReader = new BufferedReader(fr);
			String line;
			int n = 0;
			while ((line = texReader.readLine()) != null) {
				n++;
			}
			return n;
		}

		private String filters(final List badwords, final String text) {
			String replaced = text;
			Iterator<String> i = badwords.iterator();
			while (i.hasNext()) {
				replaced = replaced.replaceAll(i.next(), "[redacted]");
			}

			return replaced;
		}

		public void done() {
			System.out.println("Finished");
		}

		public String GetCert(String host, int port) throws UnknownHostException, IOException, KeyManagementException,
				NoSuchAlgorithmException, CertificateEncodingException {
			Socket sock1 = new Socket(host, port);
			dos = new DataOutputStream(sock1.getOutputStream());
			out = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
			boolean a = false;
			PrintWriter clientW;
			clientW = new PrintWriter(sock1.getOutputStream(), true);
			System.out.println("" + out.readLine());
			clientW.println("ehlo mouad");
			System.out.println("" + out.readLine());
			System.out.println("" + out.readLine());
			System.out.println("" + out.readLine());
			System.out.println("" + out.readLine());
			System.out.println("" + out.readLine());
			System.out.println("" + out.readLine());
			System.out.println("" + out.readLine());
			clientW.println("starttls");
			System.out.println("" + out.readLine());
			int p = sock1.getPort();
			String h = sock1.getInetAddress().getHostAddress();
			TLSSocketFactory ssl = new TLSSocketFactory();
			SSLSocket s = (SSLSocket) ssl.createSocket(sock1, h, p, true);
			s.startHandshake();
			System.out.println("Handshaking Complete");
			SSLSession session = s.getSession();
			X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
			byte[] hex = cert.getEncoded();
			byte[] se = MessageDigest.getInstance("SHA-256").digest(hex);
			String t = DatatypeConverter.printHexBinary(se);
			return t;

		}

		private String getTLSACert(Resolver vr, String name) throws IOException {
			String cert;
			Record qr = Record.newRecord(Name.fromConstantString(name), Type.TLSA, DClass.IN);
			Message response = vr.send(Message.newQuery(qr));
			Record[] record = response.getSectionArray(1);
			TLSARecord tlsa = (TLSARecord) record[0];
			String[] s = tlsa.rdataToString().split("\\s+");
			String CertificateHash = s[3];
			return CertificateHash + " " + response.getHeader().getFlag(Flags.AD);

		}

	}
}