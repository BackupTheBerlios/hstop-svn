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
import Queue
import threading
from optparse import OptionParser
import sys
import urllib
import urllib2
import time
import md5
import base64, binascii
from pyhstop_common import httpencode, httpdecode
import ConfigParser

QUEUE_TIMEOUT = 2
DEFAULT_LISTENPORT = 9099
DEFAULT_URL = 'http://localhost:8090/'
DEFAULT_TARGET = 'localhost:8091'
DEFAULT_CONF = 'pyhstop.conf'
REQUEST_BUFF_SIZE = 128
REQUES_MAX_SIZE = 1024
SPLITCHAR = '-'

options = None

def myHash(input):
	out = ''
	m = md5.new(str(input))
	out = m.hexdigest()
	return out[0:8]

class socketSession:
	tout = None
	work = True
	isData = True
	killSock = False
	q = None
	tc = None
	c = False
	conn = False
	hthr = None
	addr = None

	def __init__(self, sock, conn, addr):
		self.addr = addr
		self.s = sock
		self.q = queues()
		self.tc = tunnelClient(self.q, options.url, options.dest.split(':')[0], options.dest.split(':')[1], options.t, options.proxy, options.auth)
		self.tc.setSL(self)
		self.conn = conn
		self.hthr = threading.Thread(target=self.handleSession)
		self.hthr.setDaemon(True)
		self.hthr.start()
		self.tc.connect()

	def send(self):
		while self.isData:
			try:
				item = self.q.qout.get(True, QUEUE_TIMEOUT)
				#item = base64.binascii.a2b_hex(item)
				self.conn.send(item)
			except (Queue.Empty, TypeError):
				item = None
		print 'break snd'

	def terminate(self):
		self.work = False
		self.isData = False
		self.tc.work = False
		self.killSock = True
		print 'socketSession terminated'

	def handleSession(self):
		print 'Connected by', self.addr
		self.tc.newSID()
		self.c = True
		tout = threading.Thread(target=self.send)
		tout.setDaemon(True)
		tout.start()
		if self.work: self.isData = True
		self.killSock = False
		while self.isData and not self.killSock:
			data = self.conn.recv(REQUEST_BUFF_SIZE)
			if not data:
				self.isData = False
				print 'break rcv'
				break
			#data = binascii.b2a_hex(data)
			print 'snd: ', data.strip()
			try:
				self.q.qin.put(data)
			except Queue.Full:
				data = None
			self.c = False
		self.conn.close()
		self.terminate()

class socketListener:
	p = DEFAULT_LISTENPORT
	t = 'tcp'
	s = None
	tout = None
	work = True
	isData = True
	q = None
	c = False
	opts = None

	def __init__(self, sPort, sType, options):
		self.t = sType
		self.p = sPort
		
		#if self.t =='tcp':
		self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		#else:
		#	self.s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
		self.opts = options

	def do_listen(self):
		thr = []
		print 'listen.. port=', self.p
		self.s.bind(('', self.p))
		while self.work:
			self.s.listen(1)
			conn, addr = self.s.accept()
			thr.append(socketSession(self.s, conn, addr))
			continue

	def listen(self):
		thr = threading.Thread(target=self.do_listen)
		thr.setDaemon(True)
		thr.start()

	def terminate(self):
		self.work = False
		self.isData = False
		print 'socketlistener terminated'

class tunnelClient:
	q = None
	url = ''
	destHost = ''
	destPort = 0
	auth = ''
	authUsr = None
	authPwd = None
	proxy = ''
	work = True
	sid = ''
	sl = None
	proxy_handler = None
	auth_handler = None
	opener = None
	needAuth = False
	
	def __init__(self, queues, url, host, port, type, proxy, auth):
		self.q = queues
		self.url = url
		self.destHost = host
		self.destPort = port
		self.destType = type
		self.proxy = proxy
		self.auth = auth
		self.sid = ''
		if self.proxy != '':
			self.proxy_handler = urllib2.ProxyHandler({'http': self.proxy, 'https': self.proxy})
			self.opener = urllib2.build_opener(self.proxy_handler)
			urllib2.install_opener(self.opener)
		if self.auth != '':
			try:
				self.authUsr = self.auth.split(':',1)[0]
				self.authPwd = self.auth.split(':',1)[1]
			except IndexError:
				self.authUsr = None
				self.authPwd = None			
	
	def newSID(self):
		self.sid = myHash(time.time())
		print 'new: ', self.sid
	
	def setSL(self, sListener):
		self.sl = sListener
	
	def fetchData(self):
		while self.work:
			while self.sid == '':
				print 'no sid'
				time.sleep(QUEUE_TIMEOUT)
			m = myHash(time.time())
			datalist = [('i', self.sid), ('t', self.destType), ('h', self.destHost), ('p', self.destPort), ('b', m)]
			myurl = self.url + '?' + urllib.urlencode(datalist)
			fetched = False
			try:
				req = urllib2.Request(url=myurl)
				if self.needAuth and self.authUsr and self.authPwd:
					base64string = base64.encodestring('%s:%s' % (self.authUsr, self.authPwd))[:-1]
					req.add_header("Authorization", "Basic %s" % base64string)
				f = urllib2.urlopen(req)
				fetched = True
			except urllib2.HTTPError, e:
				if e.code == 401:
					self.needAuth = True
					try:
						if self.authUsr and self.authPwd:
							base64string = base64.encodestring('%s:%s' % (self.authUsr, self.authPwd))[:-1]
							req.add_header("Authorization", "Basic %s" % base64string)
							f = urllib2.urlopen(req)
							fetched = True
					except urllib2.HTTPError:
						fetched = False
			if fetched:
				ret = f.read()
				while ret:
					if ret and ret != '':
						print 'rcv: ', ret.strip()
						self.q.qout.put(httpdecode(ret))
					ret = f.read()
			else:
				self.sl.terminate()

	
	def pushData(self):
		item = None
		while self.work:
			#lastItem = item
			try:
			#	if lastItem:
			#		item = self.q.qin.get(False)
			#	else:
				item = self.q.qin.get(True, QUEUE_TIMEOUT)
			except (Queue.Empty, ):
				continue
				item = None
			#if not self.sl.c: continue
			while self.sid == '':
				print 'no sid'
				print 'item ', item
				time.sleep(QUEUE_TIMEOUT)
			#if self.sid == '': continue
			m = myHash(time.time())
			datalist = [('i', self.sid), ('t', self.destType), ('h', self.destHost), ('p', self.destPort), ('b', m)]
			post = None
			if item:
				try:
					try:
						while len(item) < REQUES_MAX_SIZE:
							item = item + self.q.qin.get(False)
					except Queue.Empty:
						pass
					post = httpencode(item)
				except AttributeError:
					item = None
			else:
				continue
			myurl = self.url + '?' + urllib.urlencode(datalist)
			fetched = False
			try:
				req = urllib2.Request(url=myurl,data=post)
				if self.needAuth and self.authUsr and self.authPwd:
					base64string = base64.encodestring('%s:%s' % (self.authUsr, self.authPwd))[:-1]
					req.add_header("Authorization", "Basic %s" % base64string)
				f = urllib2.urlopen(req)
				fetched = True
			except urllib2.HTTPError, e:
				if e.code == 401:
					self.needAuth = True
					try:
						if self.authUsr and self.authPwd:
							base64string = base64.encodestring('%s:%s' % (self.authUsr, self.authPwd))[:-1]
							req.add_header("Authorization", "Basic %s" % base64string)
							f = urllib2.urlopen(req)
							fetched = True
					except urllib2.HTTPError:
						fetched = False
			if fetched:
				ret = f.read()
				while ret:
					if ret and ret != '':
						print 'rcv: ', ret.strip()
						self.q.qout.put(httpdecode(ret))
					ret = f.read()
			else:
				self.sl.terminate()

	def connect(self):
		thr = threading.Thread(target=self.pushData)
		thr.setDaemon(True)
		thr2 = threading.Thread(target=self.fetchData)
		thr2.setDaemon(True)
		thr.start()
		thr2.start()
		

class queues:
	qin = None
	qout = None
	def __init__(self):
		self.qin = Queue.Queue()
		self.qout = Queue.Queue()

def main():
	usage = "usage: %prog [options]"
	parser = OptionParser(usage=usage)
	
	#	parser.add_option('-q', '--quiet', action="store_const", const=0, dest="v", default=1, help='quiet')
	#	parser.add_option('-v', '--verbose', action="store_const", const=1, dest="v", help='verbose')
	
	parser.add_option('-c', '--config', action='store', dest='config', default=DEFAULT_CONF, help='load parameters from configfile (default: ' + DEFAULT_CONF + ')')
	
	parser.add_option('-t', '--tcp', action='store_const', dest='mode', const='tcp', help='tcp mode (default)')
	
	#parser.add_option('-u', '--udp', action='store_const', dest='t', const='udp', help='udp mode')
		
	parser.add_option('-p', '--port', action="store", type='int', dest="port", help='port to listen (default: '+ str(DEFAULT_LISTENPORT) +')')
	
	parser.add_option('--url', action="store", dest="url", help='URL of tunnelendpoint (default: '+ DEFAULT_URL +')')
	
	parser.add_option('-d', '--dest', action="store", dest="dest", help='destination to connect to (default ' + DEFAULT_TARGET + ')')
	
	parser.add_option('--proxy', action='store', dest='proxy', help='proxy to use')
	
	parser.add_option('--auth', action='store', dest='auth', help='auth with user:password')
	
	#parser.add_option('--no-proxy', action='store_true', dest='np', default=False, help='use no proxy (default: use proxy from env)')
	
	global options
	(options, args) = parser.parse_args()
	
	cparser = ConfigParser.ConfigParser(defaults={
		'mode': 'tcp',
		'port': DEFAULT_LISTENPORT,
		'url': DEFAULT_URL,
		'dest': DEFAULT_TARGET,
		'auth': '',
		'proxy': ''
		})

	cparser.read(options.config)
	
	if cparser.has_section('pyhstopc'):
		if not options.mode:	options.mode = cparser.get('pyhstopc', 'mode')
		if not options.port:	options.port = cparser.getint('pyhstopc', 'port')
		if not options.url:	options.url = cparser.get('pyhstopc', 'url')
		if not options.dest:	options.dest = cparser.get('pyhstopc', 'dest')
		if not options.auth:	options.auth = cparser.get('pyhstopc', 'auth')
		if not options.proxy:	options.proxy = cparser.get('pyhstopc', 'proxy')
		
	print 'start..'
	
	#if options.np:
	#	options.proxy = '-'
	
	sl = socketListener(options.port, options.mode, options)
	sl.listen()
		
	input = sys.stdin.readline()
	while input:
		input = sys.stdin.readline()
		
	sl.terminate()
	print 'end..'

main()