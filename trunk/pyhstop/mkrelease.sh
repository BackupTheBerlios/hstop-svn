#! /bin/sh

[ "x$1" == "x" ] && echo "release?" && exit

RELEASE="$1"

echo making release: $RELEASE

cat CHANGELOG | sed -i s/HEAD/$RELEASE/
cd ..
svn cp pyhstop ../tags/pyhstop/$RELEASE
cd ..
svn ci -m "tagged pyhstop $RELEASE"
cd tags/pyhstop/$RELEASE
rm -rf *~ *.pyc *.kdevses
svn rm *.pem
svn rm templates
svn rm *.kdevelop
svn rm init.*
svn rm *.sh
svn rm pyhstop_common.py
sed -i s/HEAD/$RELEASE/ pyhstopd.py
sed -i s/HEAD/$RELEASE/ pyhstopc.py

cd ..
svn ci -m "release $RELEASE"

cp -r $RELEASE pyhstop-$RELEASE
rm -rf pyhstop-$RELEASE/.svn
rm -rf pyhstop-$RELEASE/daemon.py
rm -rf pyhstop-$RELEASE/init*

tar -cvzf pyhstop-$RELEASE.tgz pyhstop-$RELEASE/
gpg -b pyhstop-$RELEASE.tgz
#curl -T pyhstop-$RELEASE.tgz ftp://ftp.berlios.de/incoming/
#curl -T pyhstop-$RELEASE.tgz.sig ftp://ftp.berlios.de/incoming/

rm -r pyhstop-$RELEASE

cd ../../
svn up
