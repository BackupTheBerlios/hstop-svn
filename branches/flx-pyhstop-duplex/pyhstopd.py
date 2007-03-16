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
import base64, binascii
import cgi
from pyhstop_common import httpencode, httpdecode

QUEUE_TIMEOUT = 3
CONECTION_TIMEOUT = QUEUE_TIMEOUT * 2
DEFAULT_PORT = 9099
REQUEST_BUFF_SIZE = 128
REQUES_MAX_SIZE = 1024
SPLITCHAR = '-'

keyfile = ''
certfile = ''

class queues:
	qin = None
	qout = None
	def __init__(self):
		self.qin = Queue.Queue()
		self.qout = Queue.Queue()

class sessionItem:
	q = None
	sid = ''
	t = 'tcp'
	p = 80
	h = 'localhost'
	tin = None
	tout = None
	work = True
	conn = None
	last = True
	cleanable = False
	
	def __init__(self, sessionID, socketType, host, port):
		self.q = queues()
		self.sid = sessionID
		self.t = socketType
		self.p = int(port)
		self.h = host
		self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self.tin = threading.Thread(target=self.inThr)
		self.tout = threading.Thread(target=self.outThr)
		self.tin.setDaemon(True)
		self.tout.setDaemon(True)
		print 'added session: ', self.sid, self.t, self.h, self.p
	
	def tick(self):
		self.last = True
	
	def inThr(self):
		while self.work:
			try:
				data = self.sock.recv(REQUEST_BUFF_SIZE)
			except socket.error:
				data = None
			if data:
				#data = binascii.b2a_hex(data)
				self.q.qin.put(data)
			else:
				self.q.qin.put(None)
				self.terminate()
	
	def outThr(self):
		while self.work:
			try:
				data = self.q.qout.get(True, QUEUE_TIMEOUT)
			except Queue.Empty:
				data = None
			if data:
				try:
					#data = base64.binascii.a2b_hex(data)
					self.sock.send(data)
				except (TypeError, socket.error):
					data = None
	
	def terminate(self):
		if self.work:
			print 'terminating session: ', self.sid
			self.sock.close()
		self.work = False
	
	def start(self):
		try:
			self.sock.connect((self.h, self.p))
			self.tin.start()
			self.tout.start()
		except socket.error:
			print 'socketerror for session:', self.sid
			self.terminate()
	
	def clean(self):
		self.cleanable = True

class sessionList:
	l = {};
	
	def __init__(self):
		thr = threading.Thread(target=self.check)
		thr.setDaemon(True)
		thr.start()
	
	def add(self,sessionID, socketType, host, port):
		if not self.l.has_key(sessionID):
			self.l[sessionID] = sessionItem(sessionID, socketType, host, port)
			self.l[sessionID].start()
	
	def rm(self,sessionID):
		self.l[sessionID].terminate()
		del self.l[sessionID]
	
	def get(self,sessionID):
		try:
			i = self.l[sessionID]
			i.tick()
			return i
		except KeyError:
			return None
	
	def check(self):
		while True:
			time.sleep(CONECTION_TIMEOUT)
			cl = []
			for i in self.l:
				if self.l[i].last:
					self.l[i].last = False
				else:
					if not self.l[i].work:
						self.l[i].clean()
					else:
						self.l[i].terminate()
				if self.l[i].cleanable:
					cl.append(i)
				
			for i in cl:
				self.rm(i)
			cl = None

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
        self.socket = SSL.Connection(ctx, socket.socket(self.address_family, self.socket_type))
        self.server_bind()
        self.server_activate()


class myHTTPRequestHandler(BaseHTTPRequestHandler):
	def do_GET(self):
		if self.path.find('?') < 0:
			self.send_response(404)
			self.end_headers()
			self.wfile.write('404')
			return
		try:
			(stuff, args) = self.path.split('?',1)
		except ValueError: ## dummy
			args = self.path
		arglist = cgi.parse_qs(args)
		try:
			s = urllib.unquote(arglist['i'][0])
			sessionlist.add(s, arglist['t'][0], arglist['h'][0], arglist['p'][0])
			sitem = sessionlist.get(s)
		except KeyError:
			s = None
			sitem = None

		if sitem and sitem.work:
			try:
				if self.command == 'GET':
					# get data - GET
					try:
						mydata = urllib.unquote(arglist['d'][0])
						print 'rcv: ', mydata.strip()
						sitem.q.qout.put(httpdecode(mydata))
						item = True
					except KeyError:
						item = None
				else:
					# get data - POST
					try:
						mydata = ''
						clen = int(self.headers['Content-length'])
						mydata = self.rfile.read(clen)
						item = True
						sitem.q.qout.put(httpdecode(mydata))
					except KeyError:
						item = None
				try:
					if item:
						#item = sitem.q.qin.get(False)
						item = None
					else:
						item = sitem.q.qin.get(True, QUEUE_TIMEOUT)
				except AttributeError:
					item = None
					
				#if not item:
				#	sessionlist.rm(s)
			except (Queue.Empty, ):
				item = None
				
			self.send_response(200)
			self.end_headers()
			
			count = 0
			
			if item:
				print 'snd: ' , item.strip()
				self.wfile.write(httpencode(item))
				count = count + len(item)
				try:
					while count < REQUES_MAX_SIZE:
						item = sitem.q.qin.get(False)
						if item:
							print 'snd: ' , item.strip()
							self.wfile.write(httpencode(item))
							count = count + len(item)
				except Queue.Empty:
					item = None
		else:
			self.send_response(404)
			self.wfile.write('404')
			self.end_headers()
			if sitem:
				sitem.clean()
	
	def do_POST(self):
		self.do_GET()

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
	#	parser.add_option('-q', '--quiet', action="store_const", const=0, dest="v", default=1, help='quiet')
	#	parser.add_option('-v', '--verbose', action="store_const", const=1, dest="v", help='verbose')
	
	#parser.add_option('--root', action="store", dest="root", help='rootpath', default='/')
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
	
	hl = httpListener(options.port, '', options.ssl)
	hlThread = threading.Thread(target=hl.listen)
	hlThread.setDaemon(True)
	hlThread.start()
	
	input = sys.stdin.readline()
	while input:
		input = sys.stdin.readline()
	
	
	print 'end..'

main()