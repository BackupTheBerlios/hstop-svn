#! /bin/sh

[ "x$1" == "x" ] && echo "release?" && exit

RELEASE="$1"

echo making release: $RELEASE

cat CHANGELOG | sed s/HEAD/$RELEASE/ > CHANGELOG.tmp
mv CHANGELOG.tmp CHANGELOG
cd ..
svn cp pyhstop ../tags/pyhstop/$RELEASE
cd ..
svn ci -m "tagged $RELEASE"
cd tags/pyhstop/$RELEASE
rm -rf *~ *.pyc *.kdevses
svn rm *.pem
svn rm templates
svn rm *.kdevelop
svn rm *.sh
cat pyhstop_common.py | sed s/HEAD/$RELEASE/ > pyhstop_common.py.tmp
mv pyhstop_common.py.tmp pyhstop_common.py

cd ..
svn ci -m "release $RELEASE"

cp -r $RELEASE pyhstop-$RELEASE
rm -rf pyhstop-$RELEASE/.svn

tar -cvzf pyhstop-$RELEASE.tgz pyhstop-$RELEASE/
gpg -b pyhstop-$RELEASE.tgz
curl -T pyhstop-$RELEASE.tgz ftp://ftp.berlios.de/incoming/
curl -T pyhstop-$RELEASE.tgz.sig ftp://ftp.berlios.de/incoming/

rm -r pyhstop-$RELEASE
