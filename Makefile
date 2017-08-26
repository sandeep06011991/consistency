server:
	rm -rf log/
	mkdir log
	ant server-jar
	ant run-server -Dnode_id=1 &
	ant run-server -Dnode_id=2 &