package com.axiomatics.gmail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;

public class ComposeEmail {
	
	 /** Application name. */
    private static final String APPLICATION_NAME =
        "Gmail API";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/myCompose");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this Gmail API.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials
     */
    private static final List<String> SCOPES =
        Arrays.asList(GmailScopes.MAIL_GOOGLE_COM);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize(String pathCredentialFile) throws IOException {
        
    	// Load client secrets.
    	InputStream in = new FileInputStream(pathCredentialFile);
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Gmail client service.
     * @return an authorized Gmail client service
     * @throws IOException
     */
    public static Gmail getGmailService(String pathCredentialFile) throws IOException {
        Credential credential = authorize(pathCredentialFile);
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static MimeMessage createEmail(String to, String from, String subject, String bodyText)
			throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		
		MimeMessage email = new MimeMessage(session);
		
		email.setFrom(new InternetAddress(from));
		email.addRecipient(javax.mail.Message.RecipientType.TO,
		new InternetAddress(to));
		email.setSubject(subject);
		email.setText(bodyText);
		return email;
		}
    
    public static MimeMessage createHTMLEmail(String to, String from, String subject/*, String text*/, String html) throws AddressException, MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        Multipart multiPart = new MimeMultipart("alternative");
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);

        //MimeBodyPart textPart = new MimeBodyPart();
        //textPart.setText(text, "utf-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=utf-8");

        //multiPart.addBodyPart(textPart);
        multiPart.addBodyPart(htmlPart);
        email.setContent(multiPart);
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
    
    public static MimeMessage createEmailWithAttachment(String to,
            String from,
            String subject,
            String bodyText,
            File file)
		throws MessagingException, IOException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		
		MimeMessage email = new MimeMessage(session);
		
		email.setFrom(new InternetAddress(from));
		email.addRecipient(javax.mail.Message.RecipientType.TO,
		new InternetAddress(to));
		email.setSubject(subject);
		
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(bodyText, "text/plain");
		
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(mimeBodyPart);
		
		mimeBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(file);
		
		mimeBodyPart.setDataHandler(new DataHandler(source));
		mimeBodyPart.setFileName(file.getName());
		
		multipart.addBodyPart(mimeBodyPart);
		email.setContent(multipart);
		
		return email;
		}


    public static Message sendMessage(Gmail service, String userId, MimeMessage emailContent)
			throws MessagingException, IOException {
		Message message = createMessageWithEmail(emailContent);
		message = service.users().messages().send(userId, message).execute();
		
		//System.out.println("Message id: " + message.getId());
		//System.out.println(message.toPrettyString());
		return message;
		}
    
    public static String listMessagesMatchingQuery(Gmail service, String userId,
  	      String query) throws IOException {
  	    ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

  	    List<Message> messages = new ArrayList<Message>();
  	    while (response.getMessages() != null) {
  	      messages.addAll(response.getMessages());
  	      if (response.getNextPageToken() != null) {
  	        String pageToken = response.getNextPageToken();
  	        response = service.users().messages().list(userId).setQ(query)
  	            .setPageToken(pageToken).execute();
  	      } else {
  	        break;
  	      }
  	    }

  	    String messageId = messages.get(0).getId();
  	     	    
  	    return messageId;
  	  }

    public static void getAttachments(Gmail service, String userId, String messageId, String pathLocationDirectory)
    	      throws IOException {
	    Message message = service.users().messages().get(userId, messageId).execute();
	    List<MessagePart> parts = message.getPayload().getParts();
	    for (MessagePart part : parts) {
	      if (part.getFilename() != null && part.getFilename().length() > 0) {
	        String filename = part.getFilename();
	        String attId = part.getBody().getAttachmentId();
	        MessagePartBody attachPart = service.users().messages().attachments()
	        		.get(userId, messageId, attId).execute();

	        //String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
	        //Base64 base64Url = new Base64(true);
	        //byte[] fileByteArray = base64url.decodeBase64(attachPart.getData());
	        byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
	        FileOutputStream fileOutFile =
	            //new FileOutputStream("directory_to_store_attachments" + filename);
	        	new FileOutputStream(pathLocationDirectory + filename);
	        fileOutFile.write(fileByteArray);
	        fileOutFile.close();
	      }
	    }
	  }

    
	public static void main(String[] args) throws IOException, MessagingException {
		
		String 	pathCredentialFile 	= "client_secret.json";
		String 	to					= "philippeferron@hotmail.com";
		String 	from				= "philippe.ferron@axiomatics.com";
		String 	subject				= "Test- Compose an email";
		String 	bodyText			= "Bonjour, Je m'apelle Philippe";
		String 	userId				= "me";
		File	file 				= new File("./logs/DiscrepancesContactLists_20170623.log");
    	
        Gmail service = getGmailService(pathCredentialFile);
		//MimeMessage email = createEmail(to, from, subject, bodyText);
		MimeMessage email = createEmailWithAttachment(to, from, subject, bodyText, file);
        //String 	messageId = listMessagesMatchingQuery(service, userId, "subject: Active users with portal access");
        String messageId = listMessagesMatchingQuery(service, userId, "subject: TS Active Customers List with Portal Access on");
        getAttachments(service, userId, messageId, "");
		sendMessage(service, userId, email);
		System.out.println("Email being sent !!!");
	}

}
