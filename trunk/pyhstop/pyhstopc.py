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

QUEUE_TIMEOUT = 2
DEFAULT_LISTENPORT = 9099
DEFAULT_URL = 'http://localhost:8090/'
DEFAULT_TARGET = 'localhost:8091'
REQUEST_BUFF_SIZE = 128
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
				item = base64.binascii.a2b_hex(item)
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
			data = binascii.b2a_hex(data)
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
	proxy = ''
	work = True
	sid = ''
	sl = None
	proxy_handler = None
	auth_handler = None
	opener = None
	
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
			pwmgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
			self.auth_handler = urllib2.HTTPBasicAuthHandler(pwmgr)
			try:
				self.auth_handler.add_password(None, self.url, self.auth.split(':',1)[0], self.auth.split(':',1)[1])
			except IndexError:
				self.auth = ''
			self.opener = urllib2.build_opener(self.proxy_handler, self.auth_handler)
			urllib2.install_opener(self.opener)
			
	
	def newSID(self):
		self.sid = myHash(time.time())
		print 'new: ', self.sid
	
	def setSL(self, sListener):
		self.sl = sListener
	
	def pushData(self):
		item = None
		while self.work:
			lastItem = item
			try:
				if lastItem:
					item = self.q.qin.get(False)
				else:
					item = self.q.qin.get(True, QUEUE_TIMEOUT)
			except (Queue.Empty, ):
				item = None
			#if not self.sl.c: continue
			while self.sid == '':
				print 'no sid'
				print 'item ', item
				time.sleep(QUEUE_TIMEOUT)
			#if self.sid == '': continue
			m = myHash(time.time())
			datalist = [('i', self.sid), ('t', self.destType), ('h', self.destHost), ('p', self.destPort), ('b', m)]
			if item:
				try:
					datalist.append(('d', item))
				except AttributeError:
					item = None
			myurl = self.url + '?' + urllib.urlencode(datalist)
			try:
				req = urllib2.Request(url=myurl,)
				f = urllib2.urlopen(req)
				ret = f.read()
				while ret:
					if ret and ret != '':
						print 'rcv: ', ret.strip()
						self.q.qout.put(ret)
					ret = f.read()
			except urllib2.HTTPError:
				self.sl.terminate()

	def connect(self):
		thr = threading.Thread(target=self.pushData)
		thr.setDaemon(True)
		thr.start()

class queues:
	qin = None
	qout = None
	def __init__(self):
		self.qin = Queue.Queue()
		self.qout = Queue.Queue()

def main():
	usage = "usage: %prog [options]"
	parser = OptionParser(usage=usage)
	parser.add_option('-t', '--tcp', action='store_const', dest='t', const='tcp', default='tcp', help='tcp mode (default)')
	#parser.add_option('-u', '--udp', action='store_const', dest='t', const='udp', help='udp mode')
	#	parser.add_option('-q', '--quiet', action="store_const", const=0, dest="v", default=1, help='quiet')
	#	parser.add_option('-v', '--verbose', action="store_const", const=1, dest="v", help='verbose')
	
	parser.add_option('-p', '--port', action="store", type='int', dest="port", help='port to listen (default: '+ str(DEFAULT_LISTENPORT) +')', default=DEFAULT_LISTENPORT)
	parser.add_option('--url', action="store", dest="url", help='URL of tunnelendpoint (default: '+ DEFAULT_URL +')', default=DEFAULT_URL)
	
	parser.add_option('-d', '--dest', action="store", dest="dest", help='destination to connect to (default ' + DEFAULT_TARGET + ')', default=DEFAULT_TARGET)
	parser.add_option('--proxy', action='store', dest='proxy', default='', help='proxy to use')
	parser.add_option('--auth', action='store', dest='auth', default='', help='auth with user:password')
	#parser.add_option('--no-proxy', action='store_true', dest='np', default=False, help='use no proxy (default: use proxy from env)')
	
	global options
	(options, args) = parser.parse_args()
	
	print 'start..'
	
	#if options.np:
	#	options.proxy = '-'
	
	sl = socketListener(options.port, options.t, options)
	sl.listen()
		
	input = sys.stdin.readline()
	while input:
		input = sys.stdin.readline()
		
	sl.terminate()
	print 'end..'

main()