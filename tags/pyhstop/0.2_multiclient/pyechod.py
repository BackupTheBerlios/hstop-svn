#!/usr/bin/env python
############################################################################
#    Copyright (C) 2007 by Felix Bechstein   #
#    flx@un1337.de   #
#                                                                          #
#    This program is free software; you can redistribute it and#or modify  #
#    it under the terms of the GNU General Public License as published by  #
#    the Free Software Foundation; either version 2 of the License, or     #
#    (at your option) any later version.                                   #
#                                                                          #
#    This program is distributed in the hope that it will be useful,       #
#    but WITHOUT ANY WARRANTY; without even the implied warranty of        #
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         #
#    GNU General Public License for more details.                          #
#                                                                          #
#    You should have received a copy of the GNU General Public License     #
#    along with this program; if not, write to the                         #
#    Free Software Foundation, Inc.,                                       #
#    59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             #
############################################################################

## this is for testing only!
import socket, threading

def handleSession(sock, conn, addr):
	print 'new session'
	d = True
	c = True
	while d:
		d = conn.recv(512)
		print 'echo (', addr, '): ', d.strip() 
		conn.send(d)
 
def main():
	print 'start ..'
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.bind(('', 19999))
	thr = []
	while True:
		s.listen(3)
		conn, addr = s.accept()
		t = threading.Thread(target=handleSession,args=(s, conn, addr))
		t.start()
		thr.append(t)
main()