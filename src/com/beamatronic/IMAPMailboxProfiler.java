package com.beamatronic;

//created     January 4, 2012
//refreshed    April 23, 2015
//refreshed   August 26, 2016
//updated   December 28, 2016
//refactored January  4, 2017

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import com.sun.mail.imap.IMAPMessage;

public class IMAPMailboxProfiler {

	static int SCREENCOLUMNS = 0;

	public static void main(String args[]) {

		final String propertiesFileName = "IMAPMailboxProfiler.properties";

		// Try to read the properties file before we do anything else
		Properties props = getPropertiesFromFile(propertiesFileName);

		// Magic for later
		props.setProperty("mail.store.protocol", "imaps");

		SCREENCOLUMNS              = Integer.parseInt(props.getProperty("screencolumns"));
		String emailAddress        = props.getProperty("email");
		String imapPasswordOrToken = props.getProperty("password");
		String imapServer          = props.getProperty("imapserver");
		String inboxFolderName     = props.getProperty("inboxfoldername");
		int sendersToShow          = Integer.parseInt(props.getProperty("sendersToShow"));
		int exceptionsToShow       = Integer.parseInt(props.getProperty("exceptionsToShow"));

		printCenteredBanner("Starting up...");

		// These objects will hold the results
		TopStrings topMessageSenders = new TopStrings();
		TopStrings topExceptions     = new TopStrings();

		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imaps");
			store.connect(imapServer, emailAddress, imapPasswordOrToken);
			Folder inbox = store.getFolder(inboxFolderName);

			inbox.open(Folder.READ_ONLY);

			int inboxMessageCount = inbox.getMessageCount();
			double approxTimeSeconds = 0;
			double timeMultiplier = 1.439; // milliseconds per message, approximately

			if (inboxMessageCount != 0) {
				approxTimeSeconds = ((inboxMessageCount * timeMultiplier) / 1000);
			}

			printCenteredBanner(inboxFolderName + " contains " + inboxMessageCount + " messages, this will take about " + (int) approxTimeSeconds + " seconds.");

			Message messages[] = inbox.getMessages();
			int totalMessages = messages.length;

			// set up a FetchProfile
			FetchProfile fetchProfile = new FetchProfile();
			fetchProfile.add(FetchProfile.Item.ENVELOPE);

			printCenteredBanner("Fetching " + totalMessages + " total messages...");
			long t1 = System.currentTimeMillis();
			inbox.fetch(messages, fetchProfile);
			long t2 = System.currentTimeMillis();
			printCenteredBanner("Fetching compete.  Time taken: " + (t2 - t1) + " ms");

			IMAPMessage eachImapMessage = null;

			int numberProcessed = 0;
			int success         = 0;
			int exceptions      = 0;

			long startTime = System.currentTimeMillis();

			int progressReportFrequency = 100000;  // Set to a lower number for frequent reports if you need to see them

			String senderToString = "";

			for (Message eachMessage:messages) {
				try {
					eachImapMessage = (IMAPMessage) eachMessage;
					Address eachSender = eachImapMessage.getSender();  // Shapeways emails throw NullPointerException
					if (eachSender == null) {
						senderToString = "NULL SENDER";
					}
					else {
						senderToString = eachSender.toString();
					}
					topMessageSenders.processString(senderToString);
					success++;
				}
				catch(Exception e) {
					// Get some info about the message

					Enumeration<?> allHeaderLines = eachImapMessage.getAllHeaderLines();
					Enumeration<?> allHeaders = eachImapMessage.getAllHeaders();

					String msgEncoding = eachImapMessage.getEncoding();
					int msgSize = eachImapMessage.getSize();
					String msgSubject = eachImapMessage.getSubject();

					System.out.println("sub:    " + msgSubject);
					System.out.println("size:   " + msgSize);
					System.out.println("enc:    " + msgEncoding);
					System.out.println("all h:  " + allHeaders);
					System.out.println("all hl: " + allHeaderLines);

					e.printStackTrace();
					exceptions++;
					topExceptions.processString(e.toString());
					System.out.println();
				}

				if (numberProcessed > 0) {
					// Give a status report
					if ((numberProcessed % progressReportFrequency) == 0) { 
						int avgTimePerMessage = 0;
						long timeSpentSoFar = 0;
						if (numberProcessed != 0) {
							long timeNow = System.currentTimeMillis();
							timeSpentSoFar = timeNow - startTime;
							avgTimePerMessage = (int) (timeSpentSoFar / numberProcessed);
						}
						int messagesRemaining = totalMessages - numberProcessed;
						long timeRemaining = messagesRemaining * avgTimePerMessage;
						System.out.println(timeSpentSoFar + " spent so far. " + numberProcessed + " processed.  Avg time / message is " 
								+ avgTimePerMessage + " ms.  Remaining:" + messagesRemaining + " ETA: " + timeRemaining + " Exceptions: " + exceptions); 
					}

					// Display the current top 10 list
					if ((numberProcessed % progressReportFrequency) == 0) { 
						topMessageSenders.displayTopStrings(10);
					}
				}

				numberProcessed++;
			} // for each message

			System.out.println("Total: " + numberProcessed + 
					" Success: " + success + 
					" Excp: " + exceptions + 
					" Total Strings Processed: " + topMessageSenders.totalStringsProcessed());

			// Show the top senders
			topMessageSenders.displayTopStrings(sendersToShow);

			System.out.println();

			// Show the top exceptions, if any
			if (topExceptions.totalStringsProcessed() > 0) {
				topExceptions.displayTopStrings(exceptionsToShow);
			}

		} catch (javax.mail.AuthenticationFailedException e1) {
			System.out.println("Please check to see that you updated your credentials in the properties file.");
			System.out.println("I see that your user is currently configured as: " + emailAddress);
			System.out.println("For some email providers, if your email address is foo@bar.com then you should use just \"foo\" only (without the quotes)");
			System.out.println("Also, you may need an \"app-specific access token\" instead of your actual password.");
			// e1.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
			System.exit(1);
		}

		System.out.println();

		printCenteredBanner("All done.  Goodbye.");

	} // main()


	static Properties getPropertiesFromFile(String propertiesFilename) {
		File propertiesFile = new File(propertiesFilename);
		FileInputStream fileInput = null;
		try {
			fileInput = new FileInputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		Properties propertiesObject = new Properties();
		try {
			propertiesObject.load(fileInput);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return propertiesObject;

	}

	public static void printDecoration(int c, String s) {
		for (int i = 0; i < c; i++) { System.out.print(s); }
	}

	public static void printCenteredBanner(String s) {
		int numDecorations = ((SCREENCOLUMNS - (s.length() + 2)) / 2);
		printDecoration(numDecorations,"=");
		System.out.print(" " + s + " ");
		printDecoration(numDecorations,"=");		
		System.out.println();
	}

} // IMAPMailboxProfiler class

// EOF