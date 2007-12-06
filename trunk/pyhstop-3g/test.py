import appuifw, e32, camera
 
SCRIPT_LOCK = e32.Ao_lock( )
IMG = None
 
def __exit__( ):
  stop( )
  SCRIPT_LOCK.signal( )
 
def start( ):
    camera.start_finder( vfCallback, backlight_on=1, size=(160, 120) )
    appuifw.app.menu = [(u'Stop', stop), (u'Exit', __exit__)]
 
def stop( ):
    camera.stop_finder( )
    cnvCallback( )
    appuifw.app.menu = [(u'Start', start), (u'Exit', __exit__)]
 
def vfCallback( aIm ):
    global IMG
    appuifw.app.body.blit( aIm )
    IMG = aIm
 
def cnvCallback( aRect=None ):
    if IMG != None:
        appuifw.app.body.clear( )
        appuifw.app.body.blit( IMG )
 
 
appuifw.app.exit_key_handler = __exit__
appuifw.app.title= u'PyS60 ViewFinder'
appuifw.app.body = appuifw.Canvas( redraw_callback = cnvCallback )
start( )
 
SCRIPT_LOCK.wait( ) 
