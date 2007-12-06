CC=gcc
OBJ=hstopd.o
LIBS=-lz -lssl

all: hstopd

clean:
	rm *.o hstopd *~

hstopd:  ${OBJ}
	${CC} ${OBJ} -o hstopd ${CFLAGS} ${LIBS}

hstopd.o: hstopd.c
	${CC} -c -o hstopd.o hstopd.c ${CFLAGS}
