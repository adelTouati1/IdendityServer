package Server;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class User implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5463068729460631940L;
	private String userName; 
	private boolean hasRealname;
	private String realName; 
	private String password;
	//store date and time of last request
	private String lastRequest;
	//store date and time of each update to username
	private String lastUpdate;
	// last Access
	private String ipAccess;
	public User(String userName, String ip, Date now) {
		// TODO Auto-generated constructor stub
		this.userName = userName; 
		hasRealname = false;
		ipAccess = ip; 
		lastUpdate = new SimpleDateFormat("MM dd yyyy HH:mm").format(now);
		lastRequest = lastUpdate;
		password = null;
	}
	public void setRealName(String name){
		this.realName = name;
		hasRealname = true;
	}
	
	
	public void updateName(String newName, Date now) {
		// TODO Auto-generated method stub
		userName = newName;
		
		lastUpdate = new SimpleDateFormat("MM dd yyyy HH:mm").format(now);
		lastRequest = lastUpdate;
	}
	
	public boolean validatePassword(String pass){
		return password.equals(pass);
	}
	public String toString(){
		String str = userName;
		if (hasRealname){
			str += " \n"+ realName;
		}
		str += "\nLast account request: "+ lastRequest;
		str += "\nLast account update: "+ lastUpdate;
		str += "\nip address: " + ipAccess;
		
		return str;
	}
	public void setPassword(String password) {
		// TODO Auto-generated method stub
		this.password = password;
		
	}
	public String getUsername(){
		return userName;
	}
}