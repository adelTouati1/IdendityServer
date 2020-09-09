package Server;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class Database implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2590437652355183650L;
	private Map<String, String> userNames;
	private Map<String, User> userAccount;
	// private Set<String> uuidList;// keep running list of all uuids
	// private Set<User> userList;// keep running list of all users

	public Database(Map<String, String> userNames,
			Map<String, User> userAccount){ 
			//Set<String> uuidList,
			//Set<User> userList) {
		this.userNames = userNames;
		this.userAccount = userAccount;
		//this.uuidList = uuidList;
		//this.userList = userList;

	}

	public Map<String, String> getUserNames() {
		return userNames;
	}

	public Map<String, User> getAccounts() {
		return userAccount;
	}
	/**
	public Set<String> getUuidList() {
		return uuidList;
	}

	public Set<User> getUserList() {
		return userList;
	}*/

}