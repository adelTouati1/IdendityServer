Project 3 -Distributed Identity Server (Phase II)
CS455 S20

Adel Touati


#files
IdClient.java - All the command parsing and hashing the password
IdServer.java - Storing all client data in Database
User.java - keep track of the user user name and old user name
IdClientHandler.java - an interface for handling all client functionalities
Database.java - holds Maps and sets


#Building and running
Use make 
To run the Server use : 
$ java Server/IdServer [--numport <port#>] [--verbose]
The --numport to pick the port the server will connect to. 
The --verbose option makes the server print detailed messages on the operations as it executes them.

For running the client: java Client/IdClient --server <serverhost> [--numport <port#>] <query>
A Query can be one of the following:
--create <loginname> [<real name>] [--password <password>]
--lookup <loginname>
--reverse-lookup <UUID>
--modify <oldloginname> <newloginname> [--password <password>]
--delete <loginname> [--password <password>]
--get users|uuids|all
The above options can be abbreviated as -s, -n, -c, -l, -r, -m, -d, -p (for password) and -g.
Note that we can supply only one query at a time



