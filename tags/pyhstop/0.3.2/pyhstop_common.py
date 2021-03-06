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

import base64, binascii

VERSION = '0.3.2'

def httpencode(data):
	return data
	#return base64.binascii.b2a_hqx(base64.binascii.rlecode_hqx(data))
	return base64.binascii.b2a_hex(data)

def httpdecode(data):
	return data
	#return base64.binascii.a2b_hqx(base64.binascii.rledecode_hqx(data))
	return base64.binascii.a2b_hex(data)
