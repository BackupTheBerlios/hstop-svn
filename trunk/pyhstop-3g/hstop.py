try:
	import appuifw, e32
except:
	pass
import socket
import codecs
import zlib
import md5
import binascii
import thread
import time
import string
from select import select
import sys
sys.path.append(u'C:\Python')
sys.path.append(u'E:\Python')
try:
	from threading import Thread
except:
	print 'import of threading failed'

path = "E:\\Python\hstop.ini"
newline = "\n"
callgate = None

AGENTSTRING = u'User-Agent: Mozilla/5.0 (SymbianOS/9.1; U; en-us) AppleWebKit/413 (KHTML, like Gecko) Safari/413 es65'

thrs = []

# stores all gloabl settings:
globalsettings = ["10.13.1.100","8980","www-proxy.t-online.de", "80", "", "flx", "nopw"]

#indexes in this array
_SETTINGS_HOST = 0
_SETTINGS_PORT = 1
_SETTINGS_PROXYHOST = 2
_SETTINGS_PROXYPORT = 3
_SETTINGS_AGENT = 4
_SETTINGS_USER = 5
_SETTINGS_PWD = 6
_SETTINGS_ACCESPOINT = 7

def get_setting(index):
	global globalsettings
	try:
		return globalsettings[index]
	except:
		return None

def set_setting(index, data):
	global globalsettings
	if len(globalsettings) + 1 > index:
		for i in range(index - len(globalsettings) + 1):
			globalsettings.append(None)
	globalsettings[index] = data
	
def read_settings():
	global globalsettings
	try:
		f = codecs.open(path, 'r', 'utf8')
		settingsfile = f.read()
		settings = settingsfile.split(newline);
		f.close()
		for i in range(len(settings)):
			set_setting(i, settings[i])
	except:
		return None
	return globalsettings
  
def write_settings():
	global globalsettings
	try:
		newarray=""
		for i in range(len(globalsettings)):
			newarray += globalsettings[i] + newline
		f = codecs.open(path, 'w', 'utf_8')
		f.write(newarray)
		f.close()
	except:
		return None

def unset_accesspoint():
	global _SETTINGS_ACCESPOINT
	set_setting(_SETTINGS_ACCESPOINT, None)
	appuifw.note(u"Default access point is unset ", "info")


def set_accesspoint():
	global _SETTINGS_ACCESPOINT
	apid = socket.select_access_point()
	if appuifw.query(u"Set as default access point","query") == True:
		set_setting(_SETTINGS_ACCESPOINT,repr(apid))
		apo = socket.access_point(apid)
		socket.set_default_access_point(apo)

def get_accespoint():
	global _SETTINGS_ACCESPOINT
	apid = get_setting(_SETTINGS_ACCESPOINT)
	if not apid == None :
		apo = socket.access_point(int(apid))
		socket.set_default_access_point(apo)
	else:
		set_accesspoint()

def quit():
	write_settings()
	app_lock.signal()
	appuifw.app.set_exit()

def block_recv(sock, mlen):
	data = ""
	tdata = sock.recv(mlen)
	while (tdata != None):
		data += tdata
		mlen -= len(tdata)
		if (mlen <= 0):
			return data
		tdata = sock.recv(mlen)
	return data

def send_message(sock, msg):
	zipmsg = zlib.compress(msg)
	sock.send(str(len(zipmsg)) + "\x00")
	sock.send(zipmsg)

def recv_message(sock):
	r = sock.recv(10)
	if (r == None):
		return None
	try:
		splitted = r.split("\x00", 1)
		mlen = int(splitted[0])
	except:
		return None
	try:
		zipmsg = splitted[1]
	except:
		zipmsg = ''
	zipmsg += block_recv(sock, mlen - len(zipmsg))
	msg = zlib.decompress(zipmsg)
	return msg

def connect():
	global _SETTINGS_HOST, _SETTINGS_PORT, _SETTINGS_PROXYHOST, _SETTINGS_PROXYPORT, _SETTINGS_AGENT, _SETTINGS_USER, _SETTINGS_PWD, AGENTSTRING
	try:
		get_accespoint()
	except:
		print "no default accespoint"
		pass
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.connect((get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT))))
	proxycmd = "CONNECT " + get_setting(_SETTINGS_HOST) + ":" + get_setting(_SETTINGS_PORT) + " HTTP/1.1\n"
	proxycmd += "Host:  " + get_setting(_SETTINGS_HOST) + ":" + get_setting(_SETTINGS_PORT) + "\n"
	proxycmd += "Accept-Encoding: gzip, deflate, x-gzip, identity; q=0.9\n"
	proxycmd += AGENTSTRING + "\n\n"
	s.send(proxycmd)
	print 'send CONNECT request'
	
	print s.recv(1024)
	
	s.send("\x13\x01\x00\x00\x27")
	print 'send 0x1301000027 init seq. request'
	
	# login:
	#	send "hello:username"
	#	recv "olleh:challange"
	#	send "login:md5(challange+md5(password))"
	#	recv "nigol:pass"
	
	# data:
	#	send/recv: "\x?? (type) + data"
	
	# type:
	#	\x00: byebye
	#	\x01: hello - data: username
	#	recv: \x01 + challange
	#	\x02: login - data: md5(challange+md5(password))
	#	recv:	\x02 - data: "pass"
	#	\x10: close tunnel - data: tunnelid
	#	\x11: init tunnel - data: type + host + port
	#	recv: "\x11 + tunnelid"
	#	\x80 + tunnelid: message - data: message
	#	recv: \x80 + tunnelid - data: message
	
	send_message(s, "\x01" + get_setting(_SETTINGS_USER))
	print "send \\x01 +", get_setting(_SETTINGS_USER)
	loginchallange = recv_message(s)
	try:
		loginchallange = loginchallange[1:]
	except:
		print "login failed!"
		return False
	print "got challange:", loginchallange
	print "pw", get_setting(_SETTINGS_PWD)
	md5pw = md5.new(get_setting(_SETTINGS_PWD)).digest()
	print "md5pw", binascii.hexlify(md5pw)
	md5challange = md5.new(loginchallange + md5pw).digest()
	print "send back:", binascii.hexlify(md5challange)
	send_message(s, "\x02" + md5challange)
	if (recv_message(s) != "\x02pass"):
		print "login failed!"
		return False
	
	print recv_message(s)
	print recv_message(s)
	print recv_message(s)
	s.close
	print 'close'

class Thread:
	def __init__(self):
		print "init thread"
		self.thr = None
		self.finished = False
	
	def start(self):
		print "start thread"
		self.thr = thread.start_new_thread(self.__run, ())
		
	def run(self):
		pass
	
	def __run(self):
		self.run()
		self.finished = True
	
	def setDaemon(self, bool):
		pass
	
	def join(self):
		while not self.finished:
			time.sleep(5)
	
	def exit(self):
		self.thr.exit()

class HTTPcopy(Thread):
	def __init__(self, sread, swrite):
		Thread.__init__ (self)
		self.sread = sread
		self.swrite = swrite
		self.othr = None
		self.killed = False
	
	def setOthread(self, othr):
		self.othr = othr
	
	def run(self):
		print "start copy thread"
		self.sread.setblocking(0)
		try:
			while not self.killed:
				data = self.sread.recv(4096)
				if data:
					print "copy:", data
					self.swrite.send(data)
		except:# KeyboardInterrupt:
			print "error in copy thread"
			pass
		print "end copy thread"
		self.othr.kill()
	
	def kill(self):
		self.killed = True
		self.sread.close()

class HTTPcon(Thread):
	def __init__(self, conn, addr):
		Thread.__init__ (self)
		self.conn = conn
		self.addr = addr
	
	def run(self):
		print "connection from:", self.addr
		global _SETTINGS_HOST, _SETTINGS_PORT, _SETTINGS_PROXYHOST, _SETTINGS_PROXYPORT, _SETTINGS_AGENT, _SETTINGS_USER, _SETTINGS_PWD, AGENTSTRING
		try:
			get_accespoint()
		except:
			print "no default accespoint"
			pass
		print "get head"
		self.conn.setblocking(0)
		head = ""
		try:
			head = self.conn.recv(5)
			print head
		except:
			print "fetching head failed"
		splitted = head.split("\n")
		useragentfound = False
		head2 = ""
		for l in splitted:
			if l.startswith("User-Agent:"):
				l = AGENTSTRING
				useragentfound = True
			head2 += l + "\n"
		if not useragentfound:
			head2 = AGENTSTRING + "\n" + head2
		print "new socket"
		s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		print "connect to ", (get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT)))
		s.connect((get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT))))
		print "start copy threads"
		s.send(head2)
		thr1 = HTTPcopy(self.conn, s)
		thr1.setDaemon(True)
		thr2 = HTTPcopy(s, self.conn)
		thr2.setDaemon(True)
		thr1.setOthread(thr2)
		thr2.setOthread(thr1)
		thr2.start()
		thr1.start()
		thr2.join()
		thr1.join()
		#s.close()
		#self.conn.close()
		

class SockPair:
	def __init__(self, sock1, sock2):
		self.sock1 = sock1
		self.sock2 = sock2
		self.closed = False
	
	def copy(self, sockr, sockw):
		global AGENTSTRING
		try:
			data = sockr.recv(4096)
			if data:
				if string.find(data, 'User-Agent:'):
					splitted = data.split("\n")
					data2 = []
					for l in splitted:
						if l.startswith("User-Agent:"):
							l = AGENTSTRING
						data2.append(l)
					data = string.join(data2, "\n")
					del data2
					#print "patched useragent"
				#print "copy:", data
				print "copy"
				sockw.send(data)
			del data
			print "close socketpair", self
			self.close()
		except:
			print "close socketpair", self
			self.close()

	def socklist(self):
		global _SETTINGS_PROXYHOST, _SETTINGS_PROXYPORT
		if not self.sock2:
			print "new socket"
			self.sock2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			print "connect to ", (get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT)))
			self.sock2.connect((get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT))))
			print "connected"
		return [self.sock1, self.sock2]
	
	def other_socket(self, sock):
		if sock == self.sock1:
			return self.sock2
		if sock == self.sock2:
			return self.sock1
		return None
	
	def close(self):
		print "close socketpair"
		self.sock1.close()
		self.sock2.close()
		self.closed = True
	
	def close_if_in(self, sock):
		if self.other_socket(sock):
			self.close();
			return True
		return False
	
	def copy_if_in(self, sock):
		if self.other_socket(sock):
			self.copy(sock, self.other_socket(sock))
			return True
		return False

class HTTPproxy(Thread):
	def __init__(self):
		self.sockPairList = []
		self.callgate = None
	
	def handle_connection(self, conn, addr):
		print "connection from:", addr
		try:
			get_accespoint()
		except:
			print "no default accespoint"
		print "get head"
		#self.conn.setblocking(0)
		head = ""
		try:
			while string.find(head, "\n\r\n") < 0:
				#print "head part:", head
				head += conn.recv(4096)
			print "got head"
		except KeyboardInterrupt:
			print "fetching head failed"
		splitted = head.split("\n")
		useragentfound = False
		head2 = []
		for l in splitted:
			if l.startswith("User-Agent:"):
				l = AGENTSTRING
				useragentfound = True
			head2.append(l)
		del splitted
		head = string.join(head2, "\n")
		#if not useragentfound:
		#	head2 = AGENTSTRING + "\n" + head2
		#else:
		#	print "found User-Agent"
		print "new socket"
		s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		print "connect to ", (get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT)))
		s.connect((get_setting(_SETTINGS_PROXYHOST), int(get_setting(_SETTINGS_PROXYPORT))))
		print "start copy"
		s.send(head)
		del head
		del head2
		
		self.sockPairList.append(SockPair(conn, s))
		
		# we have 2 sockets: conn, s. we need to copy content from the one to the other
		
		
		try:
			socklist = [conn, s]
			while True:
				s.send("")
				conn.send("")
				ilist, olist, elist = select(socklist, [], [], 3)
				for so in ilist:
					data = so.recv(4096)
					if data:
						if string.find(data, 'User-Agent:'):
							splitted = data.split("\n")
							data2 = []
							for l in splitted:
								if l.startswith("User-Agent:"):
									l = AGENTSTRING
								data2.append(l)
							data = string.join(data2, "\n")
							del data2
							print "patched useragent"
						print "copy:", data
						if socklist[0] == so:
							socklist[1].send(data)
						else:
							socklist[0].send(data)
					del data
		except KeyboardInterrupt:# socket.error:# KeyboardInterrupt:
			print 'closing copy'
			conn.close()
			s.close()
		
	
	def run(self):
		#global thrs, _SETTINGS_HOST, _SETTINGS_PORT, _SETTINGS_PROXYHOST, _SETTINGS_PROXYPORT, _SETTINGS_AGENT, _SETTINGS_USER, _SETTINGS_PWD, AGENTSTRING
		global callgate
		self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self.sock.bind((u'127.0.0.1', 8081))
		self.sock.listen(5)
		#conn, addr = self.sock.accept()
		#print "append sockpair"
		#self.sockPairList.append(SockPair(conn, None))
		#callgate = e32.ao_callgate(self.sock.accept)
		#self.lthread = thread.start_new_thread(self.listen, ())
		
		while True:
			#while not self.sockPairList:
			#	print self.sockPairList
			#	time.sleep(3)
			for i in range(len(self.sockPairList)):
				#sp = self.sockPairList[i]
				if self.sockPairList[i].closed:
					del self.sockPairList[i]
					break
			socklist = [self.sock]
			for sp in self.sockPairList:
				socklist += sp.socklist()
			ilist, olist, elist = select(socklist, [], [], 3)
			print ilist
			did_sth = False
			if not ilist:
				print "continue"
				continue
			if self.sock in ilist:
				conn, addr = self.sock.accept()
				print "append sockpair"
				self.sockPairList.append(SockPair(conn, None))
				did_sth = True
			for s in ilist:
				print "check", s
				for sp in self.sockPairList:
					print "against", sp.socklist()
					if sp.copy_if_in(s):
						print "found"
						#did_sth = True
						break
			if not did_sth:
				time.sleep(3)
	
	def listen(self):
		#global callgate
		while True:
			conn, addr = self.sock.accept()
			#conn, addr = callgate()
			print "append sockpair"
			self.sockPairList.append(SockPair(conn, None))
			#print self.sockPairList

def httpproxy():
	global thrs
	print "creat thread"
	thr = HTTPproxy()
	print "set daemon"
	thr.setDaemon(True)
	print "start"
	thr.start()
	print "append"
	thrs.append(thr)
	print "httpconnection thread started!"

try: ## we are on the mobile!
	read_settings()
	
	# define application 1: text app
	appStatus = appuifw.Text(u'Appliation o-n-e is on')
	
	# define application 2: text app
	appSettings = appuifw.Text(u'Appliation t-w-o is on')
	
	# define application 3: text app
	appForwarding = appuifw.Text(u'Appliation t-h-r-e-e is on')
	
	appLogs = appuifw.Text(u'Appliation f-o-u-r is on')
	
	def handle_tab(index):
		global lb
		if index == 0:
			appuifw.app.body = appStatus # switch to application 1
		if index == 1:
			appuifw.app.body = appSettings # switch to application 2
		if index == 2:
			appuifw.app.body = appForwarding # switch to application 3
		if index == 3:
			appuifw.app.body = appLogs # switch to application 3
	
	appuifw.app.menu = [
			(u"connect",connect),
			(u"unset default ap",unset_accesspoint),
			(u"quit",quit)
		]
	app_lock = e32.Ao_lock()
	
	# create the tabs with its names in unicode as a list, include the tab handler
	#appuifw.app.set_tabs([u"status", u"settings", u"forwarding", u"logs"],handle_tab)
	
	# set the title of the script
	appuifw.app.title = u'pyHstop-3g'
	# set app.body to app1 (for start of script)
	#appuifw.app.body = appStatus
	
	httpproxy()
	appuifw.app.exit_key_handler = quit
	app_lock.wait()
except: ## we are on real python
	#connect()
	httpproxy()
	print 'terminate with EOF'
	try:
		input = sys.stdin.readline()
		while input:
			input = sys.stdin.readline()
	except KeyboardInterrupt:
		print 'interrupted'
