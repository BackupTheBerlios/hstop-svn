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
#include <string.h>
#include <zlib.h>
#include <time.h>
#include <openssl/md5.h>

int port;

void grepenv() {
	// grep listen port from env
	if (getenv("HSTOPD_PORT"))
		port = atoi(getenv("HSTOPD_PORT"));
	else
		port = DEFAULT_PORT;
}

void read_block(int sock,unsigned char *buf, int buflen) {
	int l = 0;
	int ll = 0;
	while ((l = read(sock, (unsigned char*) (buf + ll), buflen - ll))) {
		ll += l;
		if (buflen - ll <= 0)
			break;
	}
}

void read_message(int sock, unsigned char *buf, unsigned long *buflen) {
	char lenbuf[128];
	int l;
	int i = 0;
	while ((l = read(sock, (char*) (lenbuf + i), 1))) {
		if (*(lenbuf + i) == 0)
			break;
		i++;
	}
	
	unsigned long mlen = atoi(lenbuf);
	
	//printf("mlen: %u\n", mlen);
	
	unsigned char *buffer = (char*) malloc(mlen);
	read_block(sock, buffer, mlen);
	unsigned long ll = *buflen;
	uncompress(buf, buflen, buffer, mlen);
	if (*buflen > ll)
		buf[*buflen] = 0;
	//printf("len: %u\n", *buflen);
	free(buffer);
}

void send_message(int sock, unsigned char *buf, unsigned long buflen) {
	unsigned long compbuflen = (unsigned long)(buflen * 1.02 ) + 13;
	unsigned char *compbuf = (char*) malloc(compbuflen);
	char lenbuf[128];
	compress(compbuf, &compbuflen, buf, buflen);
	sprintf(lenbuf, "%u\0", compbuflen);
	write(sock, lenbuf, strlen(lenbuf));
	write(sock, compbuf, compbuflen);
	free(compbuf);
}

void handle_connection(int sock) {
	//TODO: fill me
	unsigned char buf[1024];
	// clear 0x1301000027
	int l = read(sock, buf, 5);
	printf("readlen: %d\n", l);
	// test
	//char testbuf[] = "helloooooooooooooooo girls!";
	//send_message(sock, testbuf, strlen(testbuf));
	// get loginmsg
	unsigned long buflen = 1024;
	read_message(sock, buf, &buflen);
	int i;
	//printf("read: %s#EOS#\n", buf);
	if (buflen <= 0)
		return;
	if (buf[0] != 0x01)
		return;
	char *usr = (char*) malloc(buflen);
	for (i = 1; i < buflen; i++)
		usr[i - 1] = buf[i];
	usr[buflen] = 0;
	
	srand(time(0));
	char challange[32];
	challange[0] = 0x01;
	for (i = 1; i < 33; i++)
		challange[i] = (char) (rand() % 90) + 33;
	send_message(sock, challange, 33);
	// challange send. wait for passord
	buflen = 1024;
	read_message(sock, buf, &buflen);
	
	char md5pw[] = "3c09d489ff5b05c0798564d077817c62";
	//FIXME: read usr:pw
	//FIXME: calc md5 of challange + md5pw
	//FIXME: check against recved challange
	//unsigned char md5challange[MD5_DIGEST_LENGTH];
	//unsigned char *md5challange_check = (char*) malloc(strlen(md5challange) + strlen(md5pw));
	
	//MD5(md5pw);
	
	buf[0] = 0x02;
	buf[1] = 'p';
	buf[2] = 'a';
	buf[3] = 's';
	buf[4] = 's';
	send_message(sock, buf, 5);
	
	free(usr);
}

int main(int argc, char *argv[]) {
	grepenv();
	int sock = socket(AF_INET, SOCK_STREAM, 0);
	struct sockaddr_in server;
	if (sock < 0) {
		perror("error opening socket");
		exit(EXIT_FAILURE);
	}
	
	server.sin_family = AF_INET;
	server.sin_addr.s_addr = INADDR_ANY;
	server.sin_port = htons(port);
	if (bind(sock, (struct sockaddr *) &server, sizeof(struct sockaddr_in))) {
		perror("error binding socket");
		exit(EXIT_FAILURE);
	}
	listen(sock, MAX_QUEUE);
	printf("listen on port %d\n", port);
	int sockd;
	while (( sockd = accept(sock,0,0)) > 0)	{
		printf("incomming connection: %d\n", sockd);
		if (fork() == 0) {
			close(0);
			close(sock);
			handle_connection(sockd);
			close(sockd);
			exit(EXIT_SUCCESS);
		}
		close(sockd);
		while (waitpid(-1, NULL, WNOHANG))
			{}
	}
	close(sock);
	return EXIT_SUCCESS;
}
