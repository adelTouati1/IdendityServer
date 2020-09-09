package Client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

//import org.apache.commons.cli.*;

import Server.IdClientHandler;

public class IdClient {

	public static void main(String[] args) {
		// Not sure what to put instead of null
		for (String str : args) {
			System.out.println(str);
		}
		IdClientHandler h = null;
		String host = "localhost";
		int port = 5141;
		int numQueries = 0; // should only be 1, else exit
		boolean passwordEntered = false;
		// TODO Auto-generated method stub
		if (args.length < 2) {
			System.out.println("java IdClient --server <serverhost> [--numport <port#>] <query>");
			System.exit(1);
		}

		int count = 0;
		int numQuery = 0;
		String password = "";
		boolean realname = false;
		String[] commandArgs = new String[2];
		int command = 0;

		for (String str : args) {
			if (str.contains("server") || str.equals("-s")) {
				host = args[count + 1];
			} else if (str.contains("numport") || str.equals("-n")) {
				if (args.length > count + 1) {
					port = Integer.parseInt(args[count + 1]);
				} else {
					System.out.println("java IdClient --server <serverhost> [--numport <port#>] <query>");
					System.out.println("not enough args.");
					System.exit(1);
				}
			} else if (str.contains("create") || str.equals("-c")) {
				command = 0;
				if (count + 1 >= args.length) {
					
					System.out.println("not enough args.");
					System.exit(1);
				}
				commandArgs[0] = args[count + 1];
				if(!args[count+2].contains("-p")){
					realname = true;
					commandArgs[1]= args[count+2];
				}
				numQuery++;
			} else if (str.contains("password") || str.equals("-p")) {
				// numQuery ++;
				if (count + 1 >= args.length) {
					System.out.println("not enough args.");
					System.exit(1);
				}
				passwordEntered = true;
				password = args[count + 1];
				password = encryptPassword(password);
			} else if (str.contains("lookup") || str.equals("-l")) {
				command = 1;
				if (count + 1 >= args.length) {
					System.out.println("not enough args.");
					System.exit(1);
				}
				commandArgs[0] = args[count + 1];
				numQuery++;
			} else if (str.contains("reverse-Lookup") || str.equals("-r")) {
				command = 2;
				if (count + 1 >= args.length) {
					System.out.println("not enough args.");
					System.exit(1);
				}
				commandArgs[0] = args[count + 1];
				numQuery++;
			} else if (str.contains("modify") || str.equals("-m")) {
				command = 3;
				if (count + 2 >= args.length) {
					System.out.println("not enough args.");
					System.exit(1);
				}
				commandArgs[0] = args[count + 1];
				commandArgs[1] = args[count + 2];
				numQuery++;
			} else if (str.contains("delete") || str.equals("-d")) {
				command = 4;
				if (count + 1 >= args.length) {
					System.out.println("not enough args.");
					System.exit(1);
				}
				commandArgs[0] = args[count + 1];
				numQuery++;
			} else if (str.contains("get") || str.equals("-g")) {
				command =5;
				if (count + 1 >= args.length) {
					System.out.println("not enough args.");
					System.exit(1);
				}
				commandArgs[0] = args[count+1];
				numQuery++;
			}
			count++;
		}
		
		if(numQuery !=1){
			System.out.println("incorrect number of queries");
			System.exit(2);
		}

		try {
			System.setProperty("java.security.policy", "../mysecurity.policy");

			Registry registry = LocateRegistry.getRegistry(host, port);
			String address = "IdServer";

			h = (IdClientHandler) registry.lookup(address);
			String reply;
			//create
			if(command == 0){
				if(realname){
					reply = h.CreateUser(commandArgs[0], commandArgs[1], password);
				}
				else{
					reply = h.CreateUser(commandArgs[0], password);
				}
				System.out.println(reply);
			}
			//lookup 1
			else if(command == 1){
				reply = h.lookup(commandArgs[0]);
				System.out.println(reply);
			}
			//reversre 2
			else if(command ==2){
				reply = h.reverseLookup(commandArgs[0]);
				System.out.println(reply);
			}
			//modify 3
			else if(command ==3){
				reply = h.modify(commandArgs[0],commandArgs[1],password);
				System.out.println(reply);
			}
			//delete 4
			else if(command ==4){
				reply = h.deleteUser(commandArgs[0], password);
				System.out.println(reply);
			}
			//get 5
			else if(command == 5){
				String[] get = h.get(commandArgs[0]);
				for(String str : get){
					System.out.println( str);
					System.out.println();
				}
			}
			
		} catch (AccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// client encode it using the SHA-2 algorithm.
	public static String encryptPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			// return needs to be saved with the username and every time the
			// client tries to
			// log in with password we compare the result at the server if is
			// same or not
			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}