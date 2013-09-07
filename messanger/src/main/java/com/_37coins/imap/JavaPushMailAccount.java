package com._37coins.imap;

import java.util.EventListener;
import java.util.Properties;
import java.util.Vector;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

public class JavaPushMailAccount implements Runnable {

	public final static int READ_ONLY_FOLDER = Folder.READ_ONLY;
	public final static int READ_WRITE_FOLDER = Folder.READ_WRITE;
	private boolean connected = false;
	private String accountName;
	private String serverAddress;
	private String username;
	private String password;
	private int serverPort;
	private boolean useSSL;
	private boolean shutDown = false;
	private IMAPStore server;
	private Session session;
	private IMAPFolder folder;
	private MessageCountListener messageCountListener, externalCountListener;
	private MessageChangedListener messageChangedListener,
			externalChangedListener;
	private Thread pushThread;
	private Long lastDisconnect = System.currentTimeMillis();

	public JavaPushMailAccount(String accountName, String serverAddress,
			int serverPort, boolean useSSL) {
		this.accountName = accountName;
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.useSSL = useSSL;
	}

	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public void run() {
		this.initConnection();
	}

	public void connect() {
		try {
			server.connect(serverAddress, serverPort, username, password);
			selectFolder("");
			connected = true;
			System.out.println(accountName + " connected!");
		} catch (AuthenticationFailedException ex) {
			connected = false;
			System.out.println(accountName + ex.getMessage());
			onError(ex);
		} catch (MessagingException ex) {
			connected = false;
			folder = null;
			messageChangedListener = null;
			messageCountListener = null;
			System.out.println(accountName + ex.getMessage());
			onError(ex);
		} catch (IllegalStateException ex) {
			System.out.println(accountName + ex.getMessage());
			connected = true;
			onError(ex);
		}
	}

	public void disconnect() {
		if (!connected && server == null && !server.isConnected())
			return;

		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					pushThread.interrupt();
					closeFolder();
					server.close();
					connected = false;
					System.out.println(accountName + " disconnected!");
					shutDown = true;
					onDisconnect();
				} catch (Exception e) {
					System.out.println(e.getMessage());
					onError(e);
				}
			}
		});
		t.start();
	}

	public void setMessageChangedListerer(MessageChangedListener listener) {
		removeListener(externalChangedListener);
		externalChangedListener = listener;
		addListener(externalChangedListener);
	}

	public void setMessageCounterListerer(MessageCountListener listener) {
		removeListener(externalCountListener);
		externalCountListener = listener;
		addListener(externalCountListener);
	}

	public void onError(Exception e) {
		e.printStackTrace();
	}

	public void onDisconnect() {
		if (System.currentTimeMillis() - lastDisconnect < 3 * 60 * 1000 || shutDown) {
			// reconnect failed before, give up
			Exception e = new RuntimeException("to many disconnects");
			onError(e);
		} else {
			// try reconnect
			lastDisconnect = System.currentTimeMillis();
			run();
		}
	}

	private void initConnection() {

		Properties props = System.getProperties();

		// enable to throw out everything...
		// props.put("mail.debug", "true");

		String imapProtocol = "imap";
		if (useSSL) {
			imapProtocol = "imaps";
			props.setProperty("mail.imap.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
			props.setProperty("mail.imap.socketFactory.fallback", "false");
			props.setProperty("mail.imap.minidletime", "10");
		}
		props.setProperty("mail.store.protocol", imapProtocol);
		session = Session.getDefaultInstance(props, null);
		try {
			server = (IMAPStore) session.getStore(imapProtocol);
			connect();
		} catch (MessagingException ex) {
			System.out.println(ex.getMessage());
			onError(ex);
		}
	}

	private void selectFolder(String folderName) {
		try {
			closeFolder();
			if (folderName.equalsIgnoreCase("")) {
				folder = (IMAPFolder) server.getFolder("INBOX");
			} else {
				folder = (IMAPFolder) server.getFolder(folderName);
			}
			openFolder();
		} catch (MessagingException ex) {
			System.out.println(ex.getMessage());
			onError(ex);
		} catch (IllegalStateException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void openFolder() throws MessagingException {
		if (folder == null)
			return;

		folder.open(Folder.READ_ONLY);
		folder.setSubscribed(true);
		removeAllListenersFromFolder();
		addAllListenersFromFolder();
		if (folder == null)
			return;

		Runnable r = new Runnable() {

			public void run() {

				// We need to create a new thread to keep alive the connection
				Thread t = new Thread(new KeepAliveRunnable(folder),
						"IdleConnectionKeepAlive");
				t.start();

				while (!Thread.interrupted()) {
					try {
						folder.idle();
					} catch (MessagingException e) {
						System.err.println("Messaging exception during IDLE: "
								+ e.getMessage());
						nullifyListeners();
						break;
					}
				}

				// Shutdown keep alive thread
				if (t.isAlive()) {
					t.interrupt();
				}
			}
		};
		pushThread = new Thread(r, "Push-" + accountName);
		pushThread.setDaemon(true);
		pushThread.start();
		new Thread() {
			public void run() {
				try {
					pushThread.join();
					System.out.println("disconnected...");
					onDisconnect();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// your block of code
			}
		}.start();
	}

	private void closeFolder() throws MessagingException {
		if (folder == null || !folder.isOpen())
			return;

		removeAllListenersFromFolder();
		folder.setSubscribed(false);
		folder.close(false);
		folder = null;
	}

	private void removeAllListenersFromFolder() {
		removeListener(externalChangedListener);
		removeListener(externalCountListener);
	}

	private void removeListener(EventListener listener) {
		if (listener == null || folder == null) {
			return;
		}

		if (listener instanceof MessageChangedListener) {
			folder.removeMessageChangedListener((MessageChangedListener) listener);
		} else {
			if (listener instanceof MessageCountListener) {
				folder.removeMessageCountListener((MessageCountListener) listener);
			}
		}
	}

	private void addAllListenersFromFolder() {
		addListener(externalCountListener);
		addListener(externalChangedListener);
	}

	private void addListener(EventListener listener) {
		if (listener == null || folder == null) {
			return;
		}

		if (listener instanceof MessageChangedListener) {
			folder.addMessageChangedListener((MessageChangedListener) listener);
		} else {
			if (listener instanceof MessageCountListener) {
				folder.addMessageCountListener((MessageCountListener) listener);
			}
		}

		addInternalListeners(listener);

	}

	private void addInternalListeners(EventListener listener) {
		if (listener == null || folder == null) {
			return;
		}

		if (listener instanceof MessageChangedListener
				&& messageChangedListener == null) {
			messageChangedListener = new MessageChangedListener() {

				public void messageChanged(MessageChangedEvent mce) {
					// usePush();
				}
			};
			folder.addMessageChangedListener(messageChangedListener);
		} else {
			if (listener instanceof MessageCountListener
					&& messageCountListener == null) {
				messageCountListener = new MessageCountListener() {
					@Override
					public void messagesAdded(MessageCountEvent mce) {
						// usePush();
					}

					@Override
					public void messagesRemoved(MessageCountEvent mce) {
						// usePush();
					}
				};
				folder.addMessageCountListener(messageCountListener);
			}
		}
	}

	private void nullifyListeners() {
		messageChangedListener = null;
		messageCountListener = null;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getPassword() {
		return password;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public boolean isSSL() {
		return useSSL;
	}

	public String getUsername() {
		return username;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isSessionValid() {
		return server.isConnected();
	}

	@Override
	public String toString() {
		return accountName;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getVectorData() {
		Vector data = new Vector();
		data.add(accountName);
		data.add(serverAddress);
		data.add(serverPort);
		data.add(useSSL);
		data.add(username);
		data.add(password);
		return data;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
