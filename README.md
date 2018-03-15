## After compiling everything, use following steps to communicate between instances.


1. Run 2 instances of this application


	**java Main [port] [hostname of this instance] [MTU]**
	
	
	e.g. java Main 5555 test1 9
	
	When sending a message, the program will make sure the protocol message size (type+length+payload) is not bigger than MTU.

2. Connect to remote instance


	**connect [ip:port]**
	
	e.g. connect 127.0.0.1:5555
	
	
	If both instances are running on the same machine, you can skip the IP address(connect 5555).

3. Send your message to remote instance.


	**send Test_message**

4. Disconnect when done.


	**disconnect**



---
