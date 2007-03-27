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
import Queue
import threading
from optparse import OptionParser
import sys
import urllib
import urllib2
import time
import md5
import base64, binascii
#from pyhstop_common import httpencode, httpdecode
#import pyhstop_common
import ConfigParser
try:
	import pycurl
	USE_CURL = True
except:
	print 'could not import pycurl. fallback to urllib2'
	USE_CURL = False

#VERSION = pyhstop_common.VERSION
VERSION = 'HEAD'

QUEUE_TIMEOUT = 10
DEFAULT_LISTENPORT = 9099
DEFAULT_URL = 'http://localhost:8090/'
DEFAULT_TARGET = 'localhost:8091'
DEFAULT_CONF = 'pyhstop.conf'
REQUEST_BUFF_SIZE = 128
REQUES_MAX_SIZE = 2048

options = None

def myHash(input):
	out = ''
	m = md5.new(str(input))
	out = m.hexdigest()
	return out[0:8]

def nullFunc(buf):
	pass

class udpConnectionWrapper:
	qin = None
	sockW = None
	addr = ('',0)
	connected = True
	
	def __init__(self, sockW, addr):
		self.qin = Queue.Queue()
		self.addr = addr
		self.sockW = sockW
	
	def recv(self, maxsize = 1024):
		d = None
		while self.connected:
			try:
				d = self.qin.get(True, QUEUE_TIMEOUT)
				return d
			except Queue.Empty:
				pass
	
	def send(self, data):
		i = (self.addr, data)
		print i
		self.sockW.qout.put(i)
	
	def close(self):
		self.connected = False
		self.sockW.rmCon(self.addr)

class udpWrapper:
	connections = None
	sock = None
	qout = None
	qaccept = None
	work = True
	thrIn = None
	thrOut = None
	
	def __init__(self):
		self.connections = []
		self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
		self.qout = Queue.Queue()
		self.qaccept = Queue.Queue()
		thrIn = threading.Thread(target=self.fetch)
		thrOut = threading.Thread(target=self.push)
		thrIn.setDaemon(True)
		thrOut.setDaemon(True)
		thrIn.start()
		thrOut.start()
	
	def listen(self, maxopen = 5):
		pass
	
	def accept(self):
		c = self.qaccept.get(True)
		return (c, c.addr)
	
	def bind(self, addr):
		self.sock.bind(addr)
	
	def close(self):
		self.work = False
	
	def getCon(self, addr):
		for c in self.connections:
			if c.addr == addr:
				return c
		return None
	
	def addCon(self, con):
		self.connections.append(con)
	
	def rmCon(self, addr):
		for c in self.connections:
			if c.addr == addr:
				self.connections.remove(c)
				break
	
	def push(self):
		while self.work:
			try:
				addr, data = self.qout.get(True, QUEUE_TIMEOUT)
				print addr, data
				self.sock.sendto(data, addr)
			except Queue.Empty:
				pass
	
	def fetch(self):
		while self.work:
			data, addr = self.sock.recvfrom(REQUES_MAX_SIZE)
			c = self.getCon(addr)
			if not c:
				c = udpConnectionWrapper(self, addr)
				self.addCon(c)
				self.qaccept.put(c)
			print addr, data
			c.qin.put(data)

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
		self.tc = tunnelClient(self.q)
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
			#print 'snd: ', data.strip()
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

	def __init__(self):
		self.t = options.mode
		self.p = options.port
		
		if self.t =='udp':
			self.s = udpWrapper()
		else:
			self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

	def do_listen(self):
		thr = []
		print 'listen.. port =', self.p
		self.s.bind(('', self.p))
		while self.work:
			self.s.listen(1)
			conn, addr = self.s.accept()
			thr.append(socketSession(self.s, conn, addr))

	def listen(self):
		thr = threading.Thread(target=self.do_listen)
		thr.setDaemon(True)
		thr.start()

	def terminate(self):
		self.work = False
		self.isData = False
		print 'socketlistener terminated'

class curlHeader:
	headers = {}
	
	def add_header(self, key, val):
		self.headers[key] = val
	
	def _make_headers(self):
        	headers = []
		for k,v in self.headers.iteritems():
			headers.append(("%s: %s" % (k, v)))
		headers.append("Expect:")
		return headers

def curlFetch(buf):
	return buf

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
	c = None
	cGET = None
	cPUT = None
	head = None
		
	def __init__(self, queues):
		self.q = queues
		self.url = options.url
		(self.destHost, self.destPort) = options.dest.split(':')
		self.destType = options.mode
		self.proxy = options.proxy
		self.auth = options.auth
		self.sid = ''
		self.headers = {}
		self.head = curlHeader()
		
		if USE_CURL:
			self.cGET = pycurl.Curl()
			self.cPUT = pycurl.Curl()
		
		if self.proxy != '':
			if USE_CURL:
				if self.proxy == '-':
					self.cGET.setopt(self.cGET.PROXY, '')
					self.cPUT.setopt(self.cPUT.PROXY, '')
				else:
					self.cGET.setopt(self.cGET.PROXY, self.proxy)
					self.cPUT.setopt(self.cPUT.PROXY, self.proxy)
			else:
				self.proxy_handler = urllib2.ProxyHandler({'http': self.proxy, 'https': self.proxy})
				self.opener = urllib2.build_opener(self.proxy_handler)
				urllib2.install_opener(self.opener)
		
		if self.auth != '':
			try:
				self.authUsr = self.auth.split(':',1)[0]
				self.authPwd = self.auth.split(':',1)[1]
				base64string = base64.encodestring('%s:%s' % (self.authUsr, self.authPwd))[:-1]
				self.head.add_header("Authorization", "Basic %s" % base64string)
			except IndexError:
				self.authUsr = None
				self.authPwd = None
		
		if USE_CURL:
			self.cGET.setopt(self.cGET.NOSIGNAL, 1)
			self.cPUT.setopt(self.cPUT.NOSIGNAL, 1)
			self.cGET.setopt(self.cGET.SSL_VERIFYPEER, 0) # this is a dirty hack against test-certs..
			self.cPUT.setopt(self.cPUT.SSL_VERIFYPEER, 0) # we should make an option for that!
			if options.verbose:
				self.cGET.setopt(self.cGET.VERBOSE, 1)
				self.cPUT.setopt(self.cPUT.VERBOSE, 1)
			if options.agent != '':
				self.cGET.setopt(self.cGET.USERAGENT, options.agent)
				self.cPUT.setopt(self.cPUT.USERAGENT, options.agent)
			if self.head:
				self.cGET.setopt(self.cGET.HTTPHEADER, self.head._make_headers())
				self.cPUT.setopt(self.cPUT.HTTPHEADER, self.head._make_headers())
		
	
	def newSID(self):
		self.sid = myHash(time.time())
		print 'new: ', self.sid
	
	def setSL(self, sListener):
		self.sl = sListener
	
	def fetchData(self):
		first = True
		while self.work:
			while self.sid == '':
				print 'no sid'
				time.sleep(QUEUE_TIMEOUT)
			m = myHash(time.time())
			datalist = [('i', self.sid), ('b', m)]
			if first:
				datalist.extend((('t', self.destType), ('h', self.destHost), ('p', self.destPort)))
				first = False
			myurl = self.url + '?' + urllib.urlencode(datalist)
			self.cGET.setopt(self.cGET.URL, myurl)
			fetched = False
			wrt = queueWrite(self.q.qout)
			self.cGET.setopt(self.cGET.WRITEFUNCTION, wrt.write)
			self.cGET.perform()
			resp = self.cGET.getinfo(pycurl.RESPONSE_CODE)
			if resp == 200:
				fetched = True
			else:
				print resp
				fetched = False
			
			if not fetched:
				self.sl.terminate()
		self.cGET.close()
		self.cGET = None
		
	
	def pushData(self):
		item = None
		first = True
		while self.work:
			try:
				item = self.q.qin.get(True, QUEUE_TIMEOUT)
			except (Queue.Empty, ):
				continue
				item = None
			while self.sid == '':
				print 'no sid'
				print 'item ', item
				time.sleep(QUEUE_TIMEOUT)
			post = None
			if item:
				try:
					while len(item) < REQUES_MAX_SIZE:
						item = item + self.q.qin.get(False)
				except Queue.Empty:
					pass
				#post = httpencode(item)
				post = item
			else:
				continue
			
			m = myHash(time.time())
			datalist = [('i', self.sid), ('b', m)]
			if first:
				datalist.extend((('t', self.destType), ('h', self.destHost), ('p', self.destPort)))
				first = False
			
			myurl = self.url + '?' + urllib.urlencode(datalist)
			
			self.cPUT.setopt(self.cPUT.URL, myurl)
			fetched = False
			self.cPUT.setopt(self.cPUT.WRITEFUNCTION, nullFunc)
			
			self.cPUT.setopt(self.cPUT.POST, 1)
			self.cPUT.setopt(self.cPUT.POSTFIELDS, post)
			self.cPUT.setopt(self.cPUT.POSTFIELDSIZE, len(post))
			
			self.cPUT.perform()
			resp = self.cPUT.getinfo(pycurl.RESPONSE_CODE)
			if resp == 200:
				fetched = True
			else:
				print resp
				fetched = False
			
			if not fetched:
				self.sl.terminate()
		
		self.cPUT.close()
		self.cPUT=None
	
	
	def fetchData_NOCURL(self):
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
						#print 'rcv: ', ret.strip()
						self.q.qout.put(httpdecode(ret))
					ret = f.read()
			else:
				self.sl.terminate()

	
	def pushData_NOCURL(self):
		item = None
		while self.work:
			try:
				item = self.q.qin.get(True, QUEUE_TIMEOUT)
			except (Queue.Empty, ):
				continue
				item = None
			while self.sid == '':
				print 'no sid'
				print 'item ', item
				time.sleep(QUEUE_TIMEOUT)
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
						#print 'rcv: ', ret.strip()
						self.q.qout.put(httpdecode(ret))
					ret = f.read()
			else:
				self.sl.terminate()

	def connect(self):
		if USE_CURL:
			thr = threading.Thread(target=self.pushData)
			thr2 = threading.Thread(target=self.fetchData)
		else:
			thr = threading.Thread(target=self.pushData_NOCURL)
			thr2 = threading.Thread(target=self.fetchData_NOCURL)
		#thr.setDaemon(True)
		#thr2.setDaemon(True)
		thr.start()
		thr2.start()

class queues:
	qin = None
	qout = None
	def __init__(self):
		self.qin = Queue.Queue()
		self.qout = Queue.Queue()

class queueWrite:
	q = None
	def __init__(self, q):
		self.q = q
	
	def write(self, buf):
		self.q.put(buf)

def main():
	usage = "usage: %prog [options]"
	parser = OptionParser(usage=usage)
	
	#	parser.add_option('-q', '--quiet', action="store_const", const=0, dest="v", default=1, help='quiet')
	
	parser.add_option('-c', '--config', action='store', dest='config', default=DEFAULT_CONF, help='load parameters from configfile (default: ' + DEFAULT_CONF + ')')
	
	parser.add_option('-t', '--tcp', action='store_const', dest='mode', const='tcp', help='tcp mode (default)')
	
	parser.add_option('-u', '--udp', action='store_const', dest='mode', const='udp', help='udp mode')
		
	parser.add_option('-p', '--port', action="store", type='int', dest="port", help='port to listen (default: '+ str(DEFAULT_LISTENPORT) +')')
	
	parser.add_option('--url', action="store", dest="url", help='URL of tunnelendpoint (default: '+ DEFAULT_URL +')')
	
	parser.add_option('-d', '--dest', action="store", dest="dest", help='destination to connect to (default ' + DEFAULT_TARGET + ')')
	
	parser.add_option('--proxy', action='store', dest='proxy', help='proxy to use')
	
	parser.add_option('--auth', action='store', dest='auth', help='auth with user:password')
	
	parser.add_option('-a', '--agent', action='store', dest='agent', help='fake useragent')
	
	parser.add_option('-v', '--verbose', action='store_const', dest='verbose', const=1, help='verbose')
	
	
	#parser.add_option('--no-proxy', action='store_true', dest='np', default=False, help='use no proxy (default: use proxy from env)')
	
	global options
	(options, args) = parser.parse_args()
	
	cparser = ConfigParser.ConfigParser(defaults={
		'mode': 'tcp',
		'port': DEFAULT_LISTENPORT,
		'url': DEFAULT_URL,
		'dest': DEFAULT_TARGET,
		'auth': '',
		'proxy': '',
		'agent': '',
		'verbose': 0
		})

	cparser.read(options.config)
	
	if cparser.has_section('pyhstopc'):
		if not options.mode:	options.mode = cparser.get('pyhstopc', 'mode')
		if not options.port:	options.port = cparser.getint('pyhstopc', 'port')
		if not options.url:	options.url = cparser.get('pyhstopc', 'url')
		if not options.dest:	options.dest = cparser.get('pyhstopc', 'dest')
		if not options.auth:	options.auth = cparser.get('pyhstopc', 'auth')
		if not options.agent:	options.agent = cparser.get('pyhstopc', 'agent')
		if not options.proxy:	options.proxy = cparser.get('pyhstopc', 'proxy')
		try:
			if not options.verbose:	options.verbose = cparser.getint('pyhstopc', 'verbose')
		except TypeError:
			options.verbose = 0
	
	cparser = None
		
	print 'pyhstopc Version: ' + VERSION
	print 'terminate with EOF'

	print 'start..'
	
	if USE_CURL:
		pycurl.global_init(pycurl.GLOBAL_ALL)
	
	sl = socketListener()
	sl.listen()
		
	input = sys.stdin.readline()
	while input:
		input = sys.stdin.readline()
		
	sl.terminate()
	if USE_CURL:
		pycurl.global_cleanup()
	print 'end..'

main()