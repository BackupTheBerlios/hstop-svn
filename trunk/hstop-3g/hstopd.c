#define DEFAULT_PORT 8980
#define MAX_QUEUE 10
#define DEFAULT_PW1 "flx:3c09d489ff5b05c0798564d077817c62"
#define DEFAULT_PW2 "test:3c09d489ff5b05c0798564d077817c62"

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
char **pwlist;
int pwcount;

void grepenv() {
	// grep listen port from env
	if (getenv("HSTOPD_PORT"))
		port = atoi(getenv("HSTOPD_PORT"));
	else
		port = DEFAULT_PORT;

	if (getenv("HSTOPD_PWLIST"))
		pwlist = 0;
		//FIXME: do sth.
	else {
		pwlist = (char**) malloc(sizeof(char*) * 2);
		char *pw;
		pw = (char*) malloc(MD5_DIGEST_LENGTH * 2 + 5);
		strcpy(pw, DEFAULT_PW1);
		pwlist[0] = pw;
		pw = (char*) malloc(MD5_DIGEST_LENGTH * 2 + 5);
		strcpy(pw, DEFAULT_PW2);
		pwlist[1] = pw;
		pwcount = 2;
		int i;
		for (i = 0; i < pwcount; i++)
			printf("pwstring%d: %s\n", i, pwlist[i]);
	}
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
	// get loginmsg
	unsigned long buflen = 1024;
	read_message(sock, buf, &buflen);
	int i;
	if (buflen <= 0)
		return;
	if (buf[0] != 0x01)
		return;
	char *usr = (char*) malloc(buflen);
	for (i = 1; i < buflen; i++)
		usr[i - 1] = buf[i];
	usr[buflen] = 0;
	
	srand(time(0));
	char challange[34];
	challange[0] = 0x01;
	for (i = 1; i < 33; i++)
		challange[i] = (char) (rand() % 90) + 33;
	challange[33] = 0x00;
	send_message(sock, challange, 33);
	// challange send. wait for passord
	buflen = 1024;
	read_message(sock, buf, &buflen);
	
	char *md5pw = (char*) malloc(MD5_DIGEST_LENGTH * 2 + 1);
	
	for (i = 0; i < pwcount; i++) {
		int p;
		char *usrn = (char*) malloc(strlen(pwlist[i]));
		int j;
		for (j = 0; pwlist[i][j] != 0; j++) {
			usrn[j] = pwlist[i][j];
			if (pwlist[i][j] == ':') {
				usrn[j] = 0;
				break;
			}
		}
		printf("is user %s == %s?\n", usr, usrn);
		if (strcmp(usrn, usr) == 0) {
			printf("yes!\n");
			int k = 0;
			for (j = strlen(usr) + 1; pwlist[i][j] != 0; j++) {
				md5pw[k] = pwlist[i][j];
				k++;
			}
			md5pw[k] = 0;
			free(usrn);
			break;
		}
		free(usrn);
	}
	
	printf("found pw: %s\n", md5pw);
	
	char *challange_md5pw = (char*) malloc(strlen(challange) + strlen(md5pw));
	strcpy(challange_md5pw, challange);
	strcat(challange_md5pw, md5pw);
	++challange_md5pw; // skip first char!
	
	unsigned char md5challange[MD5_DIGEST_LENGTH * 2];
	//unsigned char *md5challange_check = (char*) malloc(strlen(md5challange) + strlen(md5pw));
	MD5(challange_md5pw, strlen(challange_md5pw), md5challange);
	
	printf("md5: ");
	for (i = 0; i <= MD5_DIGEST_LENGTH; i++)
		printf("%d ", md5challange[i]);
	printf("\n");
	
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
