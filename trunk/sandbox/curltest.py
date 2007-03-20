import pycurl, threading

def dl(url):
	c = pycurl.Curl()
	c.setopt(c.URL, url)
	c.setopt(c.NOSIGNAL, 1)
	c.setopt(c.VERBOSE, 1)
	print '+perform ', url
	c.perform()
	print '-perform ', url

t1 = threading.Thread(target=dl,args=("www.heise.de",))
t2 = threading.Thread(target=dl,args=("http://www.heise.de/newsticker/meldung/87116/from/rss09",))

t1.start()
t2.start()
