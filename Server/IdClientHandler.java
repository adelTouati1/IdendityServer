package Server;

import java.rmi.RemoteException;

public interface IdClientHandler  extends java.rmi.Remote{
	//Create new User


	/**
	 * @param userName
	 * @param password
	 * @return
	 * @throws RemoteException
	 */
	String CreateUser(String userName, String password) throws RemoteException;
	
	/**
	 * @param userName
	 * @param realName
	 * @param Password
	 * @return
	 * @throws RemoteException
	 */
	String CreateUser(String userName, String realName, String Password) throws RemoteException;
	
	
	/**
	 * @param userName
	 * @return
	 * @throws RemoteException
	 */
	String lookup(String userName) throws RemoteException;
	
	/**
	 * @param UUID
	 * @return
	 * @throws RemoteException
	 */
	String reverseLookup(String UUID) throws RemoteException;
	
	
	/**
	 * @param oldName
	 * @param newName
	 * @param Password
	 * @return
	 * @throws RemoteException
	 */
	String modify(String oldName, String newName, String Password) throws RemoteException;
	
	
	/**
	 * @param name
	 * @param password
	 * @return
	 * @throws RemoteException
	 */
	String deleteUser(String name,String password) throws RemoteException;
	
	/**
	 * @param  users|uuids|all
	 * @return
	 * @throws RemoteException
	 */
	String[] get(String option) throws RemoteException;
	
	
	

	String test() throws RemoteException;
}