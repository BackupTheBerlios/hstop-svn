#! /usr/bin/python

from urllib2 import *


proxyhandler = ProxyHandler({'http': 'http://proxy.arcor-ip.de:8080', 'https': 'http://proxy.arcror-ip.de:8080'})

opener = build_opener(proxyhandler)
install_opener(opener)

req = Request('http://nossl.bigmo.dyndns.info/')
f = urlopen(req)
print f.read()
