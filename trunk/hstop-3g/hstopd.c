#define DEFAULT_PORT 8980
#define MAX_QUEUE 10

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <sys/wait.h>

int port;

void grepenv()
{
	// grep listen port from env
	if (getenv("HSTOPD_PORT"))
		port = atoi(getenv("HSTOPD_PORT"));
	else
		port = DEFAULT_PORT;
}

int main(int argc, char *argv[])
{
	grepenv();
	int sock = socket(AF_INET, SOCK_STREAM, 0);
	struct sockaddr_in server;
	if (sock < 0)
	{
		perror("error opening socket");
		exit(EXIT_FAILURE);
	}
	
	server.sin_family = AF_INET;
	server.sin_addr.s_addr = INADDR_ANY;
	server.sin_port = htons(port);
	if (bind(sock, (struct sockaddr *) &server, sizeof(struct sockaddr_in)))
	{
		perror("error binding socket");
		exit(EXIT_FAILURE);
	}
	listen(sock, MAX_QUEUE);
	printf("listen on port %d\n", port);
	int sockd;
	while (( sockd = accept(sock,0,0)) > 0)
	{
		printf("incomming connection: %d\n", sockd);
		if (fork() == 0)
		{
			close(0);
			close(sock);
			//TODO: FILLME
			exit(EXIT_SUCCESS);
		}
		close(sockd);
		while (waitpid(-1, NULL, WNOHANG))
			{}
	}
	close(sock);
	return EXIT_SUCCESS;
}
