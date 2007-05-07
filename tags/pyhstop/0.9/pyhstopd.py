#!/usr/bin/env python
############################################################################
#    Copyright (C) 2007 by Felix Bechstein                                 #
#    felix.bechstein@web.de                                                #
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
import SocketServer
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
#from pyhstop_common import httpencode, httpdecode
#import pyhstop_common
import zlib
import ConfigParser
import signal

QUEUE_TIMEOUT = 10
CONECTION_TIMEOUT = QUEUE_TIMEOUT * 2
DEFAULT_PORT = 9099
DEFAULT_CONF = 'pyhstop.conf'
REQUEST_BUFF_SIZE = 128
REQUES_MAX_SIZE = 2048
DEFAULT_CONF = 'pyhstop.conf'

#VERSION = pyhstop_common.VERSION
VERSION = '0.9'

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
	
	def __init__(self, sessionID, socketType, host, port, fromip):
		self.q = queues()
		self.sid = sessionID
		self.t = socketType
		self.p = int(port)
		self.h = host
		if self.t == 'udp':
			self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
		else:
			self.t = 'tcp'
			self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self.tin = threading.Thread(target=self.inThr)
		self.tout = threading.Thread(target=self.outThr)
		self.tin.setDaemon(True)
		self.tout.setDaemon(True)
		print 'added session: ', self.sid, self.t, self.h, self.p, 'from ', fromip
	
	def tick(self):
		self.last = True
	
	def inThr(self):
		while self.work:
			try:
				data = self.sock.recv(REQUEST_BUFF_SIZE)
			except socket.error:
				data = None
			if data:
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
					if self.t == 'udp':
						self.sock.sendto(data, (self.h, self.p))
					else:
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
	
	def add(self,sessionID, socketType, host, port, fromip):
		if not self.l.has_key(sessionID):
			self.l[sessionID] = sessionItem(sessionID, socketType, host, port, fromip)
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

class myHTTPServer(SocketServer.ThreadingMixIn ,HTTPServer):
	pass

class SecureHTTPServer(myHTTPServer):
    def __init__(self, server_address, HandlerClass):
	from OpenSSL import SSL
        BaseServer.__init__(self, server_address, HandlerClass)
        ctx = SSL.Context(SSL.SSLv23_METHOD)
        ctx.use_privatekey_file (keyfile)
        ctx.use_certificate_file(certfile)
        self.socket = SSL.Connection(ctx, socket.socket(self.address_family, self.socket_type))
        self.server_bind()
        self.server_activate()


class myHTTPRequestHandler(BaseHTTPRequestHandler):
	def do_GET(self):
		if self.path.find('?') < 0:
			if options.hide:
				self.send_response(404)
				self.send_header('Content-length', '3')
				self.send_header("Content-type", "text/html")
				self.end_headers()
				self.wfile.write('404')
			else:
				self.send_response(302, "Moved")
				self.send_header('Content-length', '0')
				self.send_header("Content-type", "text/html")
				self.send_header("Location", 'http://hstop.berlios.de/')
				self.end_headers()
			return
		try:
			(stuff, args) = self.path.split('?',1)
		except ValueError: ## dummy
			args = self.path
		arglist = cgi.parse_qs(args)
		try:
			s = urllib.unquote(arglist['i'][0])
			sitem = sessionlist.get(s)
			if not sitem:
				sessionlist.add(s, arglist['t'][0], arglist['h'][0], arglist['p'][0], self.client_address)
				sitem = sessionlist.get(s)
		except KeyError:
			s = None
			sitem = None

		if sitem and sitem.work:
			try:
				item = sitem.q.qin.get(True, QUEUE_TIMEOUT)
			except (Queue.Empty, ):
				item = None
				
			if item:
				try:
					while len(item) < REQUES_MAX_SIZE:
						item = item + sitem.q.qin.get(False)
				except (Queue.Empty, TypeError ):
					pass
				item1 = zlib.compress(item,9)
				if len(item1) < len(item):
					item = item1
					item1 = None
				#item = httpencode(item)
				self.send_response(200)
				self.send_header('Content-length', str(len(item)))
				self.end_headers()
				#print 'snd: ' , item.strip()
				self.wfile.write(item)
				
			else:
				self.send_response(200)
				self.send_header('Content-length', '0')
				self.end_headers()
		else:
			self.send_response(404)
			self.send_header('Content-length', '3')
			self.send_header("Content-type", "text/html")
			self.end_headers()
			self.wfile.write('404')
			if sitem:
				sitem.clean()
	
	def do_POST(self):
		if self.path.find('?') < 0:
			self.send_response(404)
			self.send_header('Content-length', '3')
			self.send_header("Content-type", "text/html")
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
			sitem = sessionlist.get(s)
			if not sitem:
				sessionlist.add(s, arglist['t'][0], arglist['h'][0], arglist['p'][0], self.client_address)
				sitem = sessionlist.get(s)
		except KeyError:
			s = None
			sitem = None

		if sitem and sitem.work:
			try:
				mydata = ''
				clen = int(self.headers['Content-length'])
				mydata = self.rfile.read(clen)
				item = True
				#mydata = httpdecode(mydata)
				try:
					mydata1 = zlib.decompress(mydata)
					mydata = mydata1
					mydata1 = None
				except zlib.error:
					pass
				sitem.q.qout.put(mydata)
			except KeyError:
				item = None
				
			self.send_response(200)
			self.send_header('Content-length', '0')
			self.end_headers()
			
		else:
			self.send_response(404)
			self.send_header('Content-length', '3')
			self.send_header("Content-type", "text/html")
			self.end_headers()
			self.wfile.write('404')
			if sitem:
				sitem.clean()

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
	ServerClass = myHTTPServer
	httpd = None
	status = True
	def __init__(self):
		self.p = options.port
		self.r = '' # TODO: fix this
		self.s = options.ssl
		if self.s:
			self.HandlerClass = SecureHTTPRequestHandler
			self.ServerClass = SecureHTTPServer
		self.HandlerClass.protocol_version = 'HTTP/1.1'
		try:
			self.httpd = self.ServerClass(('', self.p), self.HandlerClass)
		except socket.error:
			print 'could not bind port!'
			if self.p < 1024: print 'you have to be root, to bind ports less than 1024'
			self.status = False
	
	def listen(self):
		self.httpd.serve_forever()

def main():
	signal.signal(signal.SIGHUP, signal.SIG_IGN)
	usage = "usage: %prog [options]"
	parser = OptionParser(usage=usage)
	
	parser.add_option('-c', '--config', action='store', dest='config', default=DEFAULT_CONF, help='load parameters from configfile (default: ' + DEFAULT_CONF + ')')
	
	#	parser.add_option('-q', '--quiet', action="store_const", const=0, dest="v", default=1, help='quiet')
	#	parser.add_option('-v', '--verbose', action="store_const", const=1, dest="v", help='verbose')
	
	#parser.add_option('--root', action="store", dest="root", help='rootpath', default='/')
	parser.add_option('-s', '--ssl', action='store_true', dest='ssl', help='use https')
	parser.add_option('-n', '--no-ssl', action='store_false', dest='ssl', help='do not use https (default)')
	parser.add_option('-p', '--port', action='store', dest='port', type='int', help='port to listen')
	parser.add_option('--cert', action='store', dest='cert', default='cert.pem', help='certificate to use')
	parser.add_option('--key', action='store', dest='key', default='key.pem', help='key to use')
	parser.add_option('--hide', action='store_true', dest='hide', help='hides the tunnelserver for other clients')
	
	global options
	(options, args) = parser.parse_args()
	
	cparser = ConfigParser.ConfigParser(defaults={
		'ssl': False,
		'port': 80,
		'cert': 'cert.pem',
		'key': 'key.pem',
		'hide': False
		})

	cparser.read(options.config)
	
	if cparser.has_section('pyhstopd'):
		if not options.ssl:	options.ssl = cparser.getboolean('pyhstopd', 'ssl')
		if not options.port:	options.port = cparser.getint('pyhstopd', 'port')
		if not options.cert:	options.cert = cparser.get('pyhstopd', 'cert')
		if not options.key:	options.key = cparser.get('pyhstopd', 'key')
		try:
			if not options.hide:	options.hide = cparser.getboolean('pyhstopd', 'hide')
		except TypeError:
			options.hide = False

	cparser = None

	print 'pyhstopd Version: ' + VERSION
	print 'terminate with EOF'
	print 'start..'
	
	q = queues()
	
	if options.ssl:
		from OpenSSL import SSL
	global keyfile, certfile
	keyfile = options.key
	certfile = options.cert
	
	hl = httpListener()
	if not hl.status:
		return -1
	else:
		hlThread = threading.Thread(target=hl.listen)
		hlThread.setDaemon(True)
		hlThread.start()
		try:
			input = sys.stdin.readline()
			while input:
				input = sys.stdin.readline()
		except KeyboardInterrupt:
			print 'interrupted'
		
	print 'end..'

main()