CC=gcc
OBJ=hstopd.o

all: hstopd

clean:
	rm *.o hstopd *~

hstopd:  ${OBJ}
	${CC} ${OBJ} -o hstopd ${CFLAGS}

hstopd.o: hstopd.c
	${CC} -c -o hstopd.o hstopd.c ${CFLAGS}
