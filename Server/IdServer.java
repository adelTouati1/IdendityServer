package Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Handler;

public class IdServer extends UnicastRemoteObject implements IdClientHandler {
	private static int DEFAULTPORT = 5141;
	private static boolean debugMode = false;
	private static int port;
	private String name;
	private static Map<String, String> userNames;
	private static Map<String, User> userAccount;
	// private static Set<String> uuidList;// keep running list of all uuids
	// private static Set<User> userList;// keep running list of all users
	private static File databaseFile;
	private static Database myData;
	// rmiregistry 5141 &

	// export CLASSPATH=`pwd`:$CLASSPATH
	// rmiregistry 5141 & 1600

	private int myid;
	int clock = 0;
	private String[] comms = new String[] { "JOIN", "NEWID:", "HEARTBEAT:", "ELECTION:", "AWK", "COORDINATOR" };
	

	private int timeout = 3000; // 3 seconds
	MulticastSocket msock;
	private int mport = 5142;
	private int dport = 5143;
	byte[] buf = new byte[256];
	static Thread handler;
	static Thread listener;
	static Timer timer;
	static TimerTask awaitHeartbeat;
	private static boolean coordinator;
	private InetAddress group;
	private int numprocesses;
	DatagramPacket packet;
	static Object lock;

	/**
	 * @param s
	 * @throws RemoteException
	 */
	protected IdServer(String s) throws RemoteException {
		super();
		name = s;
		lock = new Object();
		packet = new DatagramPacket(buf, buf.length);
		try {
			msock = new MulticastSocket(mport);
			group = InetAddress.getByName("230.0.0.0");
			// set timeout to 3 seconds
			msock.setSoTimeout(timeout);
		} catch (IOException e) {
			// 
			e.printStackTrace();
		}
		// define threads

		awaitHeartbeat = new TimerTask() {

			@Override
			public void run() {
				// will run if heartbeat not heard in alloted time
				String message = comms[5] + myid;
				byte[] b = new byte[256];
				b = message.getBytes();

				DatagramPacket pack = new DatagramPacket(b, b.length, group, port);
				try {
					// sending election signal with my id
					msock.send(pack);

					// wait for at least 1 awknoledgement.
					// if now AWK recived become coordinator
					// if awk recived, expect
					msock.receive(pack);

				} catch (IOException e) {
					// 
					// e.printStackTrace();
					// now awk means i am coordinaror
				}

			}

		};
		// cordinates with other servers
		//TODO FIX THIS thread so it can hear other processes joining. currently they are not heard
		//and become new coordinatgors. 
		handler = new Thread() {
			public void run() {
				System.out.println("Listening for other processes");
				// listen for new servers joining.
				while (true) {
					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					try {
						msock.receive(packet);
						String msg = new String(packet.getData(), 0, packet.getLength());

						System.out.println(msg);

						if (msg.contains(comms[0])) {

							System.out.println("new process joining");
							synchronized (lock) {
								String send = comms[1] + numprocesses;
								numprocesses++;
								buf = send.getBytes();
								packet = new DatagramPacket(buf, buf.length);

								msock.send(packet);
							}

						} // if other process sends an election while
							// Coordinator is still tunning
							// announce that I am coordinator
						else if (msg.contains(comms[3])) {
							// in this case it is possible that other
							// servers do not know
							// the coordinator is runniing. anncounc that i
							// am still the coordinator
							buf = comms[5].getBytes();
							packet = new DatagramPacket(buf, buf.length, group, port);
							msock.send(packet);
						}

					} catch (IOException e) {
						System.out.println("nothing heard");
					}

				}
			}
		};
		/**
		 * A thread that listens to the other processes
		 *
		 */
		listener = new Thread() {
			//TODO kill this thread and start handler if this process becomes coordinator
			public void run() {
				try {
					byte[] buf = new byte[256];
					DatagramPacket p = new DatagramPacket(buf, buf.length);
					msock.receive(p);
					String msg = new String(p.getData(), 0, p.getLength());

					// recieve ID:# - indicates new server has been added
					if (msg.contains(comms[1])) {
						int temp = msg.indexOf(':') + 1;
						String convert = msg.substring(temp);
						numprocesses = Integer.parseInt(convert);
					}
					// recive heatbeat -- coordinator is alive, check vecotr
					// clock
					else if (msg.contains(comms[2]) && !coordinator) {
						timer.cancel();
						timer.purge();
						// reset timer
						timer = new Timer();
						timer.scheduleAtFixedRate(awaitHeartbeat, timeout, timeout);
						// update vector clock
						int temp = msg.indexOf(':') + 1;
						String convert = msg.substring(temp);
						int t = Integer.parseInt(convert);
						if(t> clock){
							recieveUpdate();
						}
					}
					// recivee election
					else if (msg.contains(comms[3]) && !coordinator) {
						timer.cancel();
						timer.purge();

						int temp = msg.indexOf(':') + 1;
						String convert = msg.substring(temp);
						temp = Integer.parseInt(convert);

						if (myid < temp) {
							doElection();
						} else {
							// else wait for coordinator

						}
						timer.scheduleAtFixedRate(awaitHeartbeat, timeout, timeout);
					}
					;

				} catch (IOException e) {
					
					e.printStackTrace();
				}
				run();
			}

		};

		initialize();

	}

	public void doElection() throws IOException {
		//TODO finish election process. 
		String msg = comms[3] + myid;
		System.out.println("joining election - " + msg);
		byte[] buff = new byte[256];
		buff = msg.getBytes();

		DatagramPacket p = new DatagramPacket(buff, buff.length, group, port);
		msock.send(p);

		msock.receive(p);
		msg = new String(p.getData(), 0, p.getLength());

	}

	/**
	 * begins communications with other processes
	 */
	public void initialize() {

		try {
			byte[] buf = new byte[256];
			// inform coordinator i am joining
			buf = comms[0].getBytes();
			packet = new DatagramPacket(buf, buf.length, group, port);
			msock.send(packet);

			boolean setID = false;
			while (!setID) {
				// attempt to recive a message
				// if i recieve nothing then the timeout will catch the
				// exception
				// and make this process the coordinator, otherwise another
				// process is running
				// and may be the coordinaor
				if(debugMode){ 					
					System.out.println("Listening for coordinator"); 				
					}
				DatagramPacket packet = new DatagramPacket(buf, buf.length); 				
				msock.receive(packet); 				 
				String message = new String(packet.getData(), 0, packet.getLength());

				if (message.contains(comms[1])) {
					int temp = message.indexOf(':') + 1;
					String convert = message.substring(temp);
					System.out.println(convert);

					myid = Integer.parseInt(convert);
					setID = true;

					timer.scheduleAtFixedRate(awaitHeartbeat, timeout, timeout);

				}
				System.out.println(message);

			}

		} catch (SocketException e) {
			e.printStackTrace();
			// Coordinator();

		}
		// buf = multicastMessage.getBytes();
		catch (IOException e) {
			//
			Coordinator();
		}
	}

	public String test() {
		return "Hello world";
	}

	/**
	 * Update other servers listening
	 * 
	 * @throws IOException
	 */
	public void sendUpdate() throws IOException {
		ServerSocket uSockServer = new ServerSocket(dport);

		
			Socket socket = uSockServer.accept();
			int count;
			byte[] buffer = new byte[(int) databaseFile.length()];

			OutputStream out = socket.getOutputStream();
			out.write((int) databaseFile.length());
			out.flush();

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(databaseFile));
			while ((count = in.read(buffer)) > 0) {
				out.write(buffer, 0, count);
				out.flush();
			}
			
			in.close();
			out.close();
			socket.close();

		uSockServer.close();
		sendUpdate();

	}

	/**
	 * recieves update from coordinator
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void recieveUpdate() throws UnknownHostException, IOException {
		//TODO ensure file is recieved from coordinator and saved localy
		Socket uSock = new Socket("localhost", dport);

		FileOutputStream fos = new FileOutputStream(databaseFile);
		//BufferedOutputStream out = new BufferedOutputStream(fos);

		int count;
		InputStream in = uSock.getInputStream();

		int size = in.read();
		byte[] buffer = new byte[size];

		while ((count = in.read(buffer)) > 0) {
			fos.write(buffer, 0, count);
		}

		fos.write(buffer, 0, size);
		fos.close();
		uSock.close();

	}

	private static final long serialVersionUID = 359415499962419496L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		databaseFile = new File("data");
		// add shutdown hook
		// program will now save both maps to their files.
		Thread hook = new Thread() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Thread#run()
			 */
			public void run() {
				try {
					save();
				} catch (FileNotFoundException e) {
					//
					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(hook);

		load();

		long period = 600000;
		Timer timer = new Timer();
		TimerTask automatedSave = new TimerTask() {
			@Override
			public void run() {
				try {
					save();
				} catch (FileNotFoundException e) {

					e.printStackTrace();
				} catch (IOException e) {
					//
					e.printStackTrace();
				}
			}
		};

		timer.scheduleAtFixedRate(automatedSave, period, period);

		//
		// [--PortNum <port>] [--verbose]
		if (args.length == 0) {
			// use default port
			port = DEFAULTPORT;
		} else {
			int i = 0;
			boolean useDefault = true;
			if (args[0].contains("numport")) {
				useDefault = false;
				port = Integer.parseInt(args[1]);

				i += 2;
			}
			if (i < args.length) {
				// ensure i doesn't go out of bounds
				if (args[i].contains("verbose")) {

					debugMode = true;
					System.out.println("debug mode active");
				}
			}

			if (useDefault) {
				port = DEFAULTPORT;
			}
		}

		try {
			IdServer server = new IdServer("//IdServer");
		} catch (RemoteException e) {
			// 
			e.printStackTrace();
		}
	}

	public void Coordinator() {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(port);
			// IdServer server = new IdServer("//IdServer");

			registry.rebind("IdServer", this);

			System.out.println("Server bound in registry at port: " + port);

			Thread Updator = new Thread() {
				public void run() {
					try {
						
						sendUpdate();
					} catch (IOException e) {
						// 
						System.out.println("error: unable to start updator thread");
						e.printStackTrace();
						//
					}
				}
			};
			Updator.start();
			
			System.out.println("I AM COORDINATOR");
			// timeout
			coordinator = true;
			// this is first process
			numprocesses = 1;
			myid = 0;

			// start heartbeat
			TimerTask heartbeat = new TimerTask() {

				@Override
				public void run() {
					// TODO heartbeat can also indicate current vecotr clock to other proccesses. 
					String msg;
					synchronized(lock){
						 msg = comms[2] + clock;
					}
					byte[] buf = new byte[256];
					buf = msg.getBytes();
					DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
					try {
						msock.send(packet);
						System.out.println("heartbeat sent");
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}

			};
			timer = new Timer();
			timer.scheduleAtFixedRate(heartbeat, timeout / 2, timeout / 2);
			handler.start();
			
		} catch (RemoteException e) {
			// 
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Server.IdClientHandler#CreateUser(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public String CreateUser(String userName, String password) {
		// new UUID(0, 0);
		//
		UUID uuid = UUID.randomUUID();
		String uuidString = uuid.toString();
		if (debugMode) {
			System.out.println(userName + " bound to " + uuid.toString());
		}
		userNames.put(userName, uuidString);
		String ip = null;
		try {
			ip = UnicastRemoteObject.getClientHost();
		} catch (ServerNotActiveException e) {
			//
			e.printStackTrace();
		}
		Date now = new Date();
		// String currentDate = new SimpleDateFormat("ddMMyyyy
		// HH:mm").format(now);
		User client = new User(userName, ip, now);
		client.setPassword(password);
		userAccount.put(uuid.toString(), client);
		String ret = userName + " created. UUID: " + uuid.toString();
		clock ++;
		
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Server.IdClientHandler#CreateUser(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public String CreateUser(String userName, String realname, String password) {
		UUID uuid = UUID.randomUUID();
		String test = userNames.get(userName);
		if (!test.equals(null)) {
			if (debugMode) {
				System.out.println(userName + " already exists.");
			}
			return "username already exists";
		}

		userNames.put(userName, uuid.toString());
		String ip = null;

		if (debugMode) {
			System.out.println(userName + " bound to " + uuid.toString());
		}
		try {
			ip = UnicastRemoteObject.getClientHost();
		} catch (ServerNotActiveException e) {
			//
			e.printStackTrace();
		}
		Date now = new Date();
		// String currentDate = new SimpleDateFormat("ddMMyyyy
		// HH:mm").format(now);
		User client = new User(userName, ip, now);
		if (debugMode) {
			System.out.println(client.toString());
		}
		client.setRealName(realname);
		client.setPassword(password);
		// uuidList.add(uuid.toString());
		// userList.add(client);
		userAccount.put(uuid.toString(), client);

		String ret = userName + " created with realname " + realname + "\nuuid: " + uuid.toString();
		clock ++;
		
		return ret;
	}

	@Override
	public String lookup(String userName) {
		//
		if (debugMode) {
			System.out.println("searching for: " + userName);
		}

		User client;
		if (userNames.containsKey(userName)) {
			String uuid = userNames.get(userName);
			client = userAccount.get(uuid);
		} else {
			return "Unable to find client: " + userName;
		}
		return client.toString();
	}

	@Override
	public String reverseLookup(String uuid) {
		//
		String ret = "";
		if (userAccount.containsKey(uuid)) {
			User client = userAccount.get(uuid);
			if (debugMode) {
				System.out.println("Found: " + client.getUsername());
			}

			ret = client.toString();
		} else {
			if (debugMode) {
				System.out.println("Counld not find: " + uuid);
			}
			ret = "Account: " + uuid + " does not exist.";

		}

		return ret;
	}

	@Override
	public String modify(String oldName, String newName, String password) {
		String ret = "Failed to modify";
		String uuid = userNames.get(oldName);
		if (userNames.containsKey(oldName)) {
			User client = userAccount.get(uuid);
			if (debugMode) {
				System.out.println(oldName + " found");
			}
			if (client.validatePassword(password)) {
				// userList.remove(client);
				userNames.remove(oldName);
				userNames.put(newName, uuid);

				Date now = new Date();
				client.updateName(newName, now);
				// userList.add(client);
				ret = "Name changed from " + oldName + " to " + newName;
				clock ++;
			} else {
				if (debugMode) {
					System.out.println("incorrect password");
				}
			}

		}
		return ret;
	}

	public String deleteUser(String name, String password) {
		String ret = "Failed to delete user";
		//
		String uuid = userNames.get(name);
		if (userNames.containsKey(name)) {
			User target = userAccount.get(uuid);
			if (target.validatePassword(password)) {
				if (debugMode) {
					System.out.println("Deleting: " + name);
				}
				userNames.remove(name);
				userAccount.remove(uuid);
				// userList.remove(target);
				// uuidList.remove(uuid);
				ret = "User:" + name + " deleted";
				clock ++;
			} else {
				// System.out.println("incorrect password");
				ret = "Incorrect password";
			}
		} else {
			// System.out.println("account not found");
			ret = "account not found";
		}
		return ret;
	}

	private static void save() throws FileNotFoundException, IOException {
		if (debugMode) {
			System.out.println("Saving...");
		}
		// if(databaseFile.exists()){
		// databaseFile.delete();
		// databaseFile.createNewFile();
		// }
		if (!databaseFile.exists()) {
			// databaseFile.delete();
			databaseFile.createNewFile();

		}
		Database saveData = new Database(userNames, userAccount);
		// , uuidList, userList);

		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(databaseFile));
		out.writeObject(saveData);
		out.close();
		if (debugMode) {
			System.out.println("Save complete.");
		}
	}

	private static void load() {

		if (debugMode) {
			System.out.println("Loading");
		}

		if (databaseFile.exists()) {
			try {
				ObjectInputStream oin = new ObjectInputStream(new FileInputStream(databaseFile));
				myData = (Database) oin.readObject();

				userNames = myData.getUserNames();
				userAccount = myData.getAccounts();
				// uuidList = myData.getUuidList();// keep running list of all

				// userList = myData.getUserList();// keep running list of all
				oin.close();
			} catch (Exception e) {

			}

		} else {
			if (debugMode) {
				System.out.println("Makeing new structures");
			}
			userNames = new HashMap<String, String>();
			userAccount = new HashMap<String, User>();
			// uuidList = new HashSet<String>();
			// userList = new HashSet<User>();
		}
		try {
			databaseFile.createNewFile();
		} catch (Exception e) {
		}

	}

	@Override
	public String[] get(String option) {
		//
		if (debugMode) {
			System.out.println("using get");
			System.out.println(option);
		}
		String[] ret = new String[1];
		if (option.equalsIgnoreCase("uuids") || option.equalsIgnoreCase("uuid")) {
			// return set of all UUIDS
			if (debugMode) {
				System.out.println("retriving uuids");
			}
			int count = 0;

			for (Map.Entry<String, String> entry : userNames.entrySet()) {
				// string srt : uuidList
				count++;
			}
			ret = new String[count];
			count = 0;
			for (Map.Entry<String, String> entry : userNames.entrySet()) {
				if (debugMode) {
					System.out.println(count + ": " + entry.getValue());
				}
				ret[count] = entry.getValue();
				count++;
			}

		} else if (option.equalsIgnoreCase("users") || option.equalsIgnoreCase("user")) {
			int count = 0;
			if (debugMode) {
				System.out.println("retriving users");
			}
			for (Map.Entry<String, User> entry : userAccount.entrySet()) {
				count++;
			}
			ret = new String[count];
			count = 0;
			for (Map.Entry<String, User> entry : userAccount.entrySet()) {
				ret[count] = entry.getValue().toString();
				if (debugMode) {
					System.out.println(count + ": " + ret[count]);
				}
				count++;
			}

		} else if (option.equalsIgnoreCase("all")) {
			int count = 0;
			if (debugMode) {
				System.out.println("retriving everything");
			}
			for (Map.Entry<String, User> entry : userAccount.entrySet()) {
				count++;
			}
			ret = new String[count];

			count = 0;
			for (Map.Entry<String, User> entry : userAccount.entrySet()) {
				// User target = userAccount.get(uuid);

				// ret[count] = uuid + ": \n" + target.toString();
				ret[count] = entry.getKey() + "\n" + entry.getValue().toString();

				if (debugMode) {
					System.out.println(count + ": " + ret[count]);
				}
				count++;
			}
		} else {
			ret = new String[1];
			ret[0] = "Incorrct input";
		}
		return ret;
	}

}