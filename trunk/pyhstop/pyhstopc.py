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

QUEUE_TIMEOUT = 3
DEFAULT_LISTENPORT = 9099
DEFAULT_URL = 'http://localhost:8090/'
DEFAULT_TARGET = 'localhost:8091'
REQUEST_BUFF_SIZE = 128
SPLITCHAR = '-'

def myHash(input):
	out = ''
	m = md5.new(str(input))
	out = m.hexdigest()
	return out[0:8]

class socketListener:
	p = DEFAULT_LISTENPORT
	t = 'tcp'
	s = None
	#tin = None
	tout = None
	work = True
	isData = True
	killSock = False
	q = None
	tc = None
	c = False
	
	
	def __init__(self, tc, sPort, queues, sType = 'tcp'):
		self.t = sType
		self.p = sPort
		self.q = queues
		self.tc = tc
		tc.setSL(self)
		#if self.t =='tcp':
		self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		#else:
		#	self.s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	
	def send(self, c):
		while self.isData:
			try:
				item = self.q.qout.get(True, QUEUE_TIMEOUT)
				c.send(item)
			except Queue.Empty:
				item = None
		print 'break snd'
	
	def do_listen(self):
		print 'listen.. port=', self.p
		self.s.bind(('', self.p))
		while self.work:
			self.s.listen(1)
			conn, addr = self.s.accept()
			print 'Connected by', addr
			self.q = queues()
			self.tc.newSID()
			self.c = True
			tout = threading.Thread(target=self.send,args=(conn,))
			tout.setDaemon(True)
			tout.start()
			if self.work: self.isData = True
			self.killSock = False
			while self.isData and not self.killSock:
				data = conn.recv(REQUEST_BUFF_SIZE)
				if not data:
					self.isData = False
					print 'break rcv'
					break
				print 'snd: ', data.strip()
				try:
					self.q.qin.put(data)
					#conn.send(data)
				except Queue.Full:
					data = None
			self.c = False
			conn.close()
	
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
	proxy = ''
	work = True
	sid = ''
	sl = None
	
	def __init__(self, queues, url, host, port, type, proxy):
		self.q = queues
		self.url = url
		self.destHost = host
		self.destPort = port
		self.destType = type
		self.proxy = proxy
		self.sid = ''
	
	def newSID(self):
		self.sid = myHash(time.time())
	
	def setSL(self, sListener):
		self.sl = sListener
	
	def pushData(self):
		while self.work:
			try:
				item = self.q.qin.get(True, QUEUE_TIMEOUT)
			except (Queue.Empty, ):
				item = None
			if not self.sl.c: continue
			if self.sid == '': continue
			m = myHash(time.time())
			datalist = [('i', self.sid), ('t', self.destType), ('h', self.destHost), ('p', self.destPort), ('b', m)]
			if item:
				#try:
					datalist.append(('d', item))
				#except AttributeError:
				#	item = None
			myurl = self.url + '?' + urllib.urlencode(datalist)
			try:
				#print 'req url'
				req = urllib2.Request(url=myurl,)
				f = urllib2.urlopen(req)
				ret = f.read()
				while ret:
					if ret and ret != '':
						print 'rcv: ', ret.strip()
						self.q.qout.put(ret)
					ret = f.read()
			except urllib2.HTTPError:
				self.sl.killSock = True
			
	def connect(self):
		thr = threading.Thread(target=self.pushData)
		thr.setDaemon(True)
		thr.start()

class queues:
	qin = Queue.Queue()
	qout = Queue.Queue()

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
	#parser.add_option('--proxy', action='store', dest='proxy', help='proxy to use')

	(options, args) = parser.parse_args()
	
	print 'start..'
	
	q = queues()
	
			
	tc = tunnelClient(q, options.url, options.dest.split(':')[0], options.dest.split(':')[1], options.t, '')
	
	sl = socketListener(tc, options.port,q, sType=options.t)
	sl.listen()
		
	tc.connect()
	
	sys.stdin.readline()
	sl.terminate()
	print 'end..'

main()