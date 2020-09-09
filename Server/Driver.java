package Server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Driver {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String host = "localhost";
		int port = 5141;

		IdClientHandler h;
		try {

			System.setProperty("java.security.policy", "../mysecurity.policy");
			System.setProperty("javax.net.ssl.trustStore", "../resources/Client_Truststore");
			System.setProperty("javax.net.ssl.trustStorePassword", "test123");

			Registry registry = LocateRegistry.getRegistry(host, port);
			String address = "IdServer";
			h = (IdClientHandler) registry.lookup(address);

			if (h.equals(null)) {
				System.out.println("this should not happen");
			}

			System.out.println(h.test());

			h.CreateUser("Debra", "password");

			String reply = h.CreateUser("Jonny", "password");
			System.out.println(reply);

			int pointer = reply.indexOf("UUID: ") + 6;
			String uuid = reply.substring(pointer);
			// System.out.println(uuid);
			reply = h.reverseLookup(uuid);
			System.out.println(reply);

			reply = h.CreateUser("Don", "password");
			System.out.println(reply);

			reply = h.CreateUser("Ralph", "password");
			System.out.println(reply);

			reply = h.CreateUser("hubert", "password");
			System.out.println(reply);

			reply = h.lookup("Jonny");
			System.out.println(reply);

			System.out.println("\n");

			String[] allUsers = h.get("all");

			for (String str : allUsers) {
				System.out.println(str);
			}

			allUsers = h.get("uuid");
			for (String str : allUsers) {
				System.out.println(str);
			}

			reply = h.lookup("Ralph");
			System.out.println(reply);

			reply = h.deleteUser("Ralph", "password");
			System.out.println(reply);

			reply = h.lookup("Ralph");
			System.out.println(reply);

			reply = h.lookup("Don");
			System.out.println(reply);

			reply = h.modify("Don", "DonaldDuck", "password");
			System.out.println(reply);
			reply = h.lookup("Don");
			System.out.println(reply);
			reply = h.lookup("DonaldDuck");
			System.out.println(reply);

			allUsers = h.get("all");
			for (String str : allUsers) {
				System.out.println(str);
			}

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}