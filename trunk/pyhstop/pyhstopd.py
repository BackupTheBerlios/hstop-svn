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

import socket
import os
from SocketServer import BaseServer
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
from SimpleHTTPServer import SimpleHTTPRequestHandler
import Queue
import threading
from optparse import OptionParser
import sys
import urllib
import urllib2
import time
import md5
import cgi

QUEUE_TIMEOUT = 5
DEFAULT_PORT = 9099
REQUEST_BUFF_SIZE = 128

keyfile = ''
certfile = ''

class queues:
	qin = Queue.Queue()
	qout = Queue.Queue()

class sessionItem:
	q = queues
	sid = ''
	t = 'tcp'
	p = 80
	h = 'localhost'
	
	def __init__(self, sessionID, socketType, host, port):
		self.sid = sessionID
		self.t = socketType
		self.p = int(port)
		self.h = host
		print 'added session: ', self.sid, self.t, self.h, self.p

class sessionList:
	l = {};
	
	def add(self,sessionID, socketType, host, port):
		if not self.l.has_key(sessionID):
			self.l[sessionID] = sessionItem(sessionID, socketType, host, port)
	
	def rm(self,sessionID):
		del self.l[sessionID]
	
	def get(self,sessionID):
		try:
			return self.l[sessionID]
		except KeyError:
			return None

sessionlist = sessionList()

class SecureHTTPServer(HTTPServer):
    def __init__(self, server_address, HandlerClass):
	from OpenSSL import SSL
        BaseServer.__init__(self, server_address, HandlerClass)
        ctx = SSL.Context(SSL.SSLv23_METHOD)
        #server.pem's location (containing the server private key and
        #the server certificate).
	ctx.use_privatekey_file (keyfile)
        ctx.use_certificate_file(certfile)
        self.socket = SSL.Connection(ctx, socket.socket(self.address_family,
                                                        self.socket_type))
        self.server_bind()
        self.server_activate()


class myHTTPRequestHandler(BaseHTTPRequestHandler):
	blablubb = None
	def do_GET(self):
		try:
			(stuff, args) = self.path.split('?',1)
		except Queue.Empty: ## dummy
			args = self.path
		arglist = cgi.parse_qs(args)
		try:
			s = urllib.unquote(arglist['i'][0])
			sessionlist.add(s, arglist['t'][0], arglist['h'][0], arglist['p'][0])
			sessionlist.get(s).q.qout.put(urllib.unquote(arglist['d'][0]))
			item = sessionlist.get(s).q.qin.get(True, QUEUE_TIMEOUT)
		except (Queue.Empty, AttributeError):
			item = None
		self.send_response(200)
		self.end_headers()
		try:
			if item: self.wfile.write(item)
		except AttributeError:
			item = None

class SecureHTTPRequestHandler(myHTTPRequestHandler):
    def setup(self):
        self.connection = self.request
        self.rfile = socket._fileobject(self.request, "rb", self.rbufsize)
        self.wfile = socket._fileobject(self.request, "wb", self.wbufsize)

class httpListener:
	p = 80
	r = '/'
	s = False
	HandlerClass = myHTTPRequestHandler
	ServerClass = HTTPServer
	httpd = None
	def __init__(self, port, root, ssl):
		self.p = port
		self.r = root
		self.s = ssl
		if self.s:
			self.HandlerClass = SecureHTTPRequestHandler
			self.ServerClass = SecureHTTPServer
		self.httpd = self.ServerClass(('', self.p), self.HandlerClass)
	
	def listen(self):
		self.httpd.serve_forever()

def main():
	usage = "usage: %prog [options]"
	parser = OptionParser(usage=usage)
	parser.add_option('-t', '--tcp', action='store_const', dest='t', const='tcp', default='tcp', help='tcp mode (default)')
	parser.add_option('-u', '--udp', action='store_const', dest='t', const='udp', help='udp mode')
	#parser.add_option('-q', '--quiet', action="store_const", const=0, dest="v", default=1, help='quiet')
	#parser.add_option('-v', '--verbose', action="store_const", const=1, dest="v", help='verbose')
	
	parser.add_option('--root', action="store", dest="root", help='rootpath', default='/')
	parser.add_option('-s', '--ssl', action='store_true', dest='ssl', default=False, help='use https')
	parser.add_option('-n', '--no-ssl', action='store_false', dest='ssl', help='do not use https (default)')
	parser.add_option('-p', '--port', action='store', dest='port', type='int', default=80, help='port to listen')
	parser.add_option('-c', '--cert', action='store', dest='cert', default='cert.pem', help='certificate to use')
	parser.add_option('-k', '--key', action='store', dest='key', default='key.pem', help='key to use')
	
	
	(options, args) = parser.parse_args()
	
	print 'start..'
	
	q = queues()
	
	if options.ssl:
		from OpenSSL import SSL
	global keyfile, certfile
	keyfile = options.key
	certfile = options.cert
	
	hl = httpListener(options.port, options.root, options.ssl)
	hl.listen()
	
	sys.stdin.readline()
	
	print 'end..'

main()